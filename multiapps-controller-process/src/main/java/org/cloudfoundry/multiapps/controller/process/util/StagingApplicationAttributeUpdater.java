package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Objects;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

public class StagingApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public StagingApplicationAttributeUpdater(Context context) {
        super(context, UpdateStrategy.REPLACE);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Staging staging = application.getStaging();
        Staging existingStaging = existingApplication.getStaging();
        return hasStagingChanged(staging, existingStaging);
    }

    private boolean hasStagingChanged(Staging staging, Staging existingStaging) {
        List<String> buildpacks = staging.getBuildpacks();
        String command = staging.getCommand();
        String stack = staging.getStack();
        Integer healthCheckTimeout = staging.getHealthCheckTimeout();
        String healthCheckType = staging.getHealthCheckType();
        String healthCheckHttpEndpoint = staging.getHealthCheckHttpEndpoint();
        Boolean sshEnabled = staging.isSshEnabled();
        return (buildpacks != null && !buildpacks.equals(existingStaging.getBuildpacks()))
            || (command != null && !command.equals(existingStaging.getCommand()))
            || (stack != null && !stack.equals(existingStaging.getStack()))
            || (healthCheckTimeout != null && !healthCheckTimeout.equals(existingStaging.getHealthCheckTimeout()))
            || (healthCheckType != null && !healthCheckType.equals(existingStaging.getHealthCheckType()))
            || (healthCheckHttpEndpoint != null && !healthCheckHttpEndpoint.equals(existingStaging.getHealthCheckHttpEndpoint()))
            || (sshEnabled != null && !sshEnabled.equals(existingStaging.isSshEnabled())
                || isDockerInfoModified(existingStaging.getDockerInfo(), staging.getDockerInfo()));
    }

    private boolean isDockerInfoModified(DockerInfo existingDockerInfo, DockerInfo newDockerInfo) {
        return !Objects.equals(existingDockerInfo, newDockerInfo);
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        getControllerClient().updateApplicationStaging(application.getName(), application.getStaging());
    }

}
