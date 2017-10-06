package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.RoutingParameterSet;
import com.sap.cloud.lm.sl.cf.core.parser.IdleUriParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.UriParametersParser;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class ApplicationUrisCloudModelBuilder {

    private boolean portBasedRouting;
    private SystemParameters systemParameters;
    private PropertiesAccessor propertiesAccessor;

    public ApplicationUrisCloudModelBuilder(boolean portBasedRouting, SystemParameters systemParameters,
        PropertiesAccessor propertiesAccessor) {
        this.portBasedRouting = portBasedRouting;
        this.systemParameters = systemParameters;
        this.propertiesAccessor = propertiesAccessor;
    }

    public List<String> getApplicationUris(Module module, List<Map<String, Object>> propertiesList) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT;
        Map<String, Object> moduleSystemParameters = systemParameters.getModuleParameters().getOrDefault(module.getName(),
            Collections.emptyMap());
        String defaultHost = (String) moduleSystemParameters.getOrDefault(parametersType.host, null);
        int defaultPort = (Integer) moduleSystemParameters.getOrDefault(parametersType.port, 0);
        String routePath = (String) propertiesAccessor.getParameters(module).getOrDefault(SupportedParameters.ROUTE_PATH, null);
        String defaultDomain = getDefaultDomain(parametersType, moduleSystemParameters);
        return new UriParametersParser(portBasedRouting, defaultHost, defaultDomain, defaultPort, routePath).parse(propertiesList);
    }

    public List<String> getIdleApplicationUris(Module module, List<Map<String, Object>> propertiesList) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT_IDLE;
        Map<String, Object> moduleSystemParameters = systemParameters.getModuleParameters().getOrDefault(module.getName(),
            Collections.emptyMap());
        String defaultHost = (String) moduleSystemParameters.getOrDefault(parametersType.host, null);
        int defaultPort = (Integer) moduleSystemParameters.getOrDefault(parametersType.port, 0);
        String defaultRoutePath = (String) propertiesAccessor.getParameters(module).getOrDefault(SupportedParameters.ROUTE_PATH, null);
        String defaultDomain = getDefaultDomain(parametersType, moduleSystemParameters);
        return new IdleUriParametersParser(portBasedRouting, defaultHost, defaultDomain, defaultPort, defaultRoutePath).parse(
            propertiesList);
    }

    private String getDefaultDomain(RoutingParameterSet parametersType, Map<String, Object> moduleSystemParameters) {
        if (systemParameters.getGeneralParameters().containsKey(parametersType.domain)) {
            return (String) systemParameters.getGeneralParameters().get(parametersType.domain);
        }
        return (String) moduleSystemParameters.getOrDefault(parametersType.domain, null);
    }

}
