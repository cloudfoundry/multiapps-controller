package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ClientHelper;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

@Named("deleteIdleRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteIdleRoutesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        boolean deleteIdleRoutes = context.getVariable(Variables.DELETE_IDLE_URIS);
        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);
        if (!deleteIdleRoutes || existingApp == null) {
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.DELETING_IDLE_URIS);
        CloudControllerClient client = context.getControllerClient();
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

        var clientHelper = new ClientHelper(client);
        deleteIdleRoutes(context, clientHelper, app);

        getStepLogger().debug(Messages.IDLE_URIS_DELETED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_IDLE_ROUTES;
    }

    private void deleteIdleRoutes(ProcessContext context, ClientHelper clientHelper, CloudApplicationExtended newLiveApp) {
        var currentRoutes = context.getVariable(Variables.CURRENT_ROUTES);
        Set<CloudRoute> idleRoutes = SetUtils.difference(new HashSet<>(currentRoutes), newLiveApp.getRoutes())
                                             .toSet();
        getStepLogger().debug(Messages.IDLE_URIS_FOR_APPLICATION, idleRoutes);

        for (CloudRoute idleRoute : idleRoutes) {
            deleteRoute(idleRoute, clientHelper);
            getStepLogger().debug(Messages.ROUTE_DELETED, idleRoute.getUrl());
        }
    }

    private void deleteRoute(CloudRoute route, ClientHelper clientHelper) {
        try {
            clientHelper.deleteRoute(route);
        } catch (CloudOperationException e) {
            handleCloudOperationException(e, route);
        }
    }

    private void handleCloudOperationException(CloudOperationException e, CloudRoute route) {
        if (e.getStatusCode() == HttpStatus.CONFLICT) {
            getStepLogger().info(Messages.ROUTE_NOT_DELETED, route);
            return;
        }
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().info(org.cloudfoundry.multiapps.controller.core.Messages.ROUTE_NOT_FOUND, route);
            return;
        }
        throw e;
    }

}
