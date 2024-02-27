package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class SetAppsUrisStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(getStartProgressMessage());

        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(execution.getContext());
        for (CloudApplicationExtended app : apps) {
            assignUris(app);
        }
        StepsUtil.setAppsToDeploy(execution.getContext(), apps);
        setAdditionalContextVariables(execution.getContext());

        getStepLogger().debug(getEndProgressMessage());
        return StepPhase.DONE;
    }

    private void reportAssignedUris(CloudApplication app) {
        for (String uri : app.getUris()) {
            getStepLogger().info(Messages.ASSIGNING_URI, uri, app.getName());
        }
    }

    private void assignUris(CloudApplicationExtended app) {
        List<String> newUris = getNewUris(app);
        if (newUris != null && !newUris.isEmpty()) {
            app.setUris(newUris);
            reportAssignedUris(app);
        }
    }

    protected abstract List<String> getNewUris(CloudApplicationExtended app);

    protected abstract void setAdditionalContextVariables(DelegateExecution context);

    protected abstract String getStartProgressMessage();

    protected abstract String getEndProgressMessage();

}
