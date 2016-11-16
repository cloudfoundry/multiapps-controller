package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadStatusCallbackExtended;
import com.sap.cloud.lm.sl.cf.client.util.InputStreamProducer;
import com.sap.cloud.lm.sl.cf.client.util.StreamUtil;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("uploadAppStep")
public class UploadAppStep extends AbstractXS2ProcessStepWithBridge {

    private static final String WAIT_TILL_UPLOAD_START_TASK_ID = "waitTillUploadStartTask";
    private static final String ARCHIVE_FILE_SEPARATOR = "/";
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadAppStep.class);

    public static StepMetadata getMetadata() {
        return new AsyncStepMetadata("uploadAppTask", "Upload App", "Upload App", "pollUploadAppStatusTask", "pollUploadAppStatusTimer");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    protected Function<DelegateExecution, ClientExtensions> extensionsSupplier = (context) -> getClientExtensions(context, LOGGER);

    @Inject
    protected ScheduledExecutorService asyncTaskExecutor;

    @Inject
    protected ContextExtensionDao contextExtensionDao;

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws FileStorageException, SLException {
        logActivitiTask(context, LOGGER);

        CloudApplicationExtended app = StepsUtil.getApp(context);

        try {
            info(context, format(Messages.UPLOADING_APP, app.getName()), LOGGER);
            int uploadAppTimeoutSeconds = ConfigurationUtil.getUploadAppTimeout();

            CloudFoundryOperations client = clientSupplier.apply(context);
            ClientExtensions clientExtensions = extensionsSupplier.apply(context);

            Future<?> future = asyncTaskExecutor.submit(getUploadAppStepRunnable(context, app, client, clientExtensions));
            asyncTaskExecutor.schedule(getUploadAppStepRunnableKiller(context, future), uploadAppTimeoutSeconds, TimeUnit.SECONDS);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, format(Messages.ERROR_UPLOADING_APP, app.getName()), e, LOGGER);
            throw e;
        }
        return ExecutionStatus.RUNNING;
    }

    private Runnable getUploadAppStepRunnableKiller(DelegateExecution context, Future<?> future) {
        return () -> {
            LOGGER.warn(format(Messages.CANCELING_UPLOAD_ASYNC_THREAD, context.getProcessInstanceId()));
            if (future.cancel(true)) {
                LOGGER.warn(format(Messages.ASYNC_THREAD_CANCELLED, context.getProcessInstanceId()));
            } else {
                LOGGER.warn(format(Messages.ASYNC_THREAD_COMPLETED, context.getProcessInstanceId()));
            }
        };
    }

    private String asyncUploadFiles(DelegateExecution context, ClientExtensions clientExtensions, CloudApplication app, String appArchiveId,
        String fileName) throws FileStorageException, SLException {
        final StringBuilder uploadTokenBuilder = new StringBuilder();
        FileDownloadProcessor uploadFileToControllerProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
            appArchiveId, (appArchiveStream) -> {
                File file = null;
                try (InputStreamProducer streamProducer = getInputStreamProducer(appArchiveStream, fileName)) {
                    // Start uploading application content
                    file = saveToFile(fileName, streamProducer);
                    String uploadToken = clientExtensions.asynchUploadApplication(app.getName(), file,
                        getMonitorUploadStatusCallback(context, app, file));
                    uploadTokenBuilder.append(uploadToken);
                } catch (IOException e) {
                    cleanUpTempFile(context, file);
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, fileName);
                } catch (CloudFoundryException e) {
                    cleanUpTempFile(context, file);
                    throw e;
                }
            });
        fileService.processFileContent(uploadFileToControllerProcessor);
        return uploadTokenBuilder.toString();
    }

    private void uploadFiles(DelegateExecution context, final CloudFoundryOperations client, final CloudApplication app,
        final String appArchiveId, final String fileName) throws FileStorageException, SLException {
        FileDownloadProcessor uploadFileToControllerProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
            appArchiveId, (appArchiveStream) -> {
                File file = null;
                try (InputStreamProducer streamProducer = getInputStreamProducer(appArchiveStream, fileName)) {
                    // Upload application content
                    file = saveToFile(fileName, streamProducer);
                    client.uploadApplication(app.getName(), file, getMonitorUploadStatusCallback(context, app, file));

                } catch (IOException e) {
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, fileName);
                } catch (CloudFoundryException e) {
                    throw e;
                } finally {
                    cleanUpTempFile(context, file);
                }
            });
        fileService.processFileContent(uploadFileToControllerProcessor);
    }

    MonitorUploadStatusCallback getMonitorUploadStatusCallback(DelegateExecution context, CloudApplication app, File file) {
        return new MonitorUploadStatusCallback(context, app, file);
    }

    InputStreamProducer getInputStreamProducer(InputStream appArchiveStream, String fileName) throws SLException {
        return new InputStreamProducer(appArchiveStream, fileName);
    }

    protected File saveToFile(String fileName, InputStreamProducer streamProducer) throws IOException {
        InputStream stream = streamProducer.getNextInputStream();
        if (stream == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, fileName);
        }

        String entryName = streamProducer.getStreamEntryName();
        if (isFile(fileName)) {
            return StreamUtil.saveStreamToFile(entryName, stream);
        }

        if (entryName.equals(fileName)) {
            return StreamUtil.saveZipStreamToDirectory(fileName, stream);
        }
        Path destinationDirectory = StreamUtil.getTempDirectory(fileName);
        while (stream != null) {
            if (!entryName.endsWith(ARCHIVE_FILE_SEPARATOR)) {
                StreamUtil.saveStreamToDirectory(entryName, fileName, destinationDirectory, stream);
            }
            stream = streamProducer.getNextInputStream();
            entryName = streamProducer.getStreamEntryName();
        }
        return destinationDirectory.toFile();
    }

    void cleanUpTempFile(DelegateExecution context, File file) {
        if (file != null) {
            try {
                StreamUtil.deleteFile(file);
            } catch (IOException e) {
                warn(context, format(Messages.ERROR_DELETING_APP_TEMP_FILE, file.getAbsolutePath()), LOGGER);
            }
        }
    }

    private boolean isFile(String fileName) {
        return !fileName.endsWith(ARCHIVE_FILE_SEPARATOR);
    }

    class MonitorUploadStatusCallback implements UploadStatusCallbackExtended {

        static final String FINISHED_STATUS = "finished";

        private final DelegateExecution context;
        private final CloudApplication app;
        private final File file;

        public MonitorUploadStatusCallback(DelegateExecution context, CloudApplication app, File file) {
            this.context = context;
            this.app = app;
            this.file = file;
        }

        @Override
        public void onCheckResources() {
            debug(context, "Resources checked", LOGGER);
        }

        @Override
        public void onMatchedFileNames(Set<String> matchedFileNames) {
            boolean appContentChanged = matchedFileNames.size() != 0;
            try {
                updateContextExtension(matchedFileNames, appContentChanged);
            } catch (SLException e) {
                error(context, Messages.ERROR_UPDATING_CONTEXT_EXTENSION, e, LOGGER);
            }
            info(context, format("Matched files count: {0}", matchedFileNames.size()), LOGGER);
        }

        private void updateContextExtension(Set<String> matchedFileNames, boolean appContentChanged) throws SLException {
            boolean appPropertiesChanged = StepsUtil.getAppPropertiesChanged(context);
            boolean shouldRestart = appPropertiesChanged || appContentChanged;
            contextExtensionDao.addOrUpdate(context.getProcessInstanceId(), Constants.VAR_RESTART_APPLICATION,
                Boolean.toString(shouldRestart));
        }

        @Override
        public void onProcessMatchedResources(int length) {
            info(context, format("Matched resources processed, total size is {0}", length), LOGGER);
        }

        @Override
        public boolean onProgress(String status) {
            info(context, format("Upload status: {0}", status), LOGGER);
            if (status.equals(FINISHED_STATUS)) {
                cleanUpTempFile(context, file);
            }
            return false;
        }

        @Override
        public void onError(Exception e) {
            error(context, format(Messages.ERROR_UPLOADING_APP, app.getName()), e, LOGGER);
            cleanUpTempFile(context, file);
        }

    }

    protected Runnable getUploadAppStepRunnable(DelegateExecution context, CloudApplicationExtended app, CloudFoundryOperations client,
        ClientExtensions clientExtensions) {
        return () -> {
            String processId = context.getProcessInstanceId();
            trace(context, format("Started upload app step runnable for process \"{0}\"", processId), LOGGER);
            ExecutionStatus status = ExecutionStatus.FAILED;
            Map<String, Object> outputVariables = new HashMap<>();
            try {
                String appArchiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
                String fileName = StepsUtil.getModuleFileName(context, app.getModuleName());
                debug(context, format("Uploading file \"{0}\" for application \"{1}\"", fileName, app.getName()), LOGGER);
                if (clientExtensions != null) {
                    String uploadToken = asyncUploadFiles(context, clientExtensions, app, appArchiveId, fileName);
                    outputVariables.put(Constants.VAR_UPLOAD_TOKEN, uploadToken);
                    debug(context, format("Started async upload of application \"{0}\"", fileName, app.getName()), LOGGER);
                    status = ExecutionStatus.RUNNING;
                } else {
                    uploadFiles(context, client, app, appArchiveId, fileName);
                    debug(context, format(Messages.APP_UPLOADED, app.getName()), LOGGER);
                    status = ExecutionStatus.SUCCESS;
                }
            } catch (SLException | FileStorageException e) {
                error(context, format(Messages.ERROR_UPLOADING_APP, app.getName()), e, LOGGER);
                throw new IllegalStateException(e.getMessage(), e);
            } catch (CloudFoundryException cfe) {
                SLException e = StepsUtil.createException(cfe);
                error(context, format(Messages.ERROR_UPLOADING_APP, app.getName()), e, LOGGER);
                throw new IllegalStateException(e.getMessage(), e);
            } catch (Throwable e) {
                e = getWithProperMessage(e);
                logException(context, e);
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                outputVariables.put(getStatusVariable(), status.name());
                LOGGER.info(format("Attempting to signal process with id:{0} with variables : {1}", processId, outputVariables));
                signalWaitTask(context.getProcessInstanceId(), outputVariables, ConfigurationUtil.getUploadAppTimeout() * 1000);
            }
            trace(context, format("Upload app step runnable for process \"{0}\" finished", context.getProcessInstanceId()), LOGGER);
        };
    }

    protected void signalWaitTask(String processId, Map<String, Object> outputVariables, int timeout) {
        ActivitiFacade.getInstance().signal(null, processId, WAIT_TILL_UPLOAD_START_TASK_ID, outputVariables, timeout);
    }
}
