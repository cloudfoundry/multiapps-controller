package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("swapUrisStep")
public abstract class SwapUrisStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwapUrisStep.class);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        info(context, getStartProgressMessage(), LOGGER);

        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
        for (CloudApplicationExtended app : apps) {
            switchUris(app, context);
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

    private void switchUris(CloudApplicationExtended app, DelegateExecution context) {
        List<String> tempUris = app.getTempUris();
        List<String> uris = app.getUris();
        if (tempUris != null && !tempUris.isEmpty()) {
            app.setTempUris(uris);
            app.setUris(tempUris);
            reportAssignedUris(app, context);
        }
    }

    protected abstract String getStartProgressMessage();

    protected abstract String getEndProgressMessage();

}
