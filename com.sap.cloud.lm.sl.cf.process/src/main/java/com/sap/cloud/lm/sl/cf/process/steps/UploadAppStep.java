package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadStatusCallbackExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationEnvironmentUpdater;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationFileDigestDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveContext;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationArchiveReader;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationStager;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationZipBuilder;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;

@Named("uploadAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UploadAppStep extends TimeoutAsyncFlowableStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadAppStep.class);

    static final int DEFAULT_APP_UPLOAD_TIMEOUT = (int) TimeUnit.HOURS.toSeconds(1);

    @Inject
    protected ApplicationArchiveReader applicationArchiveReader;
    @Inject
    protected ApplicationZipBuilder applicationZipBuilder;

    @Override
    public StepPhase executeAsyncStep(ProcessContext context) throws FileStorageException {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String appName = app.getName();

        getStepLogger().info(Messages.UPLOADING_APP, appName);
        CloudControllerClient client = context.getControllerClient();

        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
        String fileName = mtaArchiveElements.getModuleFileName(app.getModuleName());

        if (fileName == null) {
            getStepLogger().debug(Messages.NO_CONTENT_TO_UPLOAD);
            return StepPhase.DONE;
        }

        String newApplicationDigest = getNewApplicationDigest(context, appArchiveId, fileName);
        CloudApplication cloudApp = client.getApplication(appName);
        boolean contentChanged = detectApplicationFileDigestChanges(context, cloudApp, client, newApplicationDigest);
        if (!contentChanged && isAppStagedCorrectly(context, cloudApp)) {
            getStepLogger().info(Messages.CONTENT_OF_APPLICATION_0_IS_NOT_CHANGED, appName);
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.UPLOADING_FILE_0_FOR_APP_1, fileName, appName);
        UploadToken uploadToken = asyncUploadFiles(context, client, app, appArchiveId, fileName);

        getStepLogger().debug(Messages.STARTED_ASYNC_UPLOAD_OF_APP_0, appName);
        context.setVariable(Variables.UPLOAD_TOKEN, uploadToken);
        return StepPhase.POLL;
    }

    private boolean isAppStagedCorrectly(ProcessContext context, CloudApplication cloudApp) {
        ApplicationStager appStager = new ApplicationStager(context);
        return appStager.isApplicationStagedCorrectly(cloudApp);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_UPLOADING_APP_0, context.getVariable(Variables.APP_TO_PROCESS)
                                                                           .getName());
    }

    private String getNewApplicationDigest(ProcessContext context, String appArchiveId, String fileName) throws FileStorageException {
        StringBuilder digestStringBuilder = new StringBuilder();
        fileService.processFileContent(context.getVariable(Variables.SPACE_ID), appArchiveId,
                                       createDigestCalculatorFileContentProcessor(digestStringBuilder, fileName));
        return digestStringBuilder.toString();
    }

    private FileContentProcessor createDigestCalculatorFileContentProcessor(StringBuilder digestStringBuilder, String fileName) {
        return appArchiveStream -> {
            long maxSize = configuration.getMaxResourceFileSize();
            ApplicationArchiveContext applicationArchiveContext = createApplicationArchiveContext(appArchiveStream, fileName, maxSize);
            digestStringBuilder.append(applicationArchiveReader.calculateApplicationDigest(applicationArchiveContext));
        };
    }

    protected ApplicationArchiveContext createApplicationArchiveContext(InputStream appArchiveStream, String fileName, long maxSize) {
        return new ApplicationArchiveContext(appArchiveStream, fileName, maxSize);
    }

    private UploadToken asyncUploadFiles(ProcessContext context, CloudControllerClient client, CloudApplication app, String appArchiveId,
                                         String fileName)
        throws FileStorageException {
        AtomicReference<UploadToken> uploadTokenReference = new AtomicReference<>();

        fileService.processFileContent(context.getVariable(Variables.SPACE_ID), appArchiveId, appArchiveStream -> {
            Path filePath = null;
            long maxSize = configuration.getMaxResourceFileSize();
            try {
                ApplicationArchiveContext applicationArchiveContext = createApplicationArchiveContext(appArchiveStream, fileName, maxSize);
                filePath = extractFromMtar(applicationArchiveContext);
                UploadToken uploadToken = upload(context, client, app, filePath);
                uploadTokenReference.set(uploadToken);
            } catch (IOException e) {
                FileUtils.cleanUp(filePath, LOGGER);
                throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_MODULE_CONTENT, fileName);
            } catch (CloudOperationException e) {
                FileUtils.cleanUp(filePath, LOGGER);
                throw e;
            }
        });
        return uploadTokenReference.get();
    }

    protected Path extractFromMtar(ApplicationArchiveContext applicationArchiveContext) {
        return applicationZipBuilder.extractApplicationInNewArchive(applicationArchiveContext);
    }

    private UploadToken upload(ProcessContext context, CloudControllerClient client, CloudApplication app, Path filePath)
        throws IOException {
        return client.asyncUploadApplication(app.getName(), filePath.toFile(),
                                             getMonitorUploadStatusCallback(context, app, filePath.toFile()));
    }

    private boolean detectApplicationFileDigestChanges(ProcessContext context, CloudApplication appWithUpdatedEnvironment,
                                                       CloudControllerClient client, String newApplicationDigest) {
        ApplicationFileDigestDetector digestDetector = new ApplicationFileDigestDetector(appWithUpdatedEnvironment.getEnv());
        String currentApplicationDigest = digestDetector.detectCurrentAppFileDigest();
        boolean contentChanged = !newApplicationDigest.equals(currentApplicationDigest);
        if (contentChanged) {
            attemptToUpdateApplicationDigest(client, appWithUpdatedEnvironment, newApplicationDigest);
        }
        setAppContentChanged(context, contentChanged);
        return contentChanged;
    }

    private void attemptToUpdateApplicationDigest(CloudControllerClient client, CloudApplication app, String newApplicationDigest) {
        new ApplicationEnvironmentUpdater(app,
                                          client).updateApplicationEnvironment(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                               com.sap.cloud.lm.sl.cf.core.Constants.ATTR_APP_CONTENT_DIGEST,
                                                                               newApplicationDigest);
    }

    private void setAppContentChanged(ProcessContext context, boolean appContentChanged) {
        context.setVariable(Variables.APP_CONTENT_CHANGED, appContentChanged);
    }

    MonitorUploadStatusCallback getMonitorUploadStatusCallback(ProcessContext context, CloudApplication app, File file) {
        return new MonitorUploadStatusCallback(context, app, file);
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollUploadAppStatusExecution());
    }

    @Override
    public Integer getTimeout(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        int uploadTimeout = extractUploadTimeoutFromAppAttributes(app, DEFAULT_APP_UPLOAD_TIMEOUT);
        getStepLogger().debug(Messages.UPLOAD_APP_TIMEOUT, uploadTimeout);
        return uploadTimeout;
    }

    private int extractUploadTimeoutFromAppAttributes(CloudApplication app, int defaultAppUploadTimeout) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        Number uploadTimeout = appAttributes.get(SupportedParameters.UPLOAD_TIMEOUT, Number.class, defaultAppUploadTimeout);
        return uploadTimeout.intValue();
    }

    class MonitorUploadStatusCallback implements UploadStatusCallbackExtended {

        private final CloudApplication app;
        private final File file;
        private final ProcessContext context;

        public MonitorUploadStatusCallback(ProcessContext context, CloudApplication app, File file) {
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
            getStepLogger().debug(Messages.MATCHED_RESOURCES_PROCESSED_TOTAL_SIZE_0, length);
        }

        @Override
        public boolean onProgress(String status) {
            getStepLogger().debug(Messages.UPLOAD_STATUS_0, status);
            if (status.equals(Status.READY.toString())) {
                FileUtils.cleanUp(file.toPath(), LOGGER);
                getProcessLogsPersister().persistLogs(context.getVariable(Variables.CORRELATION_ID),
                                                      context.getVariable(Variables.TASK_ID));
            }
            return false;
        }

        @Override
        public void onError(Exception e) {
            getStepLogger().error(e, Messages.ERROR_UPLOADING_APP_0, app.getName());
            FileUtils.cleanUp(file.toPath(), LOGGER);
        }

        @Override
        public void onError(String description) {
            getStepLogger().error(Messages.ERROR_UPLOADING_APP_0_STATUS_1_DESCRIPTION_2, app.getName(), Status.FAILED, description);
            FileUtils.cleanUp(file.toPath(), LOGGER);
        }

    }

}