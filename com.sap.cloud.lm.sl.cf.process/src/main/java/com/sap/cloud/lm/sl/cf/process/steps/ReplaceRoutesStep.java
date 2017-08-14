package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("replaceRoutesStep")
public class ReplaceRoutesStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplaceRoutesStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("replaceRoutesTask").displayName("Replace Routes").description("Replace Routes").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.DELETING_IDLE_URIS, LOGGER);
            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            boolean portBasedRouting = (boolean) context.getVariable(Constants.VAR_PORT_BASED_ROUTING);

            List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
            for (CloudApplicationExtended app : apps) {
                deleteIdleRoutes(app, portBasedRouting, client, context);
            }

            debug(context, Messages.IDLE_URIS_DELETED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_DELETING_IDLE_ROUTES, e, LOGGER);
            throw e;
        }
    }

    private void deleteIdleRoutes(CloudApplicationExtended app, boolean portBasedRouting, CloudFoundryOperations client,
        DelegateExecution context) {
        List<String> uris = new ArrayList<>(app.getUris());
        List<String> existingUris = client.getApplication(app.getName()).getUris();
        if (uris.containsAll(existingUris)) {
            return;
        }
        
        client.updateApplicationUris(app.getName(), uris);
        // Only the discontinued uris would remain in the collection
        existingUris.removeAll(uris);
        for (String idleUri : existingUris) {
            deleteRoute(idleUri, portBasedRouting, client, context);
        }
    }

    private void deleteRoute(String uri, boolean portBasedRouting, CloudFoundryOperations client, DelegateExecution context) {
        try {
            new ClientHelper(client).deleteRoute(uri, portBasedRouting);
        } catch (CloudFoundryException ex) {
            if (!ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw ex;
            }
            info(context, format(Messages.ROUTE_NOT_DELETED, uri), LOGGER);
        }
    }
}
