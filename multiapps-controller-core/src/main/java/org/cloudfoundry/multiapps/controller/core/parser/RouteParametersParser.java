package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class RouteParametersParser implements ParametersParser<Set<CloudRoute>> {

    private final String defaultHost;
    private final String defaultDomain;
    private final String hostParameterName;
    private final String domainParameterName;
    private final String routePath;

    public RouteParametersParser(String defaultHost, String defaultDomain, String routePath) {
        this(defaultHost, defaultDomain, SupportedParameters.HOST, SupportedParameters.DOMAIN, routePath);
    }

    public RouteParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
                               String routePath) {
        this.defaultHost = defaultHost;
        this.defaultDomain = defaultDomain;
        this.hostParameterName = hostParameterName;
        this.domainParameterName = domainParameterName;
        this.routePath = routePath != null ? routePath : "";
    }

    @Override
    public Set<CloudRoute> parse(List<Map<String, Object>> parametersList) {
        boolean noRoute = (Boolean) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.NO_ROUTE, false);
        if (noRoute) {
            return Collections.emptySet();
        }
        return getRoutes(parametersList);
    }

    private Set<CloudRoute> getRoutes(List<Map<String, Object>> parametersList) {
        Set<CloudRoute> routes = getApplicationRoutes(parametersList);
        if (!routes.isEmpty()) {
            return routes;
        }

        List<String> hosts = getHostValues(parametersList);
        List<String> domains = getDomainValues(parametersList);

        if (domains.isEmpty() && !hosts.isEmpty()) {
            // Use hosts as domains.
            domains = hosts;
            hosts = Collections.emptyList();
        }

        return assembleRoutes(hosts, domains);
    }

    public List<String> getApplicationDomains(List<Map<String, Object>> parametersList) {
        Set<CloudRoute> routes = getApplicationRoutes(parametersList);
        if (!routes.isEmpty()) {
            return getDomainsFromRoutes(routes);
        }
        return getDomainValues(parametersList);
    }

    protected List<String> getHostValues(List<Map<String, Object>> parametersList) {
        boolean noHostname = (Boolean) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.NO_HOSTNAME, false);
        if (noHostname) {
            return Collections.emptyList();
        }

        List<String> hosts = getValuesFromSingularName(hostParameterName, parametersList);

        if (hosts.isEmpty() && defaultHost != null) {
            hosts.add(defaultHost);
        }
        return hosts;
    }

    protected List<String> getDomainValues(List<Map<String, Object>> parametersList) {
        List<String> domains = getValuesFromSingularName(domainParameterName, parametersList);

        if (domains.isEmpty() && defaultDomain != null) {
            domains.add(defaultDomain);
        }
        return domains;
    }

    /**
     * This method is doing a DesCartesian multiplication for given hosts and domains and returns constructed routes
     * 
     * @param hosts
     * @param domains
     * @return set of all routes created
     */
    private Set<CloudRoute> assembleRoutes(List<String> hosts, List<String> domains) {
        Set<CloudRoute> routes = new LinkedHashSet<>();
        for (String domain : domains) {
            if (!hosts.isEmpty()) {
                addHostBasedRoutes(routes, domain, hosts);
            } else {
                routes.add(buildCloudRoute("", domain));
            }
        }

        return routes;
    }

    private void addHostBasedRoutes(Set<CloudRoute> routes, String domain, List<String> hosts) {
        for (String host : hosts) {
            routes.add(buildCloudRoute(host, domain));
        }
    }

    public Set<CloudRoute> getApplicationRoutes(List<Map<String, Object>> parametersList) {
        List<Map<String, Object>> routesMaps = RoutesValidator.applyRoutesType(PropertiesUtil.getPropertyValue(parametersList,
                                                                                                               SupportedParameters.ROUTES,
                                                                                                               null));

        return routesMaps.stream()
                         .map(this::parseRouteMap)
                         .filter(Objects::nonNull)
                         .collect(Collectors.toSet());
    }

    public CloudRoute parseRouteMap(Map<String, Object> routeMap) {
        String routeString = (String) routeMap.get(SupportedParameters.ROUTE);
        boolean noHostname = MapUtil.parseBooleanFlag(routeMap, SupportedParameters.NO_HOSTNAME, false);

        if (routeString == null) {
            return null;
        }

        return new ApplicationURI(routeString, noHostname).toCloudRoute();
    }

    private List<String> getDomainsFromRoutes(Set<CloudRoute> routes) {
        return routes.stream()
                     .map(route -> route.getDomain()
                                        .getName())
                     .filter(Objects::nonNull)
                     .distinct()
                     .collect(Collectors.toList());
    }

    private static <T> List<T> getValuesFromSingularName(String singularParameterName, List<Map<String, Object>> parametersList) {
        String pluralParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(singularParameterName);
        return PropertiesUtil.getPluralOrSingular(parametersList, pluralParameterName, singularParameterName);
    }

    private CloudRoute buildCloudRoute(String host, String domain) {
        return new ApplicationURI(host, domain, routePath).toCloudRoute();
    }

    protected String getDefaultHost() {
        return defaultHost;
    }

    protected String getDefaultDomain() {
        return defaultDomain;
    }

}
