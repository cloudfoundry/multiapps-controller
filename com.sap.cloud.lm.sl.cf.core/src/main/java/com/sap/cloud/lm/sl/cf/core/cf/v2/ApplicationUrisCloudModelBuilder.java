package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.RoutingParameterSet;
import com.sap.cloud.lm.sl.cf.core.parser.IdleUriParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.UriParametersParser;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class ApplicationUrisCloudModelBuilder {

    private boolean portBasedRouting;
    private SystemParameters systemParameters;
    private AttributeUpdateStrategy applicationAttributesUpdateStrategy;

    public ApplicationUrisCloudModelBuilder(boolean portBasedRouting, SystemParameters systemParameters,
        AttributeUpdateStrategy applicationAttributesUpdateStrategy) {
        this.portBasedRouting = portBasedRouting;
        this.systemParameters = systemParameters;
        this.applicationAttributesUpdateStrategy = applicationAttributesUpdateStrategy;
    }

    private boolean includeProtocol() {
        Map<String, Object> generalParameters = systemParameters.getGeneralParameters();
        String platform = (String) generalParameters.get(SupportedParameters.XS_TYPE);
        return PlatformType.XS2.toString()
            .equals(platform);
    }

    public List<String> getApplicationUris(Module module, List<Map<String, Object>> propertiesList, DeployedMtaModule deployedModule) {
        List<String> uris = getUriParametersParser(module).parse(propertiesList);
        if (shouldKeepExistingUris(propertiesList)) {
            return appendExistingUris(uris, deployedModule);
        }
        return uris;
    }

    private boolean shouldKeepExistingUris(List<Map<String, Object>> propertiesList) {
        return (boolean) getPropertyValue(propertiesList, SupportedParameters.KEEP_EXISTING_ROUTES, false)
            || applicationAttributesUpdateStrategy.shouldKeepExistingRoutes();
    }

    private Object getPropertyValue(List<Map<String, Object>> propertiesList, String propertyName, Object defaultValue) {
        return PropertiesUtil.getPropertyValue(propertiesList, propertyName, defaultValue);
    }

    private List<String> appendExistingUris(List<String> uris, DeployedMtaModule deployedModule) {
        List<String> result = new ArrayList<>(uris);
        if (deployedModule != null) {
            result.addAll(deployedModule.getUris());
        }
        return ListUtil.removeDuplicates(result);
    }

    public List<Integer> getApplicationPorts(Module module, List<Map<String, Object>> propertiesList) {
        return getUriParametersParser(module).getApplicationPorts(propertiesList);
    }

    public List<String> getApplicationDomains(Module module, List<Map<String, Object>> propertiesList) {
        return getUriParametersParser(module).getApplicationDomains(propertiesList);
    }

    public List<String> getIdleApplicationUris(Module module, List<Map<String, Object>> propertiesList) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT_IDLE;
        Map<String, Object> moduleSystemParameters = systemParameters.getModuleParameters()
            .getOrDefault(module.getName(), Collections.emptyMap());
        String defaultHost = (String) moduleSystemParameters.getOrDefault(parametersType.host, null);
        int defaultPort = (Integer) moduleSystemParameters.getOrDefault(parametersType.port, 0);
        String defaultRoutePath = (String) module.getParameters()
            .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleSystemParameters);
        String protocol = MapUtils.getString(moduleSystemParameters, SupportedParameters.PROTOCOL);
        return new IdleUriParametersParser(portBasedRouting, defaultHost, defaultDomain, defaultPort, defaultRoutePath, includeProtocol(),
            protocol).parse(propertiesList);
    }

    private UriParametersParser getUriParametersParser(Module module) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT;
        Map<String, Object> moduleSystemParameters = systemParameters.getModuleParameters()
            .getOrDefault(module.getName(), Collections.emptyMap());
        String defaultHost = (String) moduleSystemParameters.getOrDefault(parametersType.host, null);
        int defaultPort = (Integer) moduleSystemParameters.getOrDefault(parametersType.port, 0);
        String routePath = (String) module.getParameters()
            .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleSystemParameters);
        String protocol = MapUtils.getString(moduleSystemParameters, SupportedParameters.PROTOCOL);
        return new UriParametersParser(portBasedRouting, defaultHost, defaultDomain, defaultPort, routePath, includeProtocol(), protocol);
    }

    private String getDefaultDomain(RoutingParameterSet parametersType, Map<String, Object> moduleSystemParameters) {
        if (systemParameters.getGeneralParameters()
            .containsKey(parametersType.domain)) {
            return (String) systemParameters.getGeneralParameters()
                .get(parametersType.domain);
        }
        return (String) moduleSystemParameters.getOrDefault(parametersType.domain, null);
    }

}
