package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Objects;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.DropletInfoFactory;
import org.cloudfoundry.multiapps.controller.client.lib.domain.HealthCheckInfo;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudProcess;
import com.sap.cloudfoundry.client.facade.domain.Staging;

public class StagingApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    private final CloudProcess existingProcess;
    private final DropletInfoFactory dropletInfoFactory = new DropletInfoFactory();

    public StagingApplicationAttributeUpdater(Context context, CloudProcess process) {
        super(context, UpdateStrategy.REPLACE);
        this.existingProcess = process;
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        Staging staging = application.getStaging();
        boolean isSshEnabled = getControllerClient().getApplicationSshEnabled(existingApplication.getGuid());
        return hasStagingChanged(staging, existingApplication, isSshEnabled);
    }

    private boolean hasStagingChanged(Staging staging, CloudApplication existingApp, boolean existingSshEnabled) {
        String command = staging.getCommand();
        var healthCheck = HealthCheckInfo.fromStaging(staging);
        var existingHealthCheck = HealthCheckInfo.fromProcess(existingProcess);
        boolean sshEnabled = staging.isSshEnabled() != null && staging.isSshEnabled();
        var dropletInfo = dropletInfoFactory.createDropletInfo(staging);
        var existingDropletInfo = dropletInfoFactory.createDropletInfo(existingApp, getControllerClient());
        return !Objects.equals(command, existingProcess.getCommand())
            || !healthCheck.equals(existingHealthCheck)
            || sshEnabled != existingSshEnabled
            || !dropletInfo.equals(existingDropletInfo);
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        getControllerClient().updateApplicationStaging(application.getName(), application.getStaging());
    }

}
