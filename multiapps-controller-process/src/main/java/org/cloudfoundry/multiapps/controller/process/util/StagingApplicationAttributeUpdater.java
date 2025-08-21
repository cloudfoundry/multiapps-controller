package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.DropletInfoFactory;
import org.cloudfoundry.multiapps.controller.client.lib.domain.HealthCheckInfo;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class StagingApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    private final ProcessContext processContext;
    private final CloudProcess existingProcess;
    private final DropletInfoFactory dropletInfoFactory = new DropletInfoFactory();

    public StagingApplicationAttributeUpdater(Context context, CloudProcess process) {
        super(context, UpdateStrategy.REPLACE);
        this.processContext = getProcessContext();
        this.existingProcess = process;
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        Staging staging = application.getStaging();
        Map<String, Boolean> appFeatures = getControllerClient().getApplicationFeatures(existingApplication.getGuid());
        return hasStagingChanged(staging, existingApplication, appFeatures);
    }

    private boolean hasStagingChanged(Staging staging, CloudApplication existingApp, Map<String, Boolean> existingAppFeatures) {
        String command = staging.getCommand();
        var healthCheck = HealthCheckInfo.fromStaging(staging);
        var existingHealthCheck = HealthCheckInfo.fromProcess(existingProcess);
        var dropletInfo = dropletInfoFactory.createDropletInfo(staging);
        var existingDropletInfo = dropletInfoFactory.createDropletInfo(existingApp, getControllerClient());

        if (!dropletInfo.equals(existingDropletInfo) || isCommandDifferent(command)) {
            processContext.setVariable(Variables.APP_NEEDS_RESTAGE, true);
            return true;
        }
        return !healthCheck.equals(existingHealthCheck) || areAppFeaturesChanged(staging.getAppFeatures(), existingAppFeatures)
            || !hasReadinessHealthCheckNotBeenChanged(staging);
    }

    private boolean isCommandDifferent(String newCommand) {
        return !StringUtils.isBlank(newCommand) && !Objects.equals(newCommand, existingProcess.getCommand());
    }

    private boolean areAppFeaturesChanged(Map<String, Boolean> newAppFeatures, Map<String, Boolean> existingAppFeatures) {
        return newAppFeatures.entrySet()
                             .stream()
                             .anyMatch(newAppFeature -> !Objects.equals(newAppFeature.getValue(),
                                                                        existingAppFeatures.get(newAppFeature.getKey())));
    }

    private boolean hasReadinessHealthCheckNotBeenChanged(Staging newStaging) {
        return Objects.equals(newStaging.getReadinessHealthCheckHttpEndpoint(), existingProcess.getReadinessHealthCheckHttpEndpoint())
            && Objects.equals(newStaging.getReadinessHealthCheckInterval(), existingProcess.getReadinessHealthCheckInterval())
            && Objects.equals(newStaging.getReadinessHealthCheckInvocationTimeout(),
                              existingProcess.getReadinessHealthCheckInvocationTimeout())
            && Objects.equals(newStaging.getReadinessHealthCheckType(), existingProcess.getReadinessHealthCheckType());
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        getControllerClient().updateApplicationStaging(application.getName(), application.getStaging());
    }

}
