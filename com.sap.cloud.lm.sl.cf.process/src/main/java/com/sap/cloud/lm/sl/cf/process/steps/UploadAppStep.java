package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.io.FileUtils;
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
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationEnvironmentUpdater;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationFileDigestDetector;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
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

    static final int DEFAULT_APP_UPLOAD_TIMEOUT = (int) TimeUnit.HOURS.toSeconds(1);
    private static final String ARCHIVE_FILE_SEPARATOR = "/";

    @Inject
    protected ApplicationConfiguration configuration;

    @Override
    public StepPhase executeAsyncStep(ExecutionWrapper execution) throws FileStorageException, SLException {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            getStepLogger().info(Messages.UPLOADING_APP, app.getName());
            CloudFoundryOperations client = execution.getCloudFoundryClientWithoutTimeout();

            String appArchiveId = StepsUtil.getRequiredStringParameter(execution.getContext(), Constants.PARAM_APP_ARCHIVE_ID);
            String fileName = StepsUtil.getModuleFileName(execution.getContext(), app.getModuleName());

            getStepLogger().debug(Messages.UPLOADING_FILE_0_FOR_APP_1, fileName, app.getName());
            String uploadToken = asyncUploadFiles(execution, client, app, appArchiveId, fileName);
            getStepLogger().debug(Messages.STARTED_ASYNC_UPLOAD_OF_APP_0, app.getName());
            execution.getContext()
                .setVariable(Constants.VAR_UPLOAD_TOKEN, uploadToken);
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            throw e;
        }
        return StepPhase.POLL;
    }

    private String asyncUploadFiles(ExecutionWrapper execution, CloudFoundryOperations client, CloudApplication app, String appArchiveId,
        String fileName) throws FileStorageException, SLException {
        final StringBuilder uploadTokenBuilder = new StringBuilder();
        final DelegateExecution context = execution.getContext();
        FileDownloadProcessor uploadFileToControllerProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
            appArchiveId, appArchiveStream -> {
                Path dir = null;
                File file = null;
                long maxStreamSize = configuration.getMaxResourceFileSize();
                try (InputStreamProducer streamProducer = getInputStreamProducer(appArchiveStream, fileName, maxStreamSize)) {
                    // Start uploading application content
                    dir = getTempDirectoryFromFilename(fileName);
                    file = saveToFile(fileName, dir, streamProducer);
                    getStepLogger().debug(Messages.CREATED_TEMP_FILE, file);
                    detectApplicationFileDigestChanges(execution, app, file, client);
                    String uploadToken = client.asyncUploadApplication(app.getName(), file, getMonitorUploadStatusCallback(app, dir));
                    uploadTokenBuilder.append(uploadToken);
                } catch (IOException e) {
                    cleanUpTempDir(dir);
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, fileName);
                } catch (CloudFoundryException | ContentException e) {
                    cleanUpTempDir(dir);
                    throw e;
                }
            });
        fileService.processFileContent(uploadFileToControllerProcessor);
        return uploadTokenBuilder.toString();
    }

    private void detectApplicationFileDigestChanges(ExecutionWrapper execution, CloudApplication app, File applicationFile,
        CloudFoundryOperations client) {
        CloudApplication existingApp = client.getApplication(app.getName());
        ApplicationFileDigestDetector applicationFileDigestDetector = new ApplicationFileDigestDetector(existingApp);
        String appNewFileDigest = applicationFileDigestDetector.detectNewAppFileDigest(applicationFile);
        String currentFileDigest = applicationFileDigestDetector.detectCurrentAppFileDigest();
        attemptToUpdateApplicationDigest(client, app, appNewFileDigest, currentFileDigest);
        setAppContentChanged(execution, hasAppFileDigestChanged(appNewFileDigest, currentFileDigest));
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

    private void setAppContentChanged(ExecutionWrapper execution, boolean appContentChanged) {
        execution.getContext()
            .setVariable(Constants.VAR_APP_CONTENT_CHANGED, Boolean.toString(appContentChanged));
    }

    MonitorUploadStatusCallback getMonitorUploadStatusCallback(CloudApplication app, Path dir) {
        return new MonitorUploadStatusCallback(app, dir);
    }

    InputStreamProducer getInputStreamProducer(InputStream appArchiveStream, String fileName, long maxStreamSize) throws SLException {
        return new InputStreamProducer(appArchiveStream, fileName, maxStreamSize);
    }

    @SuppressWarnings("resource")
    protected File saveToFile(String fileName, Path tempDirectory, InputStreamProducer streamProducer) throws IOException {
        InputStream stream = streamProducer.getNextInputStream();
        if (stream == null) {
            throw new ContentException(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, fileName);
        }

        String entryName = streamProducer.getStreamEntryName();
        StreamUtil streamUtil = new StreamUtil(stream);
        if (isFile(fileName)) {
            return streamUtil.saveStreamToFile(entryName, tempDirectory, configuration.getMaxResourceFileSize());
        }

        if (entryName.equals(fileName)) {
            return streamUtil.saveZipStreamToDirectory(fileName, tempDirectory, configuration.getMaxResourceFileSize());
        }
        long filesSize = 0;
        while (stream != null) {
            if (!entryName.endsWith(ARCHIVE_FILE_SEPARATOR)) {
                filesSize = streamUtil.saveStreamToDirectory(entryName, fileName, tempDirectory, filesSize,
                    configuration.getMaxResourceFileSize());
            }
            // no need to close this stream because no new stream object is created
            stream = streamProducer.getNextInputStream();
            streamUtil.setInputStream(stream);
            entryName = streamProducer.getStreamEntryName();
        }
        return tempDirectory.toFile();
    }

    public Path getTempDirectoryFromFilename(String fileName) throws IOException {
        return StreamUtil.getTempDirectoryFromFilename(fileName);
    }

    void cleanUpTempDir(Path dirPath) {
        if (dirPath != null) {
            File dir = dirPath.toFile();
            try {
                getStepLogger().debug(Messages.DELETING_TEMP_FILE, dir);
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                getStepLogger().warn(Messages.ERROR_DELETING_APP_TEMP_FILE, dir.getAbsolutePath());
            }
        }
    }

    private boolean isFile(String fileName) {
        return !fileName.endsWith(ARCHIVE_FILE_SEPARATOR);
    }

    class MonitorUploadStatusCallback implements UploadStatusCallbackExtended {

        static final String FINISHED_STATUS = "finished";

        private final CloudApplication app;
        private final Path dir;

        public MonitorUploadStatusCallback(CloudApplication app, Path dir) {
            this.app = app;
            this.dir = dir;
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
                cleanUpTempDir(dir);
            }
            return false;
        }

        @Override
        public void onError(Exception e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            cleanUpTempDir(dir);
        }

        @Override
        public void onError(String description) {
            getStepLogger().error(Messages.ERROR_UPLOADING_APP_BECAUSE_OF, app.getName(), description);
            cleanUpTempDir(dir);
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
        CloudApplication app = StepsUtil.getApp(context);
        int uploadTimeout = extractUploadTimeoutFromAppAttributes(app, DEFAULT_APP_UPLOAD_TIMEOUT);
        getStepLogger().debug(Messages.UPLOAD_APP_TIMEOUT, uploadTimeout);
        return uploadTimeout;
    }

    private int extractUploadTimeoutFromAppAttributes(CloudApplication app, int defaultAppUploadTimeout) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        Number uploadTimeout = appAttributes.get(SupportedParameters.UPLOAD_TIMEOUT, Number.class, defaultAppUploadTimeout);
        return uploadTimeout.intValue();
    }

}
