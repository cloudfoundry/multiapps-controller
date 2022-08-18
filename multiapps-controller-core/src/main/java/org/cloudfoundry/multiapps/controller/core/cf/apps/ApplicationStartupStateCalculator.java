package org.cloudfoundry.multiapps.controller.core.cf.apps;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;

import java.util.Map;

@Named
public class ApplicationStartupStateCalculator {

    public ApplicationStartupState computeDesiredState(CloudApplication app, Map<String, String> appEnv, boolean shouldNotStartAnyApp) {
        if (hasExecuteAppParameter(app, appEnv)) {
            return ApplicationStartupState.EXECUTED;
        }

        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, appEnv);
        boolean shouldNotStartApp = appAttributes.get(SupportedParameters.NO_START, Boolean.class, shouldNotStartAnyApp);
        return shouldNotStartApp ? ApplicationStartupState.STOPPED : ApplicationStartupState.STARTED;
    }

    public ApplicationStartupState computeCurrentState(CloudApplication app, InstancesInfo appInstances, Map<String, String> appEnv) {
        if (hasExecuteAppParameter(app, appEnv)) {
            return ApplicationStartupState.EXECUTED;
        }
        var runningInstancesCount = getRunningAppInstances(appInstances);
        if (isStarted(app, appInstances, runningInstancesCount)) {
            return ApplicationStartupState.STARTED;
        }
        if (isStopped(app, runningInstancesCount)) {
            return ApplicationStartupState.STOPPED;
        }
        return ApplicationStartupState.INCONSISTENT;
    }

    private boolean hasExecuteAppParameter(CloudApplication app, Map<String, String> appEnv) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, appEnv);
        return appAttributes.get(SupportedParameters.EXECUTE_APP, Boolean.class, false);
    }

    private boolean isStarted(CloudApplication app, InstancesInfo appInstances, long runningInstancesCount) {
        return runningInstancesCount == appInstances.getInstances()
                                                    .size() && runningInstancesCount > 0 && app.getState()
                                                                                               .equals(CloudApplication.State.STARTED);
    }

    private boolean isStopped(CloudApplication app, long runningInstancesCount) {
        return runningInstancesCount == 0 && app.getState()
                                                .equals(CloudApplication.State.STOPPED);
    }

    private long getRunningAppInstances(InstancesInfo appInstances) {
        return appInstances.getInstances()
                           .stream()
                           .map(InstanceInfo::getState)
                           .filter(InstanceState.RUNNING::equals)
                           .count();
    }

}
