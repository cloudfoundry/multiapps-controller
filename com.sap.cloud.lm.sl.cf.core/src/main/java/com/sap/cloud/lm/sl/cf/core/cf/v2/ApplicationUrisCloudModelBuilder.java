package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.RoutingParameterSet;
import com.sap.cloud.lm.sl.cf.core.parser.IdleUriParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.UriParametersParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class ApplicationUrisCloudModelBuilder {

    private final DeploymentDescriptor descriptor;
    private final CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy;

    public ApplicationUrisCloudModelBuilder(DeploymentDescriptor descriptor,
                                            CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy) {
        this.descriptor = descriptor;
        this.applicationAttributeUpdateStrategy = applicationAttributeUpdateStrategy;
    }

    public List<String> getApplicationUris(Module module, List<Map<String, Object>> propertiesList,
                                           DeployedMtaApplication deployedApplication) {
        List<String> uris = getUriParametersParser(module).parse(propertiesList);
        if (shouldKeepExistingUris(propertiesList)) {
            return appendExistingUris(uris, deployedApplication);
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

    private List<String> appendExistingUris(List<String> uris, DeployedMtaApplication deployedMtaApplication) {
        Set<String> result = new HashSet<>(uris);
        if (deployedMtaApplication != null) {
            result.addAll(deployedMtaApplication.getUris());
        }
        return new ArrayList<>(result);
    }

    public List<String> getApplicationDomains(Module module, List<Map<String, Object>> propertiesList) {
        return getUriParametersParser(module).getApplicationDomains(propertiesList);
    }

    public List<String> getIdleApplicationUris(Module module, List<Map<String, Object>> propertiesList) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT_IDLE;
        Map<String, Object> moduleParameters = module.getParameters();
        String defaultHost = (String) moduleParameters.getOrDefault(parametersType.host, null);
        String defaultRoutePath = (String) module.getParameters()
                                                 .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleParameters);
        return new IdleUriParametersParser(defaultHost, defaultDomain, defaultRoutePath).parse(propertiesList);
    }

    private UriParametersParser getUriParametersParser(Module module) {
        RoutingParameterSet parametersType = RoutingParameterSet.DEFAULT;
        Map<String, Object> moduleParameters = module.getParameters();
        String defaultHost = (String) moduleParameters.getOrDefault(parametersType.host, null);
        String routePath = (String) module.getParameters()
                                          .get(SupportedParameters.ROUTE_PATH);
        String defaultDomain = getDefaultDomain(parametersType, moduleParameters);
        return new UriParametersParser(defaultHost, defaultDomain, routePath);
    }

    private String getDefaultDomain(RoutingParameterSet parametersType, Map<String, Object> moduleParameters) {
        if (descriptor.getParameters()
                      .containsKey(parametersType.domain)) {
            return (String) descriptor.getParameters()
                                      .get(parametersType.domain);
        }
        return (String) moduleParameters.get(parametersType.domain);
    }

}
