package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

public class IdleRouteParametersParser extends RouteParametersParser {

    public IdleRouteParametersParser(String defaultHost, String defaultDomain, String routePath) {
        super(defaultHost, defaultDomain, SupportedParameters.IDLE_HOST, SupportedParameters.IDLE_DOMAIN, routePath);
    }

    public IdleRouteParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
                                     String routePath) {
        super(defaultHost, defaultDomain, hostParameterName, domainParameterName, routePath);
    }

    @Override
    public Set<CloudRoute> getApplicationRoutes(List<Map<String, Object>> parametersList) {
        Set<CloudRoute> idleRoutes = getIdleRoutes(parametersList);
        if (!idleRoutes.isEmpty()) {
            return idleRoutes;
        }

        Set<CloudRoute> liveRoutes = super.getApplicationRoutes(parametersList);
        if (!liveRoutes.isEmpty()) {
            return modifyLiveRoutes(liveRoutes);
        }
        return Collections.emptySet();
    }

    private Set<CloudRoute> getIdleRoutes(List<Map<String, Object>> parametersList) {
        List<Map<String, Object>> idleRoutesMaps = RoutesValidator.applyRoutesType(PropertiesUtil.getPropertyValue(parametersList,
                                                                                                                   SupportedParameters.IDLE_ROUTES,
                                                                                                                   null));
        return idleRoutesMaps.stream()
                             .map(this::parseIdleRouteMap)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toSet());

    }

    public CloudRoute parseIdleRouteMap(Map<String, Object> routeMap) {
        String routeString = (String) routeMap.get(SupportedParameters.IDLE_ROUTE);
        if (routeString == null) {
            return null;
        }
        boolean noHostname = MapUtil.parseBooleanFlag(routeMap, SupportedParameters.NO_HOSTNAME, false);
        String protocol = (String) routeMap.get(SupportedParameters.ROUTE_PROTOCOL);
        return new ApplicationURI(routeString, noHostname, protocol).toCloudRoute();
    }

    private Set<CloudRoute> modifyLiveRoutes(Set<CloudRoute> liveRoutes) {
        return liveRoutes.stream()
                         .map(this::modifyRoute)
                         .collect(Collectors.toSet());
    }

    private CloudRoute modifyRoute(CloudRoute inputRoute) {
        ImmutableCloudRoute.Builder modifiedRouteBuilder = ImmutableCloudRoute.builder()
                                                                              .from(inputRoute);
        String defaultDomain = getDefaultDomain();
        String defaultHost = getDefaultHost();

        if (defaultDomain != null) {
            modifiedRouteBuilder.domain(ImmutableCloudDomain.builder()
                                                            .name(defaultDomain)
                                                            .build());
        }

        if (defaultHost != null) {
            modifiedRouteBuilder.host(defaultHost);
        }

        var appUri = new ApplicationURI(defaultHost == null ? inputRoute.getHost() : defaultHost,
                                        defaultDomain == null ? inputRoute.getDomain()
                                                                          .getName()
                                            : defaultDomain,
                                        inputRoute.getPath());
        return modifiedRouteBuilder.url(appUri.toCloudRoute()
                                              .getUrl())
                                   .build();
    }
}
