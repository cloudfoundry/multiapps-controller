package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class SetAppsUrisStep extends AbstractXS2ProcessStep {

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        getStepLogger().info(getStartProgressMessage());

        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
        for (CloudApplicationExtended app : apps) {
            assignUris(app);
        }
        StepsUtil.setAppsToDeploy(context, apps);

        getStepLogger().debug(getEndProgressMessage());
        return ExecutionStatus.SUCCESS;
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

    protected abstract String getStartProgressMessage();

    protected abstract String getEndProgressMessage();

}
