package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationRoutesGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("undeployAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UndeployAppStep extends SyncFlowableStep {

    @Inject
    private ApplicationRoutesGetter applicationRoutesGetter;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            CloudApplication appToUndeploy = StepsUtil.getAppToUndeploy(execution.getContext());
            CloudControllerClient client = execution.getControllerClient();

            if (client.areTasksSupported()) {
                cancelRunningTasks(appToUndeploy, client);
            }
            stopApplication(appToUndeploy, client);
            deleteApplicationRoutes(appToUndeploy, client);
            deleteApplication(appToUndeploy, client);

            getStepLogger().debug(Messages.APPS_UNDEPLOYED);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_UNDEPLOYING_APPS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UNDEPLOYING_APPS);
            throw e;
        }
    }

    private void cancelRunningTasks(CloudApplication appToUndeploy, CloudControllerClient client) {
        List<CloudTask> tasksToCancel = client.getTasks(appToUndeploy.getName());
        for (CloudTask task : tasksToCancel) {
            CloudTask.State taskState = task.getState();
            if (taskState.equals(CloudTask.State.RUNNING) || taskState.equals(CloudTask.State.PENDING)) {
                cancelTask(task, appToUndeploy, client);
            }
        }
    }

    private void cancelTask(CloudTask task, CloudApplication appToUndeploy, CloudControllerClient client) {
        getStepLogger().info(Messages.CANCELING_TASK_ON_APP, task.getName(), appToUndeploy.getName());
        client.cancelTask(task.getMeta()
            .getGuid());
        getStepLogger().debug(Messages.CANCELED_TASK_ON_APP, task.getName(), appToUndeploy.getName());
    }

    private void stopApplication(CloudApplication app, CloudControllerClient client) {
        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        getStepLogger().debug(Messages.APP_STOPPED, app.getName());
    }

    private void deleteApplication(CloudApplication app, CloudControllerClient client) {
        getStepLogger().info(Messages.DELETING_APP, app.getName());
        client.deleteApplication(app.getName());
        getStepLogger().debug(Messages.APP_DELETED, app.getName());
    }

    private void deleteApplicationRoutes(CloudApplication app, CloudControllerClient client) {
        getStepLogger().info(Messages.DELETING_APP_ROUTES, app.getName());
        List<CloudRoute> appRoutes = applicationRoutesGetter.getRoutes(client, app.getName());
        getStepLogger().debug(Messages.ROUTES_FOR_APPLICATION, app.getName(), JsonUtil.toJson(appRoutes));
        client.updateApplicationUris(app.getName(), Collections.emptyList());
        app.getUris()
            .stream()
            .forEach(uri -> deleteApplicationRoutes(appRoutes, uri, client));
        getStepLogger().debug(Messages.DELETED_APP_ROUTES, app.getName());
    }

    private void deleteApplicationRoutes(List<CloudRoute> routes, String uri, CloudControllerClient client) {
        getStepLogger().info(Messages.DELETING_ROUTE, uri);
        boolean isPortBasedRouting = isPortBasedRouting(client, uri);
        try {
            CloudRoute route = UriUtil.findRoute(routes, uri, isPortBasedRouting);
            if (route.getAppsUsingRoute() > 1) {
                return;
            }
        } catch (NotFoundException e) {
            getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.ROUTE_NOT_FOUND, uri);
            return;
        }
        new ClientHelper(client).deleteRoute(uri, isPortBasedRouting);
        getStepLogger().debug(Messages.ROUTE_DELETED, uri);
    }

    private boolean isPortBasedRouting(CloudControllerClient client, String uri) {
        CloudInfo info = client.getCloudInfo();
        boolean isPortBasedSystem = info instanceof CloudInfoExtended && ((CloudInfoExtended) info).isPortBasedRouting();
        return isPortBasedSystem || UriUtil.isTcpOrTcpsUri(uri);
    }

}
