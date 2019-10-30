package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationRoutesGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("deleteApplicationRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteApplicationRoutesStep extends UndeployAppStep {

    private ApplicationRoutesGetter applicationRoutesGetter;

    @Inject
    public DeleteApplicationRoutesStep(ApplicationRoutesGetter applicationRoutesGetter) {
        this.applicationRoutesGetter = applicationRoutesGetter;
    }

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy) {
        deleteApplicationRoutes(client, cloudApplicationToUndeploy);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return MessageFormat.format(Messages.ERROR_DELETING_APP_ROUTES, StepsUtil.getApp(context)
                                                                                 .getName());
    }

    private void deleteApplicationRoutes(CloudControllerClient client, CloudApplication cloudApplication) {
        getStepLogger().info(Messages.DELETING_APP_ROUTES, cloudApplication.getName());
        List<CloudRoute> cloudApplicationRoutes = applicationRoutesGetter.getRoutes(client, cloudApplication.getName());
        getStepLogger().debug(Messages.ROUTES_FOR_APPLICATION, cloudApplication.getName(), JsonUtil.toJson(cloudApplicationRoutes));
        client.updateApplicationUris(cloudApplication.getName(), Collections.emptyList());
        for (String uri : cloudApplication.getUris()) {
            deleteApplicationRoutes(client, cloudApplicationRoutes, uri);
        }
        getStepLogger().debug(Messages.DELETED_APP_ROUTES, cloudApplication.getName());
    }

    private void deleteApplicationRoutes(CloudControllerClient client, List<CloudRoute> routes, String uri) {
        try {
            CloudRoute route = UriUtil.findRoute(routes, uri);
            if (route.getAppsUsingRoute() > 1) {
                return;
            }
        } catch (NotFoundException e) {
            getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.ROUTE_NOT_FOUND, uri);
            return;
        }
        getStepLogger().info(Messages.DELETING_ROUTE, uri);
        new ClientHelper(client).deleteRoute(uri);
        getStepLogger().debug(Messages.ROUTE_DELETED, uri);
    }

    @Override
    protected HookPhase getHookPhaseBeforeStep(DelegateExecution context) {
        return HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES;
    }

}