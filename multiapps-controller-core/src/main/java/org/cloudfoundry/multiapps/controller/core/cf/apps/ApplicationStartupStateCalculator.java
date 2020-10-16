package org.cloudfoundry.multiapps.controller.core.cf.apps;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named
public class ApplicationStartupStateCalculator {

    public ApplicationStartupState computeDesiredState(CloudApplication app, boolean shouldNotStartAnyApp) {
        if (hasExecuteAppParameter(app)) {
            return ApplicationStartupState.EXECUTED;
        }

        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        boolean shouldNotStartApp = appAttributes.get(SupportedParameters.NO_START, Boolean.class, shouldNotStartAnyApp);
        return shouldNotStartApp ? ApplicationStartupState.STOPPED : ApplicationStartupState.STARTED;
    }

    public ApplicationStartupState computeCurrentState(CloudApplication app) {
        if (hasExecuteAppParameter(app)) {
            return ApplicationStartupState.EXECUTED;
        }
        if (isStarted(app)) {
            return ApplicationStartupState.STARTED;
        }
        if (isStopped(app)) {
            return ApplicationStartupState.STOPPED;
        }
        return ApplicationStartupState.INCONSISTENT;
    }

    private boolean hasExecuteAppParameter(CloudApplication app) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        return appAttributes.get(SupportedParameters.EXECUTE_APP, Boolean.class, false);
    }

    private boolean isStarted(CloudApplication app) {
        return app.getRunningInstances() == app.getInstances() && app.getInstances() != 0 && app.getState()
                                                                                                .equals(CloudApplication.State.STARTED);
    }

    private boolean isStopped(CloudApplication app) {
        return app.getRunningInstances() == 0 && app.getState()
                                                    .equals(CloudApplication.State.STOPPED);
    }

}
