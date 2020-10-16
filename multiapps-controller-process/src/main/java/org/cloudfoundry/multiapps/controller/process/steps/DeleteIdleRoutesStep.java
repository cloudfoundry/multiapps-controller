package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.controller.core.helpers.ClientHelper;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

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
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);

        deleteIdleRoutes(existingApp, client, app);

        getStepLogger().debug(Messages.IDLE_URIS_DELETED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_IDLE_ROUTES;
    }

    private void deleteIdleRoutes(CloudApplication idleApp, CloudControllerClient client, CloudApplication newLiveApp) {
        List<String> idleUris = ListUtils.subtract(idleApp.getUris(), newLiveApp.getUris());
        getStepLogger().debug(Messages.IDLE_URIS_FOR_APPLICATION, idleUris);

        for (String idleUri : idleUris) {
            deleteRoute(idleUri, client);
            getStepLogger().debug(Messages.ROUTE_DELETED, idleUri);
        }
    }

    private void deleteRoute(String uri, CloudControllerClient client) {
        try {
            new ClientHelper(client).deleteRoute(uri);
        } catch (CloudOperationException e) {
            handleCloudOperationException(e, uri);
        }
    }

    private void handleCloudOperationException(CloudOperationException e, String uri) {
        if (e.getStatusCode() == HttpStatus.CONFLICT) {
            getStepLogger().info(Messages.ROUTE_NOT_DELETED, uri);
            return;
        }
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().info(org.cloudfoundry.multiapps.controller.core.Messages.ROUTE_NOT_FOUND, uri);
            return;
        }
        throw e;
    }

}
