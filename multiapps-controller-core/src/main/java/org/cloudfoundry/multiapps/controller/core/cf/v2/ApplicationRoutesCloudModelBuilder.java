package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.RoutingParameterSet;
import org.cloudfoundry.multiapps.controller.core.parser.IdleRouteParametersParser;
import org.cloudfoundry.multiapps.controller.core.parser.RouteParametersParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

public class ApplicationRoutesCloudModelBuilder {

    private final DeploymentDescriptor descriptor;
    private final CloudControllerClient client;
    private final CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy;

    public ApplicationRoutesCloudModelBuilder(DeploymentDescriptor descriptor, CloudControllerClient client,
                                              CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy) {
        this.descriptor = descriptor;
        this.client = client;
        this.applicationAttributeUpdateStrategy = applicationAttributeUpdateStrategy;
    }

    public Set<CloudRoute> getApplicationRoutes(Module module, List<Map<String, Object>> propertiesList,
                                                DeployedMtaApplication deployedApplication) {
        Set<CloudRoute> routes = getRouteParametersParser(module).parse(propertiesList);
        if (shouldKeepExistingRoutes(propertiesList)) {
            return addExistingRoutes(routes, deployedApplication);
        }
        return routes;
    }

    private boolean shouldKeepExistingRoutes(List<Map<String, Object>> propertiesList) {
        return (boolean) getPropertyValue(propertiesList, SupportedParameters.KEEP_EXISTING_ROUTES, false)
            || applicationAttributeUpdateStrategy.shouldKeepExistingRoutes();
    }

    private Object getPropertyValue(List<Map<String, Object>> propertiesList, String propertyName, Object defaultValue) {
        return PropertiesUtil.getPropertyValue(propertiesList, propertyName, defaultValue);
    }

    private Set<CloudRoute> addExistingRoutes(Set<CloudRoute> routes, DeployedMtaApplication deployedMtaApplication) {
        if (deployedMtaApplication == null) {
            return routes;
        }
        List<CloudRoute> existingRoutes = client.getApplicationRoutes(deployedMtaApplication.getGuid());
        Set<CloudRoute> mergedRoutes = new HashSet<>();
        for (CloudRoute route : routes) {
            if (route.getRequestedProtocol() == null) {
                mergedRoutes.add(getExistingRouteOrReturnNew(route, existingRoutes));
            } else {
                mergedRoutes.add(route);
            }
        }
        mergedRoutes.addAll(existingRoutes);
        return mergedRoutes;
    }

    private CloudRoute getExistingRouteOrReturnNew(CloudRoute newRoute, Collection<CloudRoute> existingRoutes) {
        return existingRoutes.stream()
                             .filter(route -> newRoute.getUrl()
                                                      .equals(route.getUrl()))
                             .findFirst()
                             .orElse(newRoute);
    }

    public List<String> getApplicationDomains(Module module, List<Map<String, Object>> propertiesList) {
        return getRouteParametersParser(module).getApplicationDomains(propertiesList);
    }

    public Set<CloudRoute> getIdleApplicationRoutes(Module module, List<Map<String, Object>> propertiesList) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT_IDLE;
        Map<String, Object> moduleParameters = module.getParameters();
        String defaultHost = (String) moduleParameters.getOrDefault(parametersType.host, null);
        String defaultRoutePath = (String) module.getParameters()
                                                 .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleParameters);
        return new IdleRouteParametersParser(defaultHost, defaultDomain, defaultRoutePath).parse(propertiesList);
    }

    private RouteParametersParser getRouteParametersParser(Module module) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT;
        Map<String, Object> moduleParameters = module.getParameters();
        String defaultHost = (String) moduleParameters.getOrDefault(parametersType.host, null);
        String routePath = (String) module.getParameters()
                                          .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleParameters);
        return new RouteParametersParser(defaultHost, defaultDomain, routePath);
    }

    private String getDefaultDomain(RoutingParameterSet parametersType, Map<String, Object> moduleParameters) {
        if (descriptor.getParameters()
                      .containsKey(parametersType.domain)) {
            return (String) descriptor.getParameters()
                                      .get(parametersType.domain);
        }
        return (String) moduleParameters.get(parametersType.domain);
    }

}
