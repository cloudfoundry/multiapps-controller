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
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationStager {

    private final ProcessContext context;
    private final StepLogger logger;
    private final CloudControllerClient client;

    public ApplicationStager(ProcessContext context) {
        this.context = context;
        this.logger = context.getStepLogger();
        this.client = context.getControllerClient();
    }

    public StagingState getStagingState() {
        UUID buildGuid = (UUID) context.getExecution()
                                       .getVariable(Constants.VAR_BUILD_GUID);
        if (buildGuid == null) {
            return ImmutableStagingState.builder()
                                        .state(PackageState.STAGED)
                                        .build();
        }
        CloudBuild build = getBuild(buildGuid);
        return getStagingState(build);
    }

    private CloudBuild getBuild(UUID buildGuid) {
        try {
            return client.getBuild(buildGuid);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                checkIfApplicationExists();
            }
            throw e;
        }
    }

    private void checkIfApplicationExists() {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
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

    public boolean isApplicationStagedCorrectly(CloudApplication app) {
        // TODO Remove the null filtering.
        // We are not sure if the controller is returning null for created_at or not, so after the proper v3 client adoption,
        // we should decide what to do with this filtering.
        List<CloudBuild> buildsForApplication = client.getBuildsForApplication(app.getMetadata()
                                                                                  .getGuid());
        if (containsNullMetadata(buildsForApplication)) {
            return false;
        }
        CloudBuild build = getLastBuild(buildsForApplication);
        if (build == null) {
            logger.debug(Messages.NO_BUILD_FOUND_FOR_APPLICATION, app.getName());
            return false;
        }
        if (isBuildStagedCorrectly(build)) {
            return true;
        }
        logMessages(app, build);
        return false;
    }

    private boolean containsNullMetadata(List<CloudBuild> buildsForApplication) {
        return buildsForApplication.stream()
                                   .anyMatch(build -> build.getMetadata() == null || build.getMetadata()
                                                                                          .getCreatedAt() == null);
    }

    private CloudBuild getLastBuild(List<CloudBuild> builds) {
        return builds.stream()
                     .max(Comparator.comparing(build -> build.getMetadata()
                                                             .getCreatedAt()))
                     .orElse(null);
    }

    private boolean isBuildStagedCorrectly(CloudBuild build) {
        return build.getState() == CloudBuild.State.STAGED && build.getDropletInfo() != null && build.getError() == null;
    }

    private void logMessages(CloudApplication app, CloudBuild build) {
        logger.info(Messages.APPLICATION_NOT_STAGED_CORRECTLY, app.getName());
        logger.debug(Messages.LAST_BUILD, JsonUtil.toJson(build));
    }

    public void bindDropletToApplication(UUID appGuid) {
        UUID buildGuid = (UUID) context.getExecution()
                                       .getVariable(Constants.VAR_BUILD_GUID);
        client.bindDropletToApp(client.getBuild(buildGuid)
                                      .getDropletInfo()
                                      .getGuid(),
                                appGuid);
    }

    public StepPhase stageApp(CloudApplication app) {
        UploadToken uploadToken = context.getVariable(Variables.UPLOAD_TOKEN);
        if (uploadToken == null) {
            return StepPhase.DONE;
        }
        logger.info(Messages.STAGING_APP, app.getName());
        return createBuild(uploadToken.getPackageGuid());
    }

    private StepPhase createBuild(UUID packageGuid) {
        try {
            context.getExecution()
                   .setVariable(Constants.VAR_BUILD_GUID, client.createBuild(packageGuid)
                                                                .getMetadata()
                                                                .getGuid());
        } catch (CloudOperationException e) {
            handleCloudOperationException(e, packageGuid);
        }
        return StepPhase.POLL;
    }

    private void handleCloudOperationException(CloudOperationException e, UUID packageGuid) {
        if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            logger.info(Messages.BUILD_FOR_PACKAGE_0_ALREADY_EXISTS, packageGuid);
            logger.warn(e, e.getMessage());
            processLastBuild(packageGuid);
            return;
        }
        throw e;
    }

    private void processLastBuild(UUID packageGuid) {
        CloudBuild lastBuild = getLastBuild(client.getBuildsForPackage(packageGuid));
        if (lastBuild == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, format(Messages.NO_BUILDS_FOUND_FOR_PACKAGE, packageGuid));
        }
        context.getExecution()
               .setVariable(Constants.VAR_BUILD_GUID, lastBuild.getMetadata()
                                                               .getGuid());
    }
}
