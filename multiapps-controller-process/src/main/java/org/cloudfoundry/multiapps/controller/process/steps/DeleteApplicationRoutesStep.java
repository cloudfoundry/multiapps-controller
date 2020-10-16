package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ApplicationRoutesGetter;
import org.cloudfoundry.multiapps.controller.core.helpers.ClientHelper;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

@Named("deleteApplicationRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteApplicationRoutesStep extends UndeployAppStep implements BeforeStepHookPhaseProvider {

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy,
                                            ProcessContext context) {
        deleteApplicationRoutes(client, cloudApplicationToUndeploy);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DELETING_APP_ROUTES, context.getVariable(Variables.APP_TO_PROCESS)
                                                                               .getName());
    }

    private void deleteApplicationRoutes(CloudControllerClient client, CloudApplication cloudApplication) {
        getStepLogger().info(Messages.DELETING_APP_ROUTES, cloudApplication.getName());
        ApplicationRoutesGetter applicationRoutesGetter = getApplicationRoutesGetter(client);
        List<CloudRoute> cloudApplicationRoutes = applicationRoutesGetter.getRoutes(cloudApplication.getName());

        getStepLogger().debug(Messages.ROUTES_FOR_APPLICATION, cloudApplication.getName(), JsonUtil.toJson(cloudApplicationRoutes, true));

        client.updateApplicationUris(cloudApplication.getName(), Collections.emptyList());
        for (String uri : cloudApplication.getUris()) {
            deleteApplicationRoutes(client, cloudApplicationRoutes, uri);
        }
        getStepLogger().debug(Messages.DELETED_APP_ROUTES, cloudApplication.getName());
    }
    
    protected ApplicationRoutesGetter getApplicationRoutesGetter(CloudControllerClient client) {
        return new ApplicationRoutesGetter(client);
    }

    private void deleteApplicationRoutes(CloudControllerClient client, List<CloudRoute> routes, String uri) {
        try {
            CloudRoute route = UriUtil.findRoute(routes, uri);
            if (route.getAppsUsingRoute() > 1 || route.hasServiceUsingRoute()) {
                getStepLogger().warn(Messages.ROUTE_NOT_DELETED, uri);
                return;
            }
        } catch (NotFoundException e) {
            getStepLogger().debug(org.cloudfoundry.multiapps.controller.core.Messages.ROUTE_NOT_FOUND, uri);
            return;
        }
        getStepLogger().info(Messages.DELETING_ROUTE, uri);
        new ClientHelper(client).deleteRoute(uri);
        getStepLogger().debug(Messages.ROUTE_DELETED, uri);
    }

    @Override
    public List<HookPhase> getHookPhasesBeforeStep(ProcessContext context) {
        return hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.BEFORE_UNMAP_ROUTES, HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES),
                                                 context);
    }
}