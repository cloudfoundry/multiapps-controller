package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getAll;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RoutesValidator;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class UriParametersParser implements ParametersParser<List<String>> {

    private boolean portBasedRouting;
    private boolean includeProtocol;
    private String defaultHost;
    private String defaultDomain;
    private Integer defaultPort;
    private String hostParameterName;
    private String domainParameterName;
    private String portParameterName;
    private boolean modifyRoute;
    private String routePath;
    private String protocol;

    public UriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort, String routePath,
        boolean includeProtocol, String protocol) {
        this(portBasedRouting, defaultHost, defaultDomain, defaultPort, SupportedParameters.HOST, SupportedParameters.DOMAIN,
            SupportedParameters.PORT, false, routePath, includeProtocol, protocol);
    }

    public UriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String hostParameterName, String domainParameterName, String portParameterName, boolean modifyRoute, String routePath,
        boolean includeProtocol, String protocol) {
        this.portBasedRouting = portBasedRouting;
        this.includeProtocol = includeProtocol;
        this.defaultHost = defaultHost;
        this.defaultDomain = defaultDomain;
        this.defaultPort = defaultPort;
        this.hostParameterName = hostParameterName;
        this.domainParameterName = domainParameterName;
        this.portParameterName = portParameterName;
        this.modifyRoute = modifyRoute;
        this.routePath = routePath;
        this.protocol = protocol;
    }

    @Override
    public List<String> parse(List<Map<String, Object>> parametersList) {
        boolean noRoute = (Boolean) getPropertyValue(parametersList, SupportedParameters.NO_ROUTE, false);
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
        List<Integer> ports = getPortValues(parametersList);

        if (domains.isEmpty() && !hosts.isEmpty()) {
            // Use hosts as domains.
            domains = hosts;
            hosts = Collections.emptyList();
        }

        return assembleUris(hosts, domains, ports);
    }

    public List<String> getApplicationDomains(List<Map<String, Object>> parametersList) {
        List<String> domains;
        List<String> routes = getApplicationRoutes(parametersList);
        if (!routes.isEmpty()) {
            domains = getDomainsFromRoutes(routes);
        } else {
            domains = getDomainValues(parametersList);
        }

        return domains;
    }

    public List<Integer> getApplicationPorts(List<Map<String, Object>> parametersList) {
        List<String> routes = getApplicationRoutes(parametersList);

        if (routes.isEmpty()) {
            return getPortValues(parametersList);
        }

        return getPortsFromRoutes(routes);
    }

    private List<String> getHostValues(List<Map<String, Object>> parametersList) {
        boolean noHostname = (Boolean) getPropertyValue(parametersList, SupportedParameters.NO_HOSTNAME, false);
        if (noHostname) {
            return Collections.emptyList();
        }

        List<String> hosts = getValuesFromSingularName(hostParameterName, parametersList);

        if (hosts.isEmpty() && defaultHost != null) {
            hosts.add(defaultHost);
        }
        return hosts;
    }

    private List<String> getDomainValues(List<Map<String, Object>> parametersList) {
        List<String> domains = getValuesFromSingularName(domainParameterName, parametersList);

        if (domains.isEmpty() && defaultDomain != null) {
            domains.add(defaultDomain);
        }
        return domains;
    }

    private List<Integer> getPortValues(List<Map<String, Object>> parametersList) {
        List<Integer> ports = getValuesFromSingularName(portParameterName, parametersList);

        if (ports.isEmpty() && defaultPort != null && defaultPort != 0) {
            ports.add(defaultPort);
        }

        return ports;
    }

    private List<String> assembleUris(List<String> hosts, List<String> domains, List<Integer> ports) {
        Set<String> uris = new LinkedHashSet<>();
        for (String domain : domains) {
            if (shouldUsePortBasedUris(ports, hosts)) {
                addPortBasedUris(uris, domain, ports);
            } else if (!hosts.isEmpty()) {
                addHostBasedUris(uris, domain, hosts);
            } else {
                uris.add(appendRoutePathIfPresent(domain));
            }
        }

        return uris.stream()
            .map(this::addProtocol)
            .collect(Collectors.toList());
    }

    private void addPortBasedUris(Set<String> uris, String domain, List<Integer> ports) {
        for (Integer port : ports) {
            uris.add(appendRoutePathIfPresent(domain + ":" + port));
        }
    }

    private void addHostBasedUris(Set<String> uris, String domain, List<String> hosts) {
        for (String host : hosts) {
            uris.add(appendRoutePathIfPresent(host + "." + domain));
        }
    }

    public List<String> getApplicationRoutes(List<Map<String, Object>> parametersList) {

        List<Map<String, Object>> routesMaps = RoutesValidator
            .applyRoutesType(PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.ROUTES, null));

        List<String> allNonNullRoutes = CollectionUtils.emptyIfNull(routesMaps)
            .stream()
            .map(routesMap -> (String) routesMap.get(SupportedParameters.ROUTE))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (modifyRoute) {
            return allNonNullRoutes.stream()
                .map(route -> modifyUri(route, parametersList))
                .collect(Collectors.toList());
        }

        return allNonNullRoutes;
    }

    public String modifyUri(String inputURI, List<Map<String, Object>> customURIParts) {
        ApplicationURI modifiedURI = new ApplicationURI(inputURI);

        List<String> domains = getDomainValues(customURIParts);
        List<String> hosts = getHostValues(customURIParts);
        List<Integer> ports = getPortValues(customURIParts);

        if (!domains.isEmpty()) {
            modifiedURI.setDomain(domains.get(0));
        }

        if (shouldUsePortBasedUris(ports, hosts)) {
            modifiedURI.setPort(ports.get(0));
        } else if (!hosts.isEmpty()) {
            modifiedURI.setHost(hosts.get(0));
        }

        return modifiedURI.toString();
    }

    private List<String> getDomainsFromRoutes(List<String> routes) {
        return routes.stream()
            .map(ApplicationURI::getDomainFromURI)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<Integer> getPortsFromRoutes(List<String> routes) {
        return routes.stream()
            .map(ApplicationURI::getPortFromURI)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private static <T> List<T> getValuesFromSingularName(String singularParameterName, List<Map<String, Object>> parametersList) {
        String pluralParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(singularParameterName);
        return getAll(parametersList, singularParameterName, pluralParameterName);
    }

    private String addProtocol(String uri) {
        if (includeProtocol) {
            return protocol + ApplicationURI.DEFAULT_SCHEME_SEPARATOR + uri;
        }
        return uri;
    }

    private String appendRoutePathIfPresent(String uri) {
        if (routePath != null) {
            return uri + routePath;
        }
        return uri;
    }

    private boolean shouldUsePortBasedUris(List<Integer> ports, List<String> hosts) {
        return (portBasedRouting || hosts.isEmpty() || isTcpOrTcps()) && !ports.isEmpty();
    }

    private boolean isTcpOrTcps() {
        return SupportedParameters.TCP.equals(protocol) || SupportedParameters.TCPS.equals(protocol);
    }

}
