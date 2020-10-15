package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class IdleRouteParametersParser extends RouteParametersParser {

    public IdleRouteParametersParser(String defaultHost, String defaultDomain, String routePath) {
        super(defaultHost, defaultDomain, SupportedParameters.IDLE_HOST, SupportedParameters.IDLE_DOMAIN, routePath);
    }

    public IdleRouteParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
                                   String routePath) {
        super(defaultHost, defaultDomain, hostParameterName, domainParameterName, routePath);
    }

    @Override
    public Set<CloudRouteSummary> getApplicationRoutes(List<Map<String, Object>> parametersList) {
        Set<CloudRouteSummary> idleRoutes = getIdleRoutes(parametersList);
        if (!idleRoutes.isEmpty()) {
            return idleRoutes;
        }

        Set<CloudRouteSummary> liveRoutes = super.getApplicationRoutes(parametersList);
        if (!liveRoutes.isEmpty()) {
            return modifyLiveRoutes(liveRoutes);
        }
        return Collections.emptySet();
    }

    private Set<CloudRouteSummary> getIdleRoutes(List<Map<String, Object>> parametersList) {
        List<Map<String, Object>> idleRoutesMaps = RoutesValidator.applyRoutesType(PropertiesUtil.getPropertyValue(parametersList,
                                                                                                                   SupportedParameters.IDLE_ROUTES,
                                                                                                                   null));
        return idleRoutesMaps.stream()
                             .map(this::parseIdleRouteMap)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toSet());

    }

    public CloudRouteSummary parseIdleRouteMap(Map<String, Object> routeMap) {
        String routeString = (String) routeMap.get(SupportedParameters.IDLE_ROUTE);
        boolean noHostname = MapUtil.parseBooleanFlag(routeMap, SupportedParameters.NO_HOSTNAME, false);

        if (routeString == null) {
            return null;
        }

        return new ApplicationURI(routeString, noHostname).toCloudRouteSummary();
    }

    private Set<CloudRouteSummary> modifyLiveRoutes(Set<CloudRouteSummary> liveRoutes) {
        return liveRoutes.stream()
                         .map(this::modifyRoute)
                         .collect(Collectors.toSet());
    }

    private CloudRouteSummary modifyRoute(CloudRouteSummary inputRoute) {
        ImmutableCloudRouteSummary.Builder modifiedRouteBuilder = ImmutableCloudRouteSummary.builder()
                                                                                            .from(inputRoute);
        String defaultDomain = getDefaultDomain();
        String defaultHost = getDefaultHost();

        if (defaultDomain != null) {
            modifiedRouteBuilder.domain(defaultDomain);
        }

        if (defaultHost != null) {
            modifiedRouteBuilder.host(defaultHost);
        }

        return modifiedRouteBuilder.build();
    }
}
