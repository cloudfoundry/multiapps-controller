package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class UndeployAppStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndeployAppStep.class);

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
        logActivitiTask(context, LOGGER);
        try {
            CloudApplication appToUndeploy = StepsUtil.getAppToUndeploy(context);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

            cancelRunningTasksIfTasksAreSupported(appToUndeploy, client, context);
            stopApplication(appToUndeploy, client, context);
            deleteApplicationRoutes(appToUndeploy, client, context);
            deleteApplication(appToUndeploy, client, context);

            debug(context, Messages.APPS_UNDEPLOYED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_UNDEPLOYING_APPS, e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, Messages.ERROR_UNDEPLOYING_APPS, e, LOGGER);
            throw e;
        }
    }

    private void cancelRunningTasksIfTasksAreSupported(CloudApplication appToUndeploy, CloudFoundryOperations client,
        DelegateExecution context) {
        if (!oneOffTasksSupportChecker.areOneOffTasksSupported(client)) {
            return;
        }
        cancelRunningTasks(appToUndeploy, client, context);
    }

    private void cancelRunningTasks(CloudApplication appToUndeploy, CloudFoundryOperations client, DelegateExecution context) {
        ClientExtensions clientExtensions = (ClientExtensions) client;
        List<CloudTask> tasksToCancel = clientExtensions.getTasks(appToUndeploy.getName());
        for (CloudTask task : tasksToCancel) {
            CloudTask.State taskState = task.getState();
            if (taskState.equals(CloudTask.State.RUNNING) || taskState.equals(CloudTask.State.PENDING)) {
                cancelTask(task, appToUndeploy, clientExtensions, context);
            }
        }
    }

    private void cancelTask(CloudTask task, CloudApplication appToUndeploy, ClientExtensions clientExtensions, DelegateExecution context) {
        info(context, format(Messages.CANCELING_TASK_ON_APP, task.getName(), appToUndeploy.getName()), LOGGER);
        clientExtensions.cancelTask(task.getMeta().getGuid());
        debug(context, format(Messages.CANCELED_TASK_ON_APP, task.getName(), appToUndeploy.getName()), LOGGER);
    }

    private void stopApplication(CloudApplication app, CloudFoundryOperations client, DelegateExecution context) {
        info(context, format(Messages.STOPPING_APP, app.getName()), LOGGER);
        client.stopApplication(app.getName());
        debug(context, format(Messages.APP_STOPPED, app.getName()), LOGGER);
    }

    private void deleteApplication(CloudApplication app, CloudFoundryOperations client, DelegateExecution context) {
        info(context, format(Messages.DELETING_APP, app.getName()), LOGGER);
        client.deleteApplication(app.getName());
        debug(context, format(Messages.APP_DELETED, app.getName()), LOGGER);
    }

    private void deleteApplicationRoutes(CloudApplication app, CloudFoundryOperations client, DelegateExecution context) {
        info(context, format(Messages.DELETING_APP_ROUTES, app.getName()), LOGGER);
        List<CloudRoute> appRoutes = applicationRoutesGetter.getRoutes(client, app.getName());
        client.updateApplicationUris(app.getName(), Collections.emptyList());
        app.getUris().stream().forEach(uri -> deleteApplicationRoute(app, appRoutes, uri, client, context));
    }

    private void deleteApplicationRoute(CloudApplication app, List<CloudRoute> routes, String uri, CloudFoundryOperations client,
        DelegateExecution context) {
        info(context, format(Messages.DELETING_ROUTE, uri), LOGGER);
        CloudRoute route = UriUtil.findRoute(routes, uri);
        if (route.getAppsUsingRoute() > 1) {
            return;
        }
        boolean portBasedRouting = isPortBasedRouting(client);
        new ClientHelper(client).deleteRoute(uri, portBasedRouting);
        debug(context, format(Messages.ROUTE_DELETED, uri), LOGGER);
    }

    private boolean isPortBasedRouting(CloudFoundryOperations client) {
        CloudInfo info = client.getCloudInfo();
        return (info instanceof CloudInfoExtended) ? ((CloudInfoExtended) info).isPortBasedRouting() : false;
    }

}
