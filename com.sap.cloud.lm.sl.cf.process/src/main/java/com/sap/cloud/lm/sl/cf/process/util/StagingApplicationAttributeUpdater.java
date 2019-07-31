package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;

public class StagingApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public StagingApplicationAttributeUpdater(CloudApplication existingApp, StepLogger stepLogger) {
        super(existingApp, stepLogger);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication app) {
        Staging staging = app.getStaging();
        Staging existingStaging = existingApp.getStaging();
        return hasStagingChanged(staging, existingStaging);
    }

    private boolean hasStagingChanged(Staging staging, Staging existingStaging) {
        String buildpackUrl = staging.getBuildpackUrl();
        String command = staging.getCommand();
        String stack = staging.getStack();
        Integer healthCheckTimeout = staging.getHealthCheckTimeout();
        String healthCheckType = staging.getHealthCheckType();
        String healthCheckHttpEndpoint = staging.getHealthCheckHttpEndpoint();
        Boolean sshEnabled = staging.isSshEnabled();
        return (buildpackUrl != null && !buildpackUrl.equals(existingStaging.getBuildpackUrl()))
            || (command != null && !command.equals(existingStaging.getCommand()))
            || (stack != null && !stack.equals(existingStaging.getStack()))
            || (healthCheckTimeout != null && !healthCheckTimeout.equals(existingStaging.getHealthCheckTimeout()))
            || (healthCheckType != null && !healthCheckType.equals(existingStaging.getHealthCheckType()))
            || (healthCheckHttpEndpoint != null && !healthCheckHttpEndpoint.equals(existingStaging.getHealthCheckHttpEndpoint()))
            || (sshEnabled != null && !sshEnabled.equals(existingStaging.isSshEnabled())
                || isDockerInfoModified(existingStaging.getDockerInfo(), staging.getDockerInfo()));
    }

    private boolean isDockerInfoModified(DockerInfo existingDockerInfo, DockerInfo newDockerInfo) {
        return existingDockerInfo != null && newDockerInfo != null && !existingDockerInfo.getImage()
                                                                                         .equals(newDockerInfo.getImage());
    }

    @Override
    protected UpdateState updateApplicationAttribute(CloudControllerClient client, CloudApplication app) {
        stepLogger.debug("Updating staging of application \"{0}\"", app.getName());
        client.updateApplicationStaging(app.getName(), app.getStaging());
        return UpdateState.UPDATED;
    }

}
