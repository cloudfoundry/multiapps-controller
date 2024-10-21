package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationFileDigestDetector;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationArchiveContext;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationDigestCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationStager;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationZipBuilder;
import org.cloudfoundry.multiapps.controller.process.util.CloudPackagesGetter;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.Status;

@Named("uploadAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UploadAppStep extends TimeoutAsyncFlowableStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadAppStep.class);
    @Inject
    protected ApplicationDigestCalculator applicationDigestCalculator;
    @Inject
    protected ApplicationZipBuilder applicationZipBuilder;
    @Inject
    protected CloudPackagesGetter cloudPackagesGetter;
    @Inject
    private ExecutorService appUploaderThreadPool;

    @Override
    public StepPhase executeAsyncStep(ProcessContext context) throws FileStorageException {
        CloudApplicationExtended applicationToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        getStepLogger().info(Messages.UPLOADING_APP, applicationToProcess.getName());

        MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
        String moduleFileName = mtaArchiveElements.getModuleFileName(applicationToProcess.getModuleName());
        CloudControllerClient client = context.getControllerClient();
        if (moduleFileName == null) {
            getStepLogger().debug(Messages.NO_CONTENT_TO_UPLOAD);
            if (applicationToProcess.getDockerInfo() != null) {
                CloudPackage cloudPackage = createDockerPackage(client, applicationToProcess);
                context.setVariable(Variables.CLOUD_PACKAGE, cloudPackage);
            }
            return StepPhase.DONE;
        }

        CloudApplication cloudApp = client.getApplication(applicationToProcess.getName());
        var appEnv = client.getApplicationEnvironment(cloudApp.getGuid());
        if (context.getVariable(Variables.SKIP_APP_DIGEST_CALCULATION)) {
            getStepLogger().infoWithoutProgressMessage(Messages.SKIPPING_APPLICATION_0_DIGEST_CALCULATION, applicationToProcess.getName());
            removeApplicationDigestIfSet(context, appEnv);
            return StepPhase.POLL;
        } else {
            getStepLogger().infoWithoutProgressMessage(Messages.CALCULATING_APPLICATION_DIGEST_0, applicationToProcess.getName());
            String newApplicationDigest = getNewApplicationDigest(context, moduleFileName);
            boolean contentChanged = detectApplicationFileDigestChanges(appEnv, newApplicationDigest);
            if (contentChanged) {
                context.setVariable(Variables.SHOULD_UPDATE_APPLICATION_DIGEST, true);
                context.setVariable(Variables.CALCULATED_APPLICATION_DIGEST, newApplicationDigest);
                return StepPhase.POLL;
            }
        }

        Optional<CloudPackage> mostRecentPackage = cloudPackagesGetter.getMostRecentAppPackage(client, cloudApp.getGuid());
        if (mostRecentPackage.isEmpty()) {
            return StepPhase.POLL;
        }

        CloudPackage latestPackage = mostRecentPackage.get();
        Optional<CloudPackage> currentPackage = cloudPackagesGetter.getAppPackage(client, cloudApp.getGuid());
        if (currentPackage.isEmpty() && isPackageInValidState(latestPackage)) {
            context.setVariable(Variables.SHOULD_SKIP_APPLICATION_UPLOAD, true);
            return useLatestPackage(context, latestPackage);
        }

        if (currentPackage.isEmpty() || !isAppStagedCorrectly(context, cloudApp)) {
            return StepPhase.POLL;
        }

        if (isPackageInValidState(latestPackage)
            && (context.getVariable(Variables.APP_NEEDS_RESTAGE) || !packagesMatch(currentPackage.get(), latestPackage))) {
            context.setVariable(Variables.SHOULD_SKIP_APPLICATION_UPLOAD, true);
            return useLatestPackage(context, latestPackage);
        }

        getStepLogger().info(Messages.CONTENT_OF_APPLICATION_0_IS_NOT_CHANGED, applicationToProcess.getName());
        context.setVariable(Variables.SHOULD_SKIP_APPLICATION_UPLOAD, true);
        return StepPhase.DONE;
    }

    private boolean packagesMatch(CloudPackage currentPackage, CloudPackage latestPackage) {
        return Objects.equals(currentPackage.getGuid(), latestPackage.getGuid());
    }

    private CloudPackage createDockerPackage(CloudControllerClient client, CloudApplicationExtended application) {
        UUID applicationGuid = client.getApplicationGuid(application.getName());
        return client.createDockerPackage(applicationGuid, application.getDockerInfo());
    }

    private String getNewApplicationDigest(ProcessContext context, String fileName) throws FileStorageException {
        ApplicationArchiveContext applicationArchiveContext = createApplicationArchiveContext(context, fileName);
        return applicationDigestCalculator.calculateApplicationDigest(applicationArchiveContext);
    }

    protected ApplicationArchiveContext createApplicationArchiveContext(ProcessContext context, String fileName) {
        long maxSize = configuration.getMaxResourceFileSize();
        List<ArchiveEntryWithStreamPositions> archiveEntryWithStreamPositions = context.getVariable(Variables.ARCHIVE_ENTRIES_POSITIONS);
        return new ApplicationArchiveContext(fileName,
                                             maxSize,
                                             archiveEntryWithStreamPositions,
                                             context.getRequiredVariable(Variables.SPACE_GUID),
                                             context.getRequiredVariable(Variables.APP_ARCHIVE_ID));
    }

    private boolean detectApplicationFileDigestChanges(Map<String, String> appEnv, String newApplicationDigest) {
        ApplicationFileDigestDetector digestDetector = new ApplicationFileDigestDetector(appEnv);
        String currentApplicationDigest = digestDetector.detectCurrentAppFileDigest();
        return !newApplicationDigest.equals(currentApplicationDigest);
    }

    private StepPhase useLatestPackage(ProcessContext context, CloudPackage latestUnusedPackage) {
        getStepLogger().debug(Messages.THE_NEWEST_PACKAGE_WILL_BE_USED_0, SecureSerialization.toJson(latestUnusedPackage));
        context.setVariable(Variables.CLOUD_PACKAGE, latestUnusedPackage);
        return StepPhase.POLL;
    }

    private boolean isPackageInValidState(CloudPackage cloudPackage) {
        LOGGER.info(format(Messages.PACKAGE_STATUS_0_IS_IN_STATE_1, cloudPackage.getGuid(), cloudPackage.getStatus()));
        return cloudPackage.getStatus() != Status.EXPIRED && cloudPackage.getStatus() != Status.FAILED
            && cloudPackage.getStatus() != Status.AWAITING_UPLOAD;
    }

    private boolean isAppStagedCorrectly(ProcessContext context, CloudApplication cloudApp) {
        ApplicationStager appStager = new ApplicationStager(context);
        return appStager.isApplicationStagedCorrectly(cloudApp);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return format(Messages.ERROR_UPLOADING_APP_0, context.getVariable(Variables.APP_TO_PROCESS)
                                                             .getName());
    }

    private void removeApplicationDigestIfSet(ProcessContext context, Map<String, String> appEnv) {
        String deployAttributesJsonValue = appEnv.get(Constants.ENV_DEPLOY_ATTRIBUTES);
        if (deployAttributesJsonValue == null) {
            return;
        }
        Map<String, Object> deployAttributes = JsonUtil.fromJson(deployAttributesJsonValue, new TypeReference<>() {
        });
        if (deployAttributes.containsKey(Constants.ATTR_APP_CONTENT_DIGEST)) {
            context.setVariable(Variables.SHOULD_UPDATE_APPLICATION_DIGEST, true);
        }
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new UploadAppAsyncExecution(fileService,
                                                   applicationZipBuilder,
                                                   getProcessLogsPersister(),
                                                   configuration,
                                                   appUploaderThreadPool),
                       new PollUploadAppStatusExecution());
    }

    @Override
    public Duration getTimeout(ProcessContext context) {
        return calculateTimeout(context, TimeoutType.UPLOAD);
    }

}