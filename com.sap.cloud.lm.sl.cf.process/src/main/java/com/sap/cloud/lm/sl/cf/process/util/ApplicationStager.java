package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

public class ApplicationStager {

    private final CloudControllerClient client;

    public ApplicationStager(CloudControllerClient client) {
        this.client = client;
    }

    public StagingState getStagingState(DelegateExecution context) {
        UUID buildGuid = (UUID) context.getVariable(Constants.VAR_BUILD_GUID);
        if (buildGuid == null) {
            return ImmutableStagingState.builder()
                                        .state(PackageState.STAGED)
                                        .build();
        }
        CloudBuild build = getBuild(context, buildGuid);
        return getStagingState(build);
    }

    private CloudBuild getBuild(DelegateExecution context, UUID buildGuid) {
        try {
            return client.getBuild(buildGuid);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                checkIfApplicationExists(context);
            }
            throw e;
        }
    }

    private void checkIfApplicationExists(DelegateExecution context) {
        CloudApplicationExtended app = StepsUtil.getApp(context);
        // This will produce an exception with a more meaningful message why the build is missing
        client.getApplication(app.getName());
    }

    private StagingState getStagingState(CloudBuild build) {
        PackageState packageState = getBuildState(build);
        ImmutableStagingState.Builder builder = ImmutableStagingState.builder()
                                                                     .state(packageState);
        if (packageState == PackageState.FAILED) {
            builder.error(build.getError());
        }
        return builder.build();
    }

    private PackageState getBuildState(CloudBuild build) {
        switch (build.getState()) {
            case FAILED:
                return PackageState.FAILED;
            case STAGED:
                return PackageState.STAGED;
            case STAGING:
                return PackageState.PENDING;
        }
        throw new IllegalArgumentException("Invalid build state");
    }

    public boolean isApplicationStagedCorrectly(StepLogger stepLogger, CloudApplication cloudApplication) {
        // TODO Remove the null filtering.
        // We are not sure if the controller is returning null for created_at or not, so after the proper v3 client adoption,
        // we should decide what to do with this filtering.
        List<CloudBuild> buildsForApplication = client.getBuildsForApplication(cloudApplication.getMetadata()
                                                                                               .getGuid());
        if (containsNullMetadata(buildsForApplication)) {
            return false;
        }
        CloudBuild cloudBuild = getLastBuild(buildsForApplication);
        if (cloudBuild == null) {
            stepLogger.debug(Messages.NO_BUILD_FOUND_FOR_APPLICATION, cloudApplication.getName());
            return false;
        }
        if (isCloudBuildStagedCorrectly(cloudBuild)) {
            return true;
        }
        logMessages(stepLogger, cloudApplication, cloudBuild);
        return false;
    }

    private boolean containsNullMetadata(List<CloudBuild> buildsForApplication) {
        return buildsForApplication.stream()
                                   .anyMatch(build -> build.getMetadata() == null || build.getMetadata()
                                                                                          .getCreatedAt() == null);
    }

    private CloudBuild getLastBuild(List<CloudBuild> cloudBuilds) {
        return cloudBuilds.stream()
                          .max(Comparator.comparing(build -> build.getMetadata()
                                                                  .getCreatedAt()))
                          .orElse(null);
    }

    private boolean isCloudBuildStagedCorrectly(CloudBuild cloudBuild) {
        return cloudBuild.getState() == CloudBuild.State.STAGED && cloudBuild.getDropletInfo() != null && cloudBuild.getError() == null;
    }

    private void logMessages(StepLogger stepLogger, CloudApplication cloudApplication, CloudBuild cloudBuild) {
        stepLogger.info(Messages.APPLICATION_NOT_STAGED_CORRECTLY, cloudApplication.getName());
        stepLogger.debug(Messages.LAST_BUILD, JsonUtil.convertToJson(cloudBuild));
    }

    public void bindDropletToApplication(DelegateExecution context, UUID appGuid) {
        UUID buildGuid = (UUID) context.getVariable(Constants.VAR_BUILD_GUID);
        client.bindDropletToApp(client.getBuild(buildGuid)
                                      .getDropletInfo()
                                      .getGuid(),
                                appGuid);
    }

    public StepPhase stageApp(DelegateExecution context, CloudApplication app, StepLogger stepLogger) {
        UploadToken uploadToken = StepsUtil.getUploadToken(context);
        if (uploadToken == null) {
            return StepPhase.DONE;
        }
        stepLogger.info(Messages.STAGING_APP, app.getName());
        return createBuild(context, uploadToken.getPackageGuid(), stepLogger);
    }

    private StepPhase createBuild(DelegateExecution context, UUID packageGuid, StepLogger stepLogger) {
        try {
            context.setVariable(Constants.VAR_BUILD_GUID, client.createBuild(packageGuid)
                                                                .getMetadata()
                                                                .getGuid());
        } catch (CloudOperationException e) {
            handleCloudOperationException(e, context, packageGuid, stepLogger);
        }
        return StepPhase.POLL;
    }

    private void handleCloudOperationException(CloudOperationException e, DelegateExecution context, UUID packageGuid,
                                               StepLogger stepLogger) {
        if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            stepLogger.info(Messages.BUILD_FOR_PACKAGE_0_ALREADY_EXISTS, packageGuid);
            stepLogger.warn(e, e.getMessage());
            processLastBuild(packageGuid, context);
            return;
        }
        throw e;
    }

    private void processLastBuild(UUID packageGuid, DelegateExecution context) {
        CloudBuild lastBuild = getLastBuild(client.getBuildsForPackage(packageGuid));
        if (lastBuild == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, format(Messages.NO_BUILDS_FOUND_FOR_PACKAGE, packageGuid));
        }
        context.setVariable(Constants.VAR_BUILD_GUID, lastBuild.getMetadata()
                                                               .getGuid());
    }
}
