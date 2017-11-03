package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
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
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReplaceRoutesStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("replaceRoutesTask").displayName("Replace Routes").description("Replace Routes").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().info(Messages.DELETING_IDLE_URIS);
            CloudFoundryOperations client = getCloudFoundryClient(context);
            boolean portBasedRouting = (boolean) context.getVariable(Constants.VAR_PORT_BASED_ROUTING);

            List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
            for (CloudApplicationExtended app : apps) {
                deleteIdleRoutes(app, portBasedRouting, client);
            }

            getStepLogger().debug(Messages.IDLE_URIS_DELETED);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_DELETING_IDLE_ROUTES);
            throw e;
        }
    }

    private void deleteIdleRoutes(CloudApplicationExtended app, boolean portBasedRouting, CloudFoundryOperations client) {
        List<String> uris = new ArrayList<>(app.getUris());
        List<String> existingUris = client.getApplication(app.getName()).getUris();
        if (uris.containsAll(existingUris)) {
            return;
        }

        client.updateApplicationUris(app.getName(), uris);
        // Only the discontinued uris would remain in the collection
        existingUris.removeAll(uris);
        for (String idleUri : existingUris) {
            deleteRoute(idleUri, portBasedRouting, client);
            getStepLogger().debug(Messages.ROUTE_DELETED, idleUri);
        }
    }

    private void deleteRoute(String uri, boolean portBasedRouting, CloudFoundryOperations client) {
        try {
            new ClientHelper(client).deleteRoute(uri, portBasedRouting);
        } catch (CloudFoundryException ex) {
            if (!ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
                throw ex;
            }
            getStepLogger().info(Messages.ROUTE_NOT_DELETED, uri);
        }
    }

}
