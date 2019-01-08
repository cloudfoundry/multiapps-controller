package com.sap.cloud.lm.sl.cf.core.cf.v2;

import static com.sap.cloud.lm.sl.common.util.ListUtil.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;

public class ApplicationEnvironmentCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 2;

    protected CloudModelConfiguration configuration;
    protected DeploymentDescriptor deploymentDescriptor;
    protected XsPlaceholderResolver xsPlaceholderResolver;
    protected DescriptorHandler handler;
    protected PropertiesAccessor propertiesAccessor;
    protected String deployId;

    public ApplicationEnvironmentCloudModelBuilder(CloudModelConfiguration configuration, DeploymentDescriptor deploymentDescriptor,
        XsPlaceholderResolver xsPlaceholderResolver, DescriptorHandler handler, PropertiesAccessor propertiesAccessor, String deployId) {
        this.configuration = configuration;
        this.deploymentDescriptor = deploymentDescriptor;
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.handler = handler;
        this.propertiesAccessor = propertiesAccessor;
        this.deployId = deployId;
    }

    public Map<Object, Object> build(Module module, List<String> services) {
        Set<String> specialModuleProperties = buildSpecialModulePropertiesSet();
        Map<String, Object> properties = propertiesAccessor.getProperties(module, specialModuleProperties);
        Map<String, Object> parameters = propertiesAccessor.getParameters(module, specialModuleProperties);
        Map<String, Object> env = new TreeMap<>();
        addMetadata(env, module);
        addServices(env, services);
        addAttributes(env, parameters);
        addProperties(env, properties);
        addDependencies(env, module);
        return MapUtil.unmodifiable(new MapToEnvironmentConverter(configuration.isPrettyPrinting()).asEnv(env));
    }
    
    private Set<String> buildSpecialModulePropertiesSet() {
        Set<String> result = new HashSet<>();
        result.addAll(SupportedParameters.APP_PROPS);
        result.addAll(SupportedParameters.APP_ATTRIBUTES);
        result.addAll(SupportedParameters.SPECIAL_MT_PROPS);
        return result;
    }

    protected void addMetadata(Map<String, Object> env, Module module) {
        addMtaMetadata(env);
        addMtaModuleMetadata(env, module);
        addProvidedDependenciesMetadata(env, module);
    }

    protected void addMtaMetadata(Map<String, Object> env) {
        Map<String, Object> mtaMetadata = new TreeMap<>();
        MapUtil.addNonNull(mtaMetadata, Constants.ATTR_ID, deploymentDescriptor.getId());
        MapUtil.addNonNull(mtaMetadata, Constants.ATTR_VERSION, deploymentDescriptor.getVersion());
        MapUtil.addNonNull(mtaMetadata, Constants.ATTR_DESCRIPTION, deploymentDescriptor.getDescription());
        MapUtil.addNonNull(mtaMetadata, Constants.ATTR_PROVIDER, deploymentDescriptor.getProvider());
        MapUtil.addNonNull(mtaMetadata, Constants.ATTR_COPYRIGHT, deploymentDescriptor.getCopyright());
        env.put(Constants.ENV_MTA_METADATA, mtaMetadata);
    }

    protected void addMtaModuleMetadata(Map<String, Object> env, Module module) {
        Map<String, Object> mtaModuleMetadata = new TreeMap<>();
        MapUtil.addNonNull(mtaModuleMetadata, Constants.ATTR_NAME, module.getName());
        MapUtil.addNonNull(mtaModuleMetadata, Constants.ATTR_DESCRIPTION, module.getDescription());
        env.put(Constants.ENV_MTA_MODULE_METADATA, mtaModuleMetadata);
    }
    
    protected void addProvidedDependenciesMetadata(Map<String, Object> env, Module module) {
        List<String> mtaModuleProvidedDependencies = module.getProvidedDependencies2()
            .stream()
            .filter(CloudModelBuilderUtil::isPublic)
            .map(ProvidedDependency::getName)
            .collect(Collectors.toList());
        env.put(Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES, mtaModuleProvidedDependencies);
    }
    
    protected void addServices(Map<String, Object> env, List<String> services) {
        env.put(Constants.ENV_MTA_SERVICES, services);
    }

    protected void addAttributes(Map<String, Object> env, Map<String, Object> properties) {
        Map<String, Object> attributes = new TreeMap<>(properties);
        attributes.keySet()
            .retainAll(SupportedParameters.APP_ATTRIBUTES);
        resolveUrlsInAppAttributes(attributes);
        if (!attributes.isEmpty()) {
            env.put(Constants.ENV_DEPLOY_ATTRIBUTES, attributes);
        }
        Boolean checkDeployId = (Boolean) attributes.get(SupportedParameters.CHECK_DEPLOY_ID);
        if (checkDeployId != null && checkDeployId) {
            env.put(Constants.ENV_DEPLOY_ID, deployId);
        }
    }
    
    private void resolveUrlsInAppAttributes(Map<String, Object> properties) {
        String serviceUrl = (String) properties.get(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_URL);
        String serviceBrokerUrl = (String) properties.get(SupportedParameters.SERVICE_BROKER_URL);
        if (serviceUrl != null) {
            properties.put(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_URL, xsPlaceholderResolver.resolve(serviceUrl));
        }
        if (serviceBrokerUrl != null) {
            properties.put(SupportedParameters.SERVICE_BROKER_URL, xsPlaceholderResolver.resolve(serviceBrokerUrl));
        }
    }
    
    protected void addProperties(Map<String, Object> env, Map<String, Object> properties) {
        env.putAll(properties);
    }
    
    protected void addToGroup(Map<String, List<Object>> groups, String group, String name, Map<String, Object> properties) {
        groups.computeIfAbsent(group, key -> new ArrayList<>())
        .add(createExtendedProperties(name, properties));
    }
    
    protected static Map<String, Object> createExtendedProperties(String name, Map<String, Object> properties) {
        Map<String, Object> extendedProperties = new TreeMap<>();
        extendedProperties.put(Constants.ATTR_NAME, name);
        extendedProperties.putAll(properties);
        return extendedProperties;
    }

    protected void addDependencies(Map<String, Object> env, Module module) {
        Map<String, List<Object>> groupsMap = new TreeMap<>();
        for (RequiredDependency requiredDependency : module.getRequiredDependencies2()) {
            addDependency(requiredDependency, env, groupsMap);
        }
        env.putAll(groupsMap);
    }

    protected void addDependency(RequiredDependency dependency, Map<String, Object> env, Map<String, List<Object>> groups) {
        if (dependency.getList() != null) {
            dependency.setGroup(dependency.getList());
        }
        addToGroupsOrEnvironment(env, groups, asList(dependency.getGroup()), dependency.getName(), dependency.getProperties());
    }

    protected void addToGroupsOrEnvironment(Map<String, Object> env, Map<String, List<Object>> groups, List<String> destinationGroups,
        String subgroupName, Map<String, Object> properties) {
        if (!destinationGroups.isEmpty()) {
            addToGroups(groups, destinationGroups, subgroupName, properties);
        } else {
            properties.forEach(env::put);
        }
    }

    protected void addToGroups(Map<String, List<Object>> groups, List<String> destinationGroups, String subgroupName,
        Map<String, Object> properties) {
        for (String group : destinationGroups) {
            addToGroup(groups, group, subgroupName, properties);
        }
    }

    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }
    
    public Map<String, Object> removeSpecialApplicationProperties(Map<String, Object> properties) {
        properties.keySet()
            .removeAll(SupportedParameters.APP_ATTRIBUTES);
        properties.keySet()
            .removeAll(SupportedParameters.APP_PROPS);
        properties.keySet()
            .removeAll(SupportedParameters.SPECIAL_MT_PROPS);
        return properties;
    }

    public Map<String, Object> removeSpecialServiceProperties(Map<String, Object> properties) {
        properties.keySet()
            .removeAll(SupportedParameters.SPECIAL_RT_PROPS);
        properties.keySet()
            .removeAll(SupportedParameters.SERVICE_PROPS);
        return properties;
    }

}
