package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudRouteExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudRouteExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceRouteBinding;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceInstanceRoutesGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

@Named("deleteApplicationRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteApplicationRoutesStep extends UndeployAppStep implements BeforeStepHookPhaseProvider {

    @Inject
    private TokenService tokenService;
    @Inject
    private WebClientFactory webClientFactory;

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication app, ProcessContext context) {
        getStepLogger().info(Messages.DELETING_APP_ROUTES, app.getName());

        List<CloudRouteExtended> appRoutes = getApplicationRoutes(client, app, context);

        getStepLogger().debug(Messages.ROUTES_FOR_APPLICATION, app.getName(), JsonUtil.toJson(appRoutes, true));

        client.updateApplicationRoutes(app.getName(), Collections.emptySet());
        for (CloudRouteExtended route : appRoutes) {
            deleteApplicationRoute(client, route);
        }
        getStepLogger().debug(Messages.DELETED_APP_ROUTES, app.getName());
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DELETING_APP_ROUTES, context.getVariable(Variables.APP_TO_PROCESS)
                                                                               .getName());
    }

    protected ServiceInstanceRoutesGetter getServiceRoutesGetter(CloudCredentials credentials, String correlationId) {
        return new ServiceInstanceRoutesGetter(configuration, webClientFactory, credentials, correlationId);
    }

    private List<CloudRouteExtended> getApplicationRoutes(CloudControllerClient client, CloudApplication app, ProcessContext context) {
        String user = context.getVariable(Variables.USER);
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        var token = tokenService.getToken(user);
        var credentials = new CloudCredentials(token, true);
        var serviceInstanceRoutesGetter = getServiceRoutesGetter(credentials, correlationId);

        var routes = client.getApplicationRoutes(app.getGuid());
        var routeGuids = routes.stream()
                               .map(CloudRoute::getGuid)
                               .map(UUID::toString)
                               .collect(Collectors.toList());
        var serviceRouteBindings = serviceInstanceRoutesGetter.getServiceRouteBindings(routeGuids);
        var routeIdsToServiceInstanceIds = serviceRouteBindings.stream()
                                                               .collect(Collectors.groupingBy(ServiceRouteBinding::getRouteId,
                                                                                              Collectors.mapping(ServiceRouteBinding::getServiceInstanceId,
                                                                                                                 Collectors.toList())));
        return routes.stream()
                     .map(route -> addServicesToRoute(route, routeIdsToServiceInstanceIds))
                     .collect(Collectors.toList());
    }

    private CloudRouteExtended addServicesToRoute(CloudRoute route, Map<String, List<String>> routesToServices) {
        var boundServiceGuids = routesToServices.getOrDefault(route.getGuid()
                                                                   .toString(),
                                                              Collections.emptyList());
        return ImmutableCloudRouteExtended.builder()
                                          .from(route)
                                          .boundServiceInstanceGuids(boundServiceGuids)
                                          .build();
    }

    private void deleteApplicationRoute(CloudControllerClient client, CloudRouteExtended route) {
        if (route.getAppsUsingRoute() > 1 || !route.getBoundServiceInstanceGuids()
                                                   .isEmpty()) {
            getStepLogger().warn(Messages.ROUTE_NOT_DELETED, route.getUrl());
            return;
        }
        getStepLogger().info(Messages.DELETING_ROUTE, route.getUrl());
        client.deleteRoute(route.getHost(), route.getDomain()
                                                 .getName(),
                           route.getPath());
        getStepLogger().debug(Messages.ROUTE_DELETED, route.getUrl());
    }

    @Override
    public List<HookPhase> getHookPhasesBeforeStep(ProcessContext context) {
        return hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.BEFORE_UNMAP_ROUTES, HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES),
                                                 context);
    }
}