package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public abstract class SetAppsUrisStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetAppsUrisStep.class);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        info(context, getStartProgressMessage(), LOGGER);

        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
        for (CloudApplicationExtended app : apps) {
            assignUris(app, context);
        }
        StepsUtil.setAppsToDeploy(context, apps);

        debug(context, getEndProgressMessage(), LOGGER);
        return ExecutionStatus.SUCCESS;
    }

    private void reportAssignedUris(CloudApplication app, DelegateExecution context) {
        for (String uri : app.getUris()) {
            info(context, format(Messages.ASSIGNING_URI, uri, app.getName()), LOGGER);
        }
    }

    private void assignUris(CloudApplicationExtended app, DelegateExecution context) {
        List<String> newUris = getNewUris(app);
        if (newUris != null && !newUris.isEmpty()) {
            app.setUris(newUris);
            reportAssignedUris(app, context);
        }
    }

    protected abstract List<String> getNewUris(CloudApplicationExtended app);

    protected abstract String getStartProgressMessage();

    protected abstract String getEndProgressMessage();

}
