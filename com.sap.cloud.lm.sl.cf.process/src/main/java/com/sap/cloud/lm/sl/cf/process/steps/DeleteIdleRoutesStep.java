package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("deleteIdleRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteIdleRoutesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        boolean deleteIdleRoutes = StepsUtil.getDeleteIdleUris(execution.getContext());
        CloudApplication existingApp = StepsUtil.getExistingApp(execution.getContext());
        if (!deleteIdleRoutes || existingApp == null) {
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.DELETING_IDLE_URIS);
        CloudControllerClient client = execution.getControllerClient();
        CloudApplication app = StepsUtil.getApp(execution.getContext());

        deleteIdleRoutes(existingApp, client, app);

        getStepLogger().debug(Messages.IDLE_URIS_DELETED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DELETING_IDLE_ROUTES;
    }

    private void deleteIdleRoutes(CloudApplication existingApp, CloudControllerClient client, CloudApplication app) {
        List<String> idleUris = ListUtils.subtract(existingApp.getUris(), app.getUris());
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
            getStepLogger().info(com.sap.cloud.lm.sl.cf.core.message.Messages.ROUTE_NOT_FOUND, uri);
            return;
        }
        throw e;
    }

}
