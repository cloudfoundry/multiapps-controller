package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getAll;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;

public class UriParametersParser implements ParametersParser<List<String>> {

    private boolean portBasedRouting;
    private boolean includeProtocol;
    private String defaultHost;
    private String defaultDomain;
    private Integer defaultPort;
    private String hostParameterName;
    private String domainParameterName;
    private String portParameterName;
    private String routePath;
    private String protocol;

    public UriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort, String routePath,
        boolean includeProtocol, String protocol) {
        this(portBasedRouting, defaultHost, defaultDomain, defaultPort, SupportedParameters.HOST, SupportedParameters.DOMAIN,
            SupportedParameters.PORT, routePath, includeProtocol, protocol);
    }

    public UriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String hostParameterName, String domainParameterName, String portParameterName, String routePath, boolean includeProtocol,
        String protocol) {
        this.portBasedRouting = portBasedRouting;
        this.includeProtocol = includeProtocol;
        this.defaultHost = defaultHost;
        this.defaultDomain = defaultDomain;
        this.defaultPort = defaultPort;
        this.hostParameterName = hostParameterName;
        this.domainParameterName = domainParameterName;
        this.portParameterName = portParameterName;
        this.routePath = routePath;
        this.protocol = protocol;
    }

    @Override
    public List<String> parse(List<Map<String, Object>> parametersList) {
        boolean noRoute = (Boolean) getPropertyValue(parametersList, SupportedParameters.NO_ROUTE, false);
        if (noRoute) {
            return Collections.emptyList();
        }
        return getUris(getApplicationHosts(parametersList), getApplicationDomains(parametersList), getApplicationPorts(parametersList));
    }

    public List<Integer> getApplicationPorts(List<Map<String, Object>> parametersList) {
        String portsParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(portParameterName);
        List<Integer> ports = getAll(parametersList, portParameterName, portsParameterName);
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

    public List<String> getApplicationDomains(List<Map<String, Object>> parametersList) {
        String domainsParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(domainParameterName);
        List<String> domains = getAll(parametersList, domainParameterName, domainsParameterName);
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
        String hostsParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(hostParameterName);
        List<String> hosts = getAll(parametersList, hostParameterName, hostsParameterName);
        if (hosts.isEmpty() && defaultHost != null) {
            hosts.add(defaultHost);
        }
        return hosts;
    }

    private void addHostBasedUris(Set<String> uris, String domain, List<String> hosts) {
        for (String host : hosts) {
            uris.add(appendRoutePathIfPresent(host + "." + domain));
        }
    }

    private List<String> getUris(List<String> hosts, List<String> domains, List<Integer> ports) {
        if (domains.isEmpty() && hosts.isEmpty()) {
            return new ArrayList<>();
        }
        if (domains.isEmpty() && !hosts.isEmpty()) {
            return getUris(Collections.emptyList(), hosts, ports); // Use hosts as domains.
        }
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

    private String addProtocol(String uri) {
        if (includeProtocol) {
            return protocol + UriUtil.DEFAULT_SCHEME_SEPARATOR + uri;
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
