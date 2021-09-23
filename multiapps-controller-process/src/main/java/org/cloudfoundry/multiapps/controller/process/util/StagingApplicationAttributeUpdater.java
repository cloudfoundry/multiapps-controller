package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Objects;

import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.Staging;

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
        String stackName = staging.getStackName();
        Integer healthCheckTimeout = staging.getHealthCheckTimeout();
        Integer healthCheckInvocationTimeout = staging.getInvocationTimeout();
        String healthCheckType = staging.getHealthCheckType();
        String healthCheckHttpEndpoint = staging.getHealthCheckHttpEndpoint();
        Boolean sshEnabled = staging.isSshEnabled();
        return !Objects.equals(buildpacks, existingStaging.getBuildpacks())
            || !Objects.equals(command, existingStaging.getCommand())
            || !Objects.equals(stackName, existingStaging.getStackName())
            || !Objects.equals(healthCheckTimeout, existingStaging.getHealthCheckTimeout())
            || !Objects.equals(healthCheckType, existingStaging.getHealthCheckType())
            || !Objects.equals(healthCheckInvocationTimeout, existingStaging.getInvocationTimeout())
            || !Objects.equals(healthCheckHttpEndpoint, existingStaging.getHealthCheckHttpEndpoint())
            || !Objects.equals(sshEnabled, existingStaging.isSshEnabled())
            || !Objects.equals(staging.getDockerInfo(), existingStaging.getDockerInfo());
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        getControllerClient().updateApplicationStaging(application.getName(), application.getStaging());
    }

}
