package org.cloudfoundry.multiapps.controller.process.util;

import static java.text.MessageFormat.format;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.PackageState;

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
        UUID buildGuid = context.getVariable(Variables.BUILD_GUID);
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
        client.getApplicationGuid(app.getName());
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
            default:
                throw new IllegalArgumentException("Invalid build state");
        }
    }

    public boolean isApplicationStagedCorrectly(CloudApplication app) {
        List<CloudBuild> buildsForApplication = client.getBuildsForApplication(app.getGuid());
        if (buildsForApplication.isEmpty()) {
            logger.debug(Messages.NO_BUILD_FOUND_FOR_APPLICATION, app.getName());
            return false;
        }
        Optional<DropletInfo> currentDropletForApplication = findOrReturnEmpty(() -> client.getCurrentDropletForApplication(app.getGuid()));
        if (currentDropletForApplication.isEmpty()) {
            logger.debug(Messages.APPLICATION_NOT_STAGED_CORRECTLY_MISSING_DROPLET, app.getName());
            return false;
        }
        CloudBuild build = getLastBuild(buildsForApplication);
        if (isBuildStagedCorrectly(build)) {
            return true;
        }
        logMessages(app, build);
        return false;
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
        logger.debug(Messages.LAST_BUILD, SecureSerialization.toJson(build));
    }

    public void bindDropletToApplication(UUID appGuid) {
        UUID buildGuid = context.getVariable(Variables.BUILD_GUID);
        client.bindDropletToApp(client.getBuild(buildGuid)
                                      .getDropletInfo()
                                      .getGuid(),
                                appGuid);
    }

    public StepPhase stageApp(CloudApplication app) {
        CloudPackage cloudPackage = context.getVariable(Variables.CLOUD_PACKAGE);
        if (cloudPackage == null) {
            return StepPhase.DONE;
        }
        logger.info(Messages.STAGING_APP, app.getName());
        return createBuild(cloudPackage.getGuid());
    }

    private StepPhase createBuild(UUID packageGuid) {
        try {
            context.setVariable(Variables.BUILD_GUID, client.createBuild(packageGuid)
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
        context.setVariable(Variables.BUILD_GUID, lastBuild.getGuid());
    }

    private Optional<DropletInfo> findOrReturnEmpty(Supplier<DropletInfo> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
