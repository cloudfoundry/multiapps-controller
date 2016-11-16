package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getAll;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class UriParametersParser implements ParametersParser<List<String>> {

    private boolean portBasedRouting;
    private String defaultHost;
    private String defaultDomain;
    private Integer defaultPort;
    private String hostParameterName;
    private String domainParameterName;
    private String portParameterName;
    private String routePath;

    public UriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String routePath) {
        this(portBasedRouting, defaultHost, defaultDomain, defaultPort, SupportedParameters.HOST, SupportedParameters.DOMAIN,
            SupportedParameters.PORT, routePath);
    }

    public UriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String hostParameterName, String domainParameterName, String portParameterName, String routePath) {
        this.portBasedRouting = portBasedRouting;
        this.defaultHost = defaultHost;
        this.defaultDomain = defaultDomain;
        this.defaultPort = defaultPort;
        this.hostParameterName = hostParameterName;
        this.domainParameterName = domainParameterName;
        this.portParameterName = portParameterName;
        this.routePath = routePath;
    }

    @Override
    public List<String> parse(List<Map<String, Object>> parametersList) {
        boolean noRoute = (Boolean) getPropertyValue(parametersList, SupportedParameters.NO_ROUTE, false);
        if (noRoute) {
            return Collections.emptyList();
        }
        return getUris(getApplicationHosts(parametersList), getApplicationDomains(parametersList), getApplicationPorts(parametersList));
    }

    private List<Integer> getApplicationPorts(List<Map<String, Object>> parametersList) {
        List<Integer> ports = getAll(parametersList, portParameterName, SupportedParameters.SINGULAR_PLURAL_MAPPING.get(portParameterName));
        if (ports.isEmpty() && defaultPort != null && defaultPort != 0) {
            ports.add(defaultPort);
        }
        return ports;
    }

    private void addPortBasedUris(Set<String> uris, String domain, List<Integer> ports) {
        for (Integer port : ports) {
            uris.add(appendRoutePathIfPresent(domain + ":" + port));
        }
    }

    private List<String> getApplicationDomains(List<Map<String, Object>> parametersList) {
        List<String> domains = getAll(parametersList, domainParameterName,
            SupportedParameters.SINGULAR_PLURAL_MAPPING.get(domainParameterName));
        if (domains.isEmpty() && defaultDomain != null) {
            domains.add(defaultDomain);
        }
        return domains;
    }

    private List<String> getApplicationHosts(List<Map<String, Object>> parametersList) {
        boolean noHostname = (Boolean) getPropertyValue(parametersList, SupportedParameters.NO_HOSTNAME, false);
        if (noHostname) {
            return Collections.emptyList();
        }
        List<String> hostss = getAll(parametersList, hostParameterName, SupportedParameters.SINGULAR_PLURAL_MAPPING.get(hostParameterName));
        if (hostss.isEmpty() && defaultHost != null && !noHostname) {
            hostss.add(defaultHost);
        }
        return hostss;
    }

    private void addHostBasedUris(Set<String> uris, String domain, List<String> hosts) {
        for (String host : hosts) {
            uris.add(appendRoutePathIfPresent(host + "." + domain));
        }
    }

    private List<String> getUris(List<String> hosts, List<String> domains, List<Integer> ports) {
        Set<String> uris = new LinkedHashSet<>();
        if (!domains.isEmpty()) {
            for (String domain : domains) {
                if (shouldUsePortBasedUris(ports, hosts)) {
                    addPortBasedUris(uris, domain, ports);
                } else if (!hosts.isEmpty()) {
                    addHostBasedUris(uris, domain, hosts);
                } else {
                    uris.add(appendRoutePathIfPresent(domain));
                }
            }
        } else if (!hosts.isEmpty()) {
            return getUris(Collections.emptyList(), hosts, ports); // Use hosts as domains;
        }
        return new ArrayList<>(uris);
    }

    private String appendRoutePathIfPresent(String uri) {
        if (routePath != null) {
            return uri + routePath;
        }
        return uri;
    }

    private boolean shouldUsePortBasedUris(List<Integer> ports, List<String> hosts) {
        return (portBasedRouting || hosts.isEmpty()) && !ports.isEmpty();
    }

}
