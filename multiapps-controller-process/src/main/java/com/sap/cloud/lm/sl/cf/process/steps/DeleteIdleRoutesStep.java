package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("deleteIdleRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteIdleRoutesStep extends SyncFlowableStep {

    @Inject
    private SpaceGetter spaceGetter;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DELETING_IDLE_URIS);
            CloudControllerClient client = execution.getControllerClient();
            boolean portBasedRouting = (boolean) execution.getContext()
                                                          .getVariable(Constants.VAR_PORT_BASED_ROUTING);

            List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(execution.getContext());
            for (CloudApplicationExtended app : apps) {
                deleteIdleRoutes(app, portBasedRouting, client);
            }

            getStepLogger().debug(Messages.IDLE_URIS_DELETED);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_DELETING_IDLE_ROUTES);
            throw e;
        }
    }

    private void deleteIdleRoutes(CloudApplicationExtended app, boolean portBasedRouting, CloudControllerClient client) {
        List<String> idleUris = new ArrayList<>(app.getIdleUris());
        for (String idleUri : idleUris) {
            deleteRoute(idleUri, portBasedRouting, client);
            getStepLogger().debug(Messages.ROUTE_DELETED, idleUri);
        }
    }

    private void deleteRoute(String uri, boolean portBasedRouting, CloudControllerClient client) {
        try {
            boolean portRoute = portBasedRouting || UriUtil.isTcpOrTcpsUri(uri);
            new ClientHelper(client, spaceGetter).deleteRoute(uri, portRoute);
        } catch (CloudOperationException e) {
            if (!e.getStatusCode()
                  .equals(HttpStatus.CONFLICT)) {
                throw e;
            }
            getStepLogger().info(Messages.ROUTE_NOT_DELETED, uri);
        }
    }

}
