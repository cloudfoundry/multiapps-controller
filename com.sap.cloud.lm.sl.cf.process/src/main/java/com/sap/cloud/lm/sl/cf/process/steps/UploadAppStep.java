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

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadStatusCallbackExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationEnvironmentUpdater;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationFileDigestDetector;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveExtractor;
import com.sap.cloud.lm.sl.common.SLException;

@Component("uploadAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UploadAppStep extends TimeoutAsyncFlowableStep {

    static final int DEFAULT_APP_UPLOAD_TIMEOUT = (int) TimeUnit.HOURS.toSeconds(1);

    @Inject
    protected ApplicationConfiguration configuration;

    @Override
    public StepPhase executeAsyncStep(ExecutionWrapper execution) throws FileStorageException {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            getStepLogger().info(Messages.UPLOADING_APP, app.getName());
            CloudControllerClient client = execution.getControllerClient();

            String appArchiveId = StepsUtil.getRequiredStringParameter(execution.getContext(), Constants.PARAM_APP_ARCHIVE_ID);
            String fileName = StepsUtil.getModuleFileName(execution.getContext(), app.getModuleName());

            if (fileName == null) {
                getStepLogger().debug(Messages.NO_CONTENT_TO_UPLOAD);

                return StepPhase.DONE;
            }

            getStepLogger().debug(Messages.UPLOADING_FILE_0_FOR_APP_1, fileName, app.getName());

            String uploadToken = asyncUploadFiles(execution, client, app, appArchiveId, fileName);
            getStepLogger().debug(Messages.STARTED_ASYNC_UPLOAD_OF_APP_0, app.getName());
            execution.getContext()
                .setVariable(Constants.VAR_UPLOAD_TOKEN, uploadToken);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            throw e;
        }
        return StepPhase.POLL;
    }

    private String asyncUploadFiles(ExecutionWrapper execution, CloudControllerClient client, CloudApplication app, String appArchiveId,
        String fileName) throws FileStorageException {
        final StringBuilder uploadTokenBuilder = new StringBuilder();
        final DelegateExecution context = execution.getContext();
        FileDownloadProcessor uploadFileToControllerProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
            appArchiveId, appArchiveStream -> {
                Path filePath = null;
                long maxSize = configuration.getMaxResourceFileSize();
                try {
                    filePath = extractFromMtar(appArchiveStream, fileName, maxSize);
                    upload(execution, client, app, filePath, uploadTokenBuilder);
                } catch (IOException e) {
                    cleanUp(filePath);
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, fileName);
                } catch (CloudOperationException e) {
                    cleanUp(filePath);
                    throw e;
                }
            });

        fileService.processFileContent(uploadFileToControllerProcessor);
        return uploadTokenBuilder.toString();
    }

    protected Path extractFromMtar(InputStream appArchiveStream, String fileName, long maxSize) {
        ApplicationArchiveExtractor appExtractor = new ApplicationArchiveExtractor(appArchiveStream, fileName, maxSize, getStepLogger());
        return appExtractor.extract();
    }

    private void upload(ExecutionWrapper execution, CloudControllerClient client, CloudApplication app, Path filePath,
        final StringBuilder uploadTokenBuilder) throws IOException {
        detectApplicationFileDigestChanges(execution, app, filePath.toFile(), client);
        String uploadToken = client.asyncUploadApplication(app.getName(), filePath.toFile(),
            getMonitorUploadStatusCallback(app, filePath.toFile(), execution.getContext()));
        uploadTokenBuilder.append(uploadToken);
    }

    private void detectApplicationFileDigestChanges(ExecutionWrapper execution, CloudApplication app, File applicationFile,
        CloudControllerClient client) {
        CloudApplication existingApp = client.getApplication(app.getName());
        ApplicationFileDigestDetector applicationFileDigestDetector = new ApplicationFileDigestDetector(existingApp);
        String appNewFileDigest = applicationFileDigestDetector.detectNewAppFileDigest(applicationFile);
        String currentFileDigest = applicationFileDigestDetector.detectCurrentAppFileDigest();
        attemptToUpdateApplicationDigest(client, app, appNewFileDigest, currentFileDigest);
        setAppContentChanged(execution, hasAppFileDigestChanged(appNewFileDigest, currentFileDigest));
    }

    private void attemptToUpdateApplicationDigest(CloudControllerClient client, CloudApplication app, String newFileDigest,
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

    MonitorUploadStatusCallback getMonitorUploadStatusCallback(CloudApplication app, File file, DelegateExecution context) {
        return new MonitorUploadStatusCallback(app, file, context);
    }

    private void cleanUp(Path filePath) {
        if (filePath == null) {
            return;
        }
        File file = filePath.toFile();
        if (!file.exists()) {
            return;
        }

        try {
            getStepLogger().debug(Messages.DELETING_TEMP_FILE, filePath);
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            getStepLogger().warn(Messages.ERROR_DELETING_APP_TEMP_FILE, filePath.toAbsolutePath());
        }
    }

    class MonitorUploadStatusCallback implements UploadStatusCallbackExtended {

        static final String FINISHED_STATUS = "finished";

        private final CloudApplication app;
        private final File file;
        private final DelegateExecution context;

        public MonitorUploadStatusCallback(CloudApplication app, File file, DelegateExecution context) {
            this.app = app;
            this.file = file;
            this.context = context;
        }

        @Override
        public void onCheckResources() {
            getStepLogger().debug("Resources checked");
        }

        @Override
        public void onMatchedFileNames(Set<String> matchedFileNames) {
            getStepLogger().debug(Messages.MATCHED_FILES_COUNT_0, matchedFileNames.size());
        }

        @Override
        public void onProcessMatchedResources(int length) {
            getStepLogger().debug(Messages.MATCHED_RESROUCES_PROCESSED_TOTAL_SIZE_0, length);
        }

        @Override
        public boolean onProgress(String status) {
            getStepLogger().debug(Messages.UPLOAD_STATUS_0, status);
            if (status.equals(FINISHED_STATUS)) {
                cleanUp(file.toPath());
                getProcessLogsPersister().persistLogs(context);
            }
            return false;
        }

        @Override
        public void onError(Exception e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP, app.getName());
            cleanUp(file.toPath());
        }

        @Override
        public void onError(String description) {
            getStepLogger().error(Messages.ERROR_UPLOADING_APP_BECAUSE_OF, app.getName(), description);
            cleanUp(file.toPath());
        }

    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_MODULES_INDEX;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
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
