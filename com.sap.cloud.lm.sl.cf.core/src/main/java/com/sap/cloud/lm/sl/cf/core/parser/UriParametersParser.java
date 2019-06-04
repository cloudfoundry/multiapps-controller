package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPluralOrSingular;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
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

    private String defaultHost;
    private String defaultDomain;
    private String hostParameterName;
    private String domainParameterName;
    private boolean modifyRoute;
    private String routePath;

    public UriParametersParser(String defaultHost, String defaultDomain, String routePath) {
        this(defaultHost, defaultDomain, SupportedParameters.HOST, SupportedParameters.DOMAIN, false, routePath);
    }

    public UriParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
        boolean modifyRoute, String routePath) {
        this.defaultHost = defaultHost;
        this.defaultDomain = defaultDomain;
        this.hostParameterName = hostParameterName;
        this.domainParameterName = domainParameterName;
        this.modifyRoute = modifyRoute;
        this.routePath = routePath;
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

        if (domains.isEmpty() && !hosts.isEmpty()) {
            // Use hosts as domains.
            domains = hosts;
            hosts = Collections.emptyList();
        }

        return assembleUris(hosts, domains);
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

        if (!domains.isEmpty()) {
            modifiedURI.setDomain(domains.get(0));
        }

        if (!hosts.isEmpty()) {
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

    private static <T> List<T> getValuesFromSingularName(String singularParameterName, List<Map<String, Object>> parametersList) {
        String pluralParameterName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(singularParameterName);
        return getPluralOrSingular(parametersList, pluralParameterName, singularParameterName);
    }

    private String appendRoutePathIfPresent(String uri) {
        if (routePath != null) {
            return uri + routePath;
        }
        return uri;
    }

}
