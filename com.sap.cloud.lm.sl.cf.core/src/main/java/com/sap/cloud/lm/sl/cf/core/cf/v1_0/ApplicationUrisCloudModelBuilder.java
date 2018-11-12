package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.RoutingParameterSet;
import com.sap.cloud.lm.sl.cf.core.parser.IdleUriParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.UriParametersParser;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

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
        return (boolean) PropertiesUtil.getPropertyValue(propertiesList, SupportedParameters.KEEP_EXISTING_ROUTES, false);
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
        String defaultRoutePath = (String) propertiesAccessor.getParameters(module)
            .getOrDefault(SupportedParameters.ROUTE_PATH, null);
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
        String routePath = (String) propertiesAccessor.getParameters(module)
            .getOrDefault(SupportedParameters.ROUTE_PATH, null);
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
