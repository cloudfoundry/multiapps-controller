package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class UriParametersParser implements ParametersParser<List<String>> {

    private final String defaultHost;
    private final String defaultDomain;
    private final String hostParameterName;
    private final String domainParameterName;
    private final String routePath;

    public UriParametersParser(String defaultHost, String defaultDomain, String routePath) {
        this(defaultHost, defaultDomain, SupportedParameters.HOST, SupportedParameters.DOMAIN, routePath);
    }

    public UriParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
                               String routePath) {
        this.defaultHost = defaultHost;
        this.defaultDomain = defaultDomain;
        this.hostParameterName = hostParameterName;
        this.domainParameterName = domainParameterName;
        this.routePath = routePath;
    }

    @Override
    public List<String> parse(List<Map<String, Object>> parametersList) {
        boolean noRoute = (Boolean) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.NO_ROUTE, false);
        if (noRoute) {
            return Collections.emptyList();
        }
        return getUris(parametersList);
    }

    private List<String> getUris(List<Map<String, Object>> parametersList) {
        List<String> routes = getApplicationRoutes(parametersList);
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

        return assembleUris(hosts, domains);
    }

    public List<String> getApplicationDomains(List<Map<String, Object>> parametersList) {
        List<String> routes = getApplicationRoutes(parametersList);
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

    private List<String> assembleUris(List<String> hosts, List<String> domains) {
        Set<String> uris = new LinkedHashSet<>();
        for (String domain : domains) {
            if (!hosts.isEmpty()) {
                addHostBasedUris(uris, domain, hosts);
            } else {
                uris.add(appendRoutePathIfPresent(domain));
            }
        }

        return new ArrayList<>(uris);
    }

    private void addHostBasedUris(Set<String> uris, String domain, List<String> hosts) {
        for (String host : hosts) {
            uris.add(appendRoutePathIfPresent(host + "." + domain));
        }
    }

    public List<String> getApplicationRoutes(List<Map<String, Object>> parametersList) {
        List<Map<String, Object>> routesMaps = RoutesValidator.applyRoutesType(PropertiesUtil.getPropertyValue(parametersList,
                                                                                                               SupportedParameters.ROUTES,
                                                                                                               null));

        return routesMaps.stream()
                         .map(routesMap -> (String) routesMap.get(SupportedParameters.ROUTE))
                         .filter(Objects::nonNull)
                         .map(UriUtil::stripScheme)
                         .collect(Collectors.toList());
    }

    private List<String> getDomainsFromRoutes(List<String> routes) {
        return routes.stream()
                     .map(ApplicationURI::getDomainFromURI)
                     .filter(Objects::nonNull)
                     .distinct()
                     .collect(Collectors.toList());
    }

    private static <T> List<T> getValuesFromSingularName(String singularParameterName, List<Map<String, Object>> parametersList) {
        String pluralParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(singularParameterName);
        return PropertiesUtil.getPluralOrSingular(parametersList, pluralParameterName, singularParameterName);
    }

    private String appendRoutePathIfPresent(String uri) {
        if (routePath != null) {
            return uri + routePath;
        }
        return uri;
    }

    protected String getDefaultHost() {
        return defaultHost;
    }

    protected String getDefaultDomain() {
        return defaultDomain;
    }

}
