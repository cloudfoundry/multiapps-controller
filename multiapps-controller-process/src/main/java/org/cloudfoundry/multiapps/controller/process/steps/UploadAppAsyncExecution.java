package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.UploadStatusCallbackExtended;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationEnvironmentUpdater;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerPersister;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.context.ApplicationToUploadContext;
import org.cloudfoundry.multiapps.controller.process.context.ImmutableApplicationToUploadContext;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationArchiveContext;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationZipBuilder;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.Status;

public class UploadAppAsyncExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadAppAsyncExecution.class);

    private final FileService fileService;
    private final ApplicationZipBuilder applicationZipBuilder;
    private final ProcessLoggerPersister processLoggerPersister;
    private final ApplicationConfiguration applicationConfiguration;
    private final ExecutorService appUploaderThreadPool;

    public UploadAppAsyncExecution(FileService fileService, ApplicationZipBuilder applicationZipBuilder,
                                   ProcessLoggerPersister processLoggerPersister, ApplicationConfiguration applicationConfiguration,
                                   ExecutorService appUploaderThreadPool) {
        this.fileService = fileService;
        this.applicationZipBuilder = applicationZipBuilder;
        this.processLoggerPersister = processLoggerPersister;
        this.applicationConfiguration = applicationConfiguration;
        this.appUploaderThreadPool = appUploaderThreadPool;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudApplicationExtended applicationToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        if (context.getVariable(Variables.SHOULD_SKIP_APPLICATION_UPLOAD)) {
            context.getStepLogger()
                   .debug(Messages.SKIPPING_UPLOAD_OF_APPLICATION_0, applicationToProcess.getName());
            return AsyncExecutionState.FINISHED;
        }
        if (context.getVariable(Variables.UPLOAD_START_TIME) == null) {
            context.setVariable(Variables.UPLOAD_START_TIME, LocalDateTime.now());
        }
        ApplicationToUploadContext applicationToUploadContext = buildApplicationToUploadContext(context, applicationToProcess);
        CloudControllerClient client = context.getControllerClient();
        Future<CloudPackage> runningUpload;
        try {
            runningUpload = appUploaderThreadPool.submit(() -> doUpload(context, applicationToProcess, applicationToUploadContext));
        } catch (RejectedExecutionException rejectedExecutionException) {
            LOGGER.error(rejectedExecutionException.getMessage());
            context.getStepLogger()
                   .warnWithoutProgressMessage(Messages.UPLOAD_OF_APPLICATION_0_WAS_NOT_ACCEPTED_BY_INSTANCE_1,
                                               applicationToProcess.getName(), applicationConfiguration.getApplicationInstanceIndex());
            return AsyncExecutionState.RUNNING;
        }
        CloudPackage cloudPackage;
        try {
            cloudPackage = runningUpload.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SLException(e, e.getMessage());
        }
        LocalDateTime startTime = context.getVariable(Variables.UPLOAD_START_TIME);
        long timeElapsedForUpload = Duration.between(startTime, LocalDateTime.now())
                                            .toMillis();
        context.getStepLogger()
               .infoWithoutProgressMessage(Messages.TIME_ELAPSED_FOR_UPLOAD_0_IN_MILLIS, timeElapsedForUpload);
        return processCloudPackage(context, client, cloudPackage);
    }

    private ApplicationToUploadContext buildApplicationToUploadContext(ProcessContext context,
                                                                       CloudApplicationExtended applicationToProcess) {
        MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
        return ImmutableApplicationToUploadContext.builder()
                                                  .application(applicationToProcess)
                                                  .moduleFileName(mtaArchiveElements.getModuleFileName(applicationToProcess.getModuleName()))
                                                  .spaceGuid(context.getVariable(Variables.SPACE_GUID))
                                                  .correlationId(context.getVariable(Variables.CORRELATION_ID))
                                                  .taskId(context.getVariable(Variables.TASK_ID))
                                                  .appArchiveId(context.getRequiredVariable(Variables.APP_ARCHIVE_ID))
                                                  .stepLogger(context.getStepLogger())
                                                  .build();
    }

    private CloudPackage doUpload(ProcessContext context, CloudApplicationExtended applicationToProcess,
                                  ApplicationToUploadContext applicationToUploadContext) {
        context.getStepLogger()
               .infoWithoutProgressMessage(Messages.UPLOAD_OF_APPLICATION_0_STARTED_ON_INSTANCE_1, applicationToProcess.getName(),
                                           applicationConfiguration.getApplicationInstanceIndex());
        try {
            return proceedWithUpload(context.getControllerClient(), applicationToUploadContext);
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private CloudPackage proceedWithUpload(CloudControllerClient client, ApplicationToUploadContext applicationToUploadContext)
        throws FileStorageException {
        applicationToUploadContext.getStepLogger()
                                  .debug(Messages.UPLOADING_FILE_0_FOR_APP_1, applicationToUploadContext.getModuleFileName(),
                                         applicationToUploadContext.getApplication()
                                                                   .getName());
        CloudPackage cloudPackage = asyncUploadFiles(client, applicationToUploadContext);
        applicationToUploadContext.getStepLogger()
                                  .info(Messages.STARTED_ASYNC_UPLOAD_OF_APP_0, applicationToUploadContext.getApplication()
                                                                                                          .getName());
        LOGGER.info(format(Messages.UPLOADED_PACKAGE_0, cloudPackage));
        return cloudPackage;
    }

    private CloudPackage asyncUploadFiles(CloudControllerClient client, ApplicationToUploadContext applicationToUploadContext)
        throws FileStorageException {
        Path extractedAppPath = extractApplicationFromArchive(applicationToUploadContext);
        LOGGER.debug(MessageFormat.format(Messages.APPLICATION_WITH_NAME_0_SAVED_TO_1, applicationToUploadContext.getApplication()
                                                                                                                 .getName(),
                                          extractedAppPath));
        applicationToUploadContext.getStepLogger()
                                  .infoWithoutProgressMessage(Messages.SIZE_OF_APP_0_IS_1_BYTES, applicationToUploadContext.getApplication()
                                                                                                                           .getName(),
                                                              extractedAppPath.toFile()
                                                                              .length());
        return upload(client, applicationToUploadContext, extractedAppPath);
    }

    private Path extractApplicationFromArchive(ApplicationToUploadContext applicationToUploadContext) throws FileStorageException {
        LocalDateTime startTime = LocalDateTime.now();
        Path extractedAppPath = fileService.processFileContent(applicationToUploadContext.getSpaceGuid(),
                                                               applicationToUploadContext.getAppArchiveId(),
                                                               appArchiveStream -> extractFromMtar(createApplicationArchiveContext(appArchiveStream,
                                                                                                                                   applicationToUploadContext.getModuleFileName(),
                                                                                                                                   applicationConfiguration.getMaxResourceFileSize())));
        long timeElapsedForUpload = Duration.between(startTime, LocalDateTime.now())
                                            .toMillis();
        applicationToUploadContext.getStepLogger()
                                  .infoWithoutProgressMessage(Messages.TIME_ELAPSED_FOR_APP_BINARY_DOWNLOAD_0_IN_MILLIS,
                                                              timeElapsedForUpload);
        return extractedAppPath;
    }

    protected ApplicationArchiveContext createApplicationArchiveContext(InputStream appArchiveStream, String fileName, long maxSize) {
        return new ApplicationArchiveContext(appArchiveStream, fileName, maxSize);
    }

    protected Path extractFromMtar(ApplicationArchiveContext applicationArchiveContext) {
        return applicationZipBuilder.extractApplicationInNewArchive(applicationArchiveContext);
    }

    private CloudPackage upload(CloudControllerClient client, ApplicationToUploadContext applicationToUploadContext,
                                Path extractedModulePath) {
        try {
            return client.asyncUploadApplicationWithExponentialBackoff(applicationToUploadContext.getApplication()
                                                                                                 .getName(),
                                                                       extractedModulePath,
                                                                       getMonitorUploadStatusCallback(applicationToUploadContext.getApplication(),
                                                                                                      extractedModulePath,
                                                                                                      applicationToUploadContext.getStepLogger(),
                                                                                                      applicationToUploadContext.getCorrelationId(),
                                                                                                      applicationToUploadContext.getTaskId()),
                                                                       null);
        } catch (Exception e) {
            FileUtils.cleanUp(extractedModulePath, LOGGER);
            throw new SLException(e,
                                  Messages.ERROR_WHILE_STARTING_ASYNC_UPLOAD_OF_APP_WITH_NAME_0,
                                  applicationToUploadContext.getApplication()
                                                            .getName());
        }
    }

    private AsyncExecutionState processCloudPackage(ProcessContext context, CloudControllerClient client, CloudPackage cloudPackage) {
        CloudApplication cloudApp = client.getApplication(context.getVariable(Variables.APP_TO_PROCESS)
                                                                 .getName());
        if (context.getVariable(Variables.SHOULD_UPDATE_APPLICATION_DIGEST)) {
            var appEnv = client.getApplicationEnvironment(cloudApp.getGuid());
            String newApplicationDigest = context.getVariable(Variables.CALCULATED_APPLICATION_DIGEST);
            attemptToUpdateApplicationDigest(client, cloudApp, appEnv, newApplicationDigest);
        }
        context.setVariable(Variables.CLOUD_PACKAGE, cloudPackage);
        context.setVariable(Variables.APP_CONTENT_CHANGED, true);
        return AsyncExecutionState.FINISHED;
    }

    private void attemptToUpdateApplicationDigest(CloudControllerClient client, CloudApplication app, Map<String, String> appEnv,
                                                  String newApplicationDigest) {
        new ApplicationEnvironmentUpdater(app, appEnv, client).updateApplicationEnvironment(Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                                            Constants.ATTR_APP_CONTENT_DIGEST,
                                                                                            newApplicationDigest);
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_OCCURRED_DURING_APPLICATION_UPLOAD_0, appToProcess.getName());
    }

    MonitorUploadStatusCallback getMonitorUploadStatusCallback(CloudApplication app, Path file, StepLogger stepLogger, String correlationId,
                                                               String taskId) {
        return new MonitorUploadStatusCallback(app, file, stepLogger, correlationId, taskId);
    }

    class MonitorUploadStatusCallback implements UploadStatusCallbackExtended {

        private final CloudApplication app;
        private final Path file;
        private final StepLogger stepLogger;
        private final String correlationId;
        private final String taskId;

        public MonitorUploadStatusCallback(CloudApplication app, Path file, StepLogger stepLogger, String correlationId, String taskId) {
            this.app = app;
            this.file = file;
            this.stepLogger = stepLogger;
            this.correlationId = correlationId;
            this.taskId = taskId;
        }

        @Override
        public void onCheckResources() {
            stepLogger.debug("Resources checked");
        }

        @Override
        public void onMatchedFileNames(Set<String> matchedFileNames) {
            stepLogger.debug(Messages.MATCHED_FILES_COUNT_0, matchedFileNames.size());
        }

        @Override
        public void onProcessMatchedResources(int length) {
            stepLogger.debug(Messages.MATCHED_RESOURCES_PROCESSED_TOTAL_SIZE_0, length);
        }

        @Override
        public boolean onProgress(String status) {
            stepLogger.debug(Messages.UPLOAD_STATUS_0, status);
            if (status.equals(Status.READY.toString())) {
                FileUtils.cleanUp(file, LOGGER);
                processLoggerPersister.persistLogs(correlationId, taskId);
            }
            return false;
        }

        @Override
        public void onError(Exception e) {
            stepLogger.error(e, Messages.ERROR_UPLOADING_APP_0, app.getName());
            FileUtils.cleanUp(file, LOGGER);
        }

        @Override
        public void onError(String description) {
            stepLogger.error(Messages.ERROR_UPLOADING_APP_0_STATUS_1_DESCRIPTION_2, app.getName(), Status.FAILED, description);
            FileUtils.cleanUp(file, LOGGER);
        }
    }

}
