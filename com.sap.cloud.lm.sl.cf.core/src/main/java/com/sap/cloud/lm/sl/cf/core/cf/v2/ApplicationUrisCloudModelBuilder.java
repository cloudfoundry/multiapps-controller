package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.RoutingParameterSet;
import com.sap.cloud.lm.sl.cf.core.parser.IdleUriParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.UriParametersParser;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class ApplicationUrisCloudModelBuilder {

    private final DeploymentDescriptor descriptor;
    private final boolean portBasedRouting;
    private final CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy;

    public ApplicationUrisCloudModelBuilder(DeploymentDescriptor descriptor, boolean portBasedRouting,
        CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy) {
        this.descriptor = descriptor;
        this.portBasedRouting = portBasedRouting;
        this.applicationAttributeUpdateStrategy = applicationAttributeUpdateStrategy;
    }

    private boolean includeProtocol() {
        Map<String, Object> parameters = descriptor.getParameters();
        String platform = (String) parameters.get(SupportedParameters.XS_TYPE);
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
            || applicationAttributeUpdateStrategy.shouldKeepExistingRoutes();
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
        Map<String, Object> moduleParameters = module.getParameters();
        String defaultHost = (String) moduleParameters.getOrDefault(parametersType.host, null);
        int defaultPort = (Integer) moduleParameters.getOrDefault(parametersType.port, 0);
        String defaultRoutePath = (String) module.getParameters()
            .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleParameters);
        String protocol = MapUtils.getString(moduleParameters, SupportedParameters.PROTOCOL);
        return new IdleUriParametersParser(portBasedRouting, defaultHost, defaultDomain, defaultPort, defaultRoutePath, includeProtocol(),
            protocol).parse(propertiesList);
    }

    private UriParametersParser getUriParametersParser(Module module) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT;
        Map<String, Object> moduleParameters = module.getParameters();
        String defaultHost = (String) moduleParameters.getOrDefault(parametersType.host, null);
        int defaultPort = (Integer) moduleParameters.getOrDefault(parametersType.port, 0);
        String routePath = (String) module.getParameters()
            .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleParameters);
        String protocol = MapUtils.getString(moduleParameters, SupportedParameters.PROTOCOL);
        return new UriParametersParser(portBasedRouting, defaultHost, defaultDomain, defaultPort, routePath, includeProtocol(), protocol);
    }

    private String getDefaultDomain(RoutingParameterSet parametersType, Map<String, Object> moduleParameters) {
        if (descriptor.getParameters()
            .containsKey(parametersType.domain)) {
            return (String) descriptor.getParameters()
                .get(parametersType.domain);
        }
        return (String) moduleParameters.getOrDefault(parametersType.domain, null);
    }

}
