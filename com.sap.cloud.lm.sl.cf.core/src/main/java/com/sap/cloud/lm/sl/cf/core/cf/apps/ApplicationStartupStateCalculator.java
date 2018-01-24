package com.sap.cloud.lm.sl.cf.core.cf.apps;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class ApplicationStartupStateCalculator {

    public ApplicationStartupState computeDesiredState(CloudApplication app, boolean shouldNotStartAnyApp) {
        if (hasExecuteAppParameter(app)) {
            return ApplicationStartupState.EXECUTED;
        }

        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        boolean shouldNotStartApp = attributesGetter.getAttribute(SupportedParameters.NO_START, Boolean.class, shouldNotStartAnyApp);
        return (shouldNotStartApp) ? ApplicationStartupState.STOPPED : ApplicationStartupState.STARTED;
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
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        return attributesGetter.getAttribute(SupportedParameters.EXECUTE_APP, Boolean.class, false);
    }

    private org.cloudfoundry.client.lib.domain.CloudApplication.AppState getRequestedState(CloudApplication app) {
        return app.getState();
    }

    private boolean isStarted(CloudApplication app) {
        return app.getRunningInstances() == app.getInstances() && app.getInstances() != 0
            && getRequestedState(app).equals(org.cloudfoundry.client.lib.domain.CloudApplication.AppState.STARTED);
    }

    private boolean isStopped(CloudApplication app) {
        return app.getRunningInstances() == 0
            && getRequestedState(app).equals(org.cloudfoundry.client.lib.domain.CloudApplication.AppState.STOPPED);
    }

}
