package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadStatusCallbackExtended;
import com.sap.cloud.lm.sl.cf.client.util.InputStreamProducer;
import com.sap.cloud.lm.sl.cf.client.util.StreamUtil;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationEnvironmentUpdater;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationFileDigestDetector;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component("uploadAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UploadAppStep extends TimeoutAsyncActivitiStep {

    private static final Integer DEFAULT_UPLOAD_TIMEOUT = 1800; // 30 minutes
    private static final String ARCHIVE_FILE_SEPARATOR = "/";

    @Inject
    protected ActivitiFacade activitiFacade;
    @Inject
    protected ApplicationConfiguration configuration;

    @Override
    public StepPhase executeAsyncStep(ExecutionWrapper execution) throws FileStorageException, SLException {
        getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            getStepLogger().info(Messages.UPLOADING_APP, app.getName());

            int uploadAppTimeoutSeconds = configuration.getUploadAppTimeout();
            getStepLogger().debug(Messages.UPLOAD_APP_TIMEOUT, uploadAppTimeoutSeconds);

            CloudFoundryOperations client = execution.getCloudFoundryClientWithoutTimeout();

            uploadApp(execution, app, client);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            throw e;
        }
        return StepPhase.POLL;
    }

    private void uploadApp(ExecutionWrapper execution, CloudApplicationExtended app, CloudFoundryOperations client) {
        String processId = execution.getContext()
            .getProcessInstanceId();
        getStepLogger().trace("Started upload app step runnable for process \"{0}\"", processId);
        try {
            attemptToUploadApp(execution, app, client);
        } catch (SLException | FileStorageException e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            logException(execution.getContext(), e);
            throw new SLException(e, e.getMessage());
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            logException(execution.getContext(), e);
            throw e;
        } catch (Throwable e) {
            Throwable eWithMessage = getWithProperMessage(e);
            logException(execution.getContext(), eWithMessage);
            if (e instanceof Exception) {
                // only wrap Runtime & checked exceptions as Runtime ones
                throw new RuntimeException(eWithMessage.getMessage(), eWithMessage);
            } else {
                // Errors and other should be handled elsewhere
                throw e;
            }
        }
        getStepLogger().trace("Upload app step runnable for process \"{0}\" finished", execution.getContext()
            .getProcessInstanceId());
    }

    private void attemptToUploadApp(ExecutionWrapper execution, CloudApplicationExtended app, CloudFoundryOperations client)
        throws FileStorageException {
        AsyncExecutionState status = AsyncExecutionState.ERROR;
        try {
            String appArchiveId = StepsUtil.getRequiredStringParameter(execution.getContext(), Constants.PARAM_APP_ARCHIVE_ID);
            String fileName = StepsUtil.getModuleFileName(execution.getContext(), app.getModuleName());
            getStepLogger().debug("Uploading file \"{0}\" for application \"{1}\"", fileName, app.getName());
            String uploadToken = asyncUploadFiles(execution, client, app, appArchiveId, fileName);
            execution.getContext()
                .setVariable(Constants.VAR_UPLOAD_TOKEN, uploadToken);
            getStepLogger().debug("Started async upload of application \"{0}\"", fileName, app.getName());
            status = AsyncExecutionState.RUNNING;
        } finally {
            execution.getContext()
                .setVariable(Constants.VAR_UPLOAD_STATE, status.name());
        }
    }

    private String asyncUploadFiles(ExecutionWrapper execution, CloudFoundryOperations client, CloudApplication app, String appArchiveId,
        String fileName) throws FileStorageException, SLException {
        final StringBuilder uploadTokenBuilder = new StringBuilder();
        final DelegateExecution context = execution.getContext();
        FileDownloadProcessor uploadFileToControllerProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
            appArchiveId, (appArchiveStream) -> {
                File file = null;
                long maxStreamSize = ApplicationConfiguration.getInstance()
                    .getMaxResourceFileSize();
                try (InputStreamProducer streamProducer = getInputStreamProducer(appArchiveStream, fileName, maxStreamSize)) {
                    // Start uploading application content
                    file = saveToFile(fileName, streamProducer);
                    getStepLogger().debug(Messages.CREATED_TEMP_FILE, file);
                    detectApplicationFileDigestChanges(context, app, file, client);
                    String uploadToken = client.asyncUploadApplication(app.getName(), file,
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

    private void detectApplicationFileDigestChanges(DelegateExecution context, CloudApplication app, File applicationFile,
        CloudFoundryOperations client) {
        CloudApplication existingApp = client.getApplication(app.getName());
        ApplicationFileDigestDetector applicationFileDigestDetector = new ApplicationFileDigestDetector(existingApp);
        String appNewFileDigest = applicationFileDigestDetector.detectNewAppFileDigest(applicationFile);
        String currentFileDigest = applicationFileDigestDetector.detectCurrentAppFileDigest();
        attemptToUpdateApplicationDigest(client, app, appNewFileDigest, currentFileDigest);
        setAppContentChanged(context, hasAppFileDigestChanged(appNewFileDigest, currentFileDigest));
    }

    private void attemptToUpdateApplicationDigest(CloudFoundryOperations client, CloudApplication app, String newFileDigest,
        String currentFileDigest) {
        if (!hasAppFileDigestChanged(newFileDigest, currentFileDigest)) {
            return;
        }
        new ApplicationEnvironmentUpdater(app, client).updateApplicationEnvironment(
            com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES, com.sap.cloud.lm.sl.cf.core.Constants.ATTR_APP_CONTENT_DIGEST,
            newFileDigest);
    }

    private boolean hasAppFileDigestChanged(String newFileDigest, String currentFileDigest) {
        return !newFileDigest.equals(currentFileDigest);
    }

    private void setAppContentChanged(DelegateExecution context, boolean appContentChanged) {
        context
            .setVariable(Constants.VAR_APP_CONTENT_CHANGED, Boolean.toString(appContentChanged));
    }

    MonitorUploadStatusCallback getMonitorUploadStatusCallback(DelegateExecution context, CloudApplication app, File file) {
        return new MonitorUploadStatusCallback(context, app, file);
    }

    InputStreamProducer getInputStreamProducer(InputStream appArchiveStream, String fileName, long maxStreamSize) throws SLException {
        return new InputStreamProducer(appArchiveStream, fileName, maxStreamSize);
    }

    @SuppressWarnings("resource")
    protected File saveToFile(String fileName, InputStreamProducer streamProducer) throws IOException {
        InputStream stream = streamProducer.getNextInputStream();
        if (stream == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, fileName);
        }

        String entryName = streamProducer.getStreamEntryName();
        StreamUtil streamUtil = new StreamUtil(stream);
        if (isFile(fileName)) {
            return streamUtil.saveStreamToFile(entryName);
        }

        if (entryName.equals(fileName)) {
            return streamUtil.saveZipStreamToDirectory(fileName, ApplicationConfiguration.getInstance()
                .getMaxResourceFileSize());
        }
        Path destinationDirectory = StreamUtil.getTempDirectory(fileName);
        while (stream != null) {
            if (!entryName.endsWith(ARCHIVE_FILE_SEPARATOR)) {
                streamUtil.saveStreamToDirectory(entryName, fileName, destinationDirectory);
            }
            // no need to close this stream because no new stream object is created
            stream = streamProducer.getNextInputStream();
            streamUtil.setInputStream(stream);
            entryName = streamProducer.getStreamEntryName();
        }
        return destinationDirectory.toFile();
    }

    void cleanUpTempFile(DelegateExecution context, File file) {
        if (file != null) {
            try {
                getStepLogger().debug(Messages.DELETING_TEMP_FILE, file);
                StreamUtil.deleteFile(file);
            } catch (IOException e) {
                getStepLogger().warn(Messages.ERROR_DELETING_APP_TEMP_FILE, file.getAbsolutePath());
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
            getStepLogger().debug("Resources checked");
        }

        @Override
        public void onMatchedFileNames(Set<String> matchedFileNames) {
            getStepLogger().info("Matched files count: {0}", matchedFileNames.size());
        }

        @Override
        public void onProcessMatchedResources(int length) {
            getStepLogger().info("Matched resources processed, total size is {0}", length);
        }

        @Override
        public boolean onProgress(String status) {
            getStepLogger().info(Messages.UPLOAD_STATUS_0, status);
            if (status.equals(FINISHED_STATUS)) {
                cleanUpTempFile(context, file);
            }
            return false;
        }

        @Override
        public void onError(Exception e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            cleanUpTempFile(context, file);
        }

        @Override
        public void onError(String description) {
            // TODO Auto-generated method stub

        }

    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions() {
        return Arrays.asList(new PollUploadAppStatusExecution());
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return DEFAULT_UPLOAD_TIMEOUT;
    }

}
