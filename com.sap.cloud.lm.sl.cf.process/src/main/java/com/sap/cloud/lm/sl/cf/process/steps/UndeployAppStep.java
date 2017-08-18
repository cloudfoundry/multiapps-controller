package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationRoutesGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.OneOffTasksSupportChecker;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("undeployAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UndeployAppStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("undeployAppTask").displayName("Undeploy Discontinued App").description(
            "Undeploy Discontinued App").build();
    }

    @Inject
    private OneOffTasksSupportChecker oneOffTasksSupportChecker;
    @Inject
    private ApplicationRoutesGetter applicationRoutesGetter;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();
        try {
            CloudApplication appToUndeploy = StepsUtil.getAppToUndeploy(context);

            CloudFoundryOperations client = getCloudFoundryClient(context);

            cancelRunningTasksIfTasksAreSupported(appToUndeploy, client);
            stopApplication(appToUndeploy, client);
            deleteApplicationRoutes(appToUndeploy, client);
            deleteApplication(appToUndeploy, client);

            getStepLogger().debug(Messages.APPS_UNDEPLOYED);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_UNDEPLOYING_APPS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UNDEPLOYING_APPS);
            throw e;
        }
    }

    private void cancelRunningTasksIfTasksAreSupported(CloudApplication appToUndeploy, CloudFoundryOperations client) {
        if (!oneOffTasksSupportChecker.areOneOffTasksSupported(client)) {
            return;
        }
        cancelRunningTasks(appToUndeploy, client);
    }

    private void cancelRunningTasks(CloudApplication appToUndeploy, CloudFoundryOperations client) {
        ClientExtensions clientExtensions = (ClientExtensions) client;
        List<CloudTask> tasksToCancel = clientExtensions.getTasks(appToUndeploy.getName());
        for (CloudTask task : tasksToCancel) {
            CloudTask.State taskState = task.getState();
            if (taskState.equals(CloudTask.State.RUNNING) || taskState.equals(CloudTask.State.PENDING)) {
                cancelTask(task, appToUndeploy, clientExtensions);
            }
        }
    }

    private void cancelTask(CloudTask task, CloudApplication appToUndeploy, ClientExtensions clientExtensions) {
        getStepLogger().info(Messages.CANCELING_TASK_ON_APP, task.getName(), appToUndeploy.getName());
        clientExtensions.cancelTask(task.getMeta().getGuid());
        getStepLogger().debug(Messages.CANCELED_TASK_ON_APP, task.getName(), appToUndeploy.getName());
    }

    private void stopApplication(CloudApplication app, CloudFoundryOperations client) {
        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        getStepLogger().debug(Messages.APP_STOPPED, app.getName());
    }

    private void deleteApplication(CloudApplication app, CloudFoundryOperations client) {
        getStepLogger().info(Messages.DELETING_APP, app.getName());
        client.deleteApplication(app.getName());
        getStepLogger().debug(Messages.APP_DELETED, app.getName());
    }

    private void deleteApplicationRoutes(CloudApplication app, CloudFoundryOperations client) {
        getStepLogger().info(Messages.DELETING_APP_ROUTES, app.getName());
        List<CloudRoute> appRoutes = applicationRoutesGetter.getRoutes(client, app.getName());
        client.updateApplicationUris(app.getName(), Collections.emptyList());
        app.getUris().stream().forEach(uri -> deleteApplicationRoute(app, appRoutes, uri, client));
    }

    private void deleteApplicationRoute(CloudApplication app, List<CloudRoute> routes, String uri, CloudFoundryOperations client) {
        getStepLogger().info(Messages.DELETING_ROUTE, uri);
        CloudRoute route = UriUtil.findRoute(routes, uri);
        if (route.getAppsUsingRoute() > 1) {
            return;
        }
        boolean portBasedRouting = isPortBasedRouting(client);
        new ClientHelper(client).deleteRoute(uri, portBasedRouting);
        getStepLogger().debug(Messages.ROUTE_DELETED, uri);
    }

    private boolean isPortBasedRouting(CloudFoundryOperations client) {
        CloudInfo info = client.getCloudInfo();
        return (info instanceof CloudInfoExtended) ? ((CloudInfoExtended) info).isPortBasedRouting() : false;
    }

}
