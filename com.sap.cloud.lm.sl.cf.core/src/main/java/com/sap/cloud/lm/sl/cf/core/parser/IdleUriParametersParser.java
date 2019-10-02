package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RoutesValidator;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class IdleUriParametersParser extends UriParametersParser {

    public IdleUriParametersParser(String defaultHost, String defaultDomain, String routePath) {
        super(defaultHost, defaultDomain, SupportedParameters.IDLE_HOST, SupportedParameters.IDLE_DOMAIN, routePath);
    }

    public IdleUriParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
                                   String routePath) {
        super(defaultHost, defaultDomain, hostParameterName, domainParameterName, routePath);
    }

    @Override
    public List<String> getApplicationRoutes(List<Map<String, Object>> parametersList) {
        List<String> idleRoutes = getIdleRoutes(parametersList);
        if (!idleRoutes.isEmpty()) {
            return idleRoutes;
        }

        List<String> liveRoutes = super.getApplicationRoutes(parametersList);
        if (!liveRoutes.isEmpty()) {
            return modifyLiveRoutes(liveRoutes);
        }
        return Collections.emptyList();
    }

    private List<String> getIdleRoutes(List<Map<String, Object>> parametersList) {
        List<Map<String, Object>> idleRoutesMaps = RoutesValidator.applyRoutesType(PropertiesUtil.getPropertyValue(parametersList,
                                                                                                                   SupportedParameters.IDLE_ROUTES,
                                                                                                                   null));
        return idleRoutesMaps.stream()
                             .map(routesMap -> (String) routesMap.get(SupportedParameters.IDLE_ROUTE))
                             .filter(Objects::nonNull)
                             .map(UriUtil::stripScheme)
                             .collect(Collectors.toList());

    }

    private List<String> modifyLiveRoutes(List<String> liveRoutes) {
        return liveRoutes.stream()
                         .map(this::modifyUri)
                         .collect(Collectors.toList());
    }

    private String modifyUri(String inputURI) {
        ApplicationURI modifiedURI = new ApplicationURI(inputURI);

        String defaultDomain = getDefaultDomain();
        String defaultHost = getDefaultHost();

        if (defaultDomain != null) {
            modifiedURI.setDomain(defaultDomain);
        }

        if (defaultHost != null) {
            modifiedURI.setHost(defaultHost);
        }

        return modifiedURI.toString();
    }
}
