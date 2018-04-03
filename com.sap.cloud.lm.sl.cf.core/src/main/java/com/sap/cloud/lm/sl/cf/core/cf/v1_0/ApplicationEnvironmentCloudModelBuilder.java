package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertiesList;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.mergeProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MapToEnvironmentConverter;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;

public class ApplicationEnvironmentCloudModelBuilder {
    protected CloudModelConfiguration configuration;
    protected DeploymentDescriptor deploymentDescriptor;
    protected XsPlaceholderResolver xsPlaceholderResolver;
    protected DescriptorHandler handler;
    protected String deployId;

    private static final int MTA_MAJOR_VERSION = 1;

    public ApplicationEnvironmentCloudModelBuilder(CloudModelConfiguration configuration, DeploymentDescriptor deploymentDescriptor,
        XsPlaceholderResolver xsPlaceholderResolver, DescriptorHandler handler, String deployId) {
        this.configuration = configuration;
        this.deploymentDescriptor = deploymentDescriptor;
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.handler = handler;
        this.deployId = deployId;
    }

    public Map<Object, Object> build(Module module, List<String> descriptorDefinedUris, List<String> applicationServices,
        Map<String, Object> properties, Map<String, Object> parameters) throws ContentException {
        Map<String, Object> env = new TreeMap<>();
        addMetadata(env, module);
        addServices(env, applicationServices);
        addAttributes(env, descriptorDefinedUris, parameters);
        addProperties(env, properties);
        addDependencies(env, module);
        return MapUtil.unmodifiable(new MapToEnvironmentConverter(configuration.isPrettyPrinting()).asEnv(env));
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
        List<String> mtaModuleProvidedDependencies = module.getProvidedDependencies1_0()
            .stream()
            .filter(providedDependency -> CloudModelBuilderUtil.isPublic(providedDependency))
            .map(providedDependency -> providedDependency.getName())
            .collect(Collectors.toList());
        env.put(Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES, mtaModuleProvidedDependencies);
    }

    protected void addServices(Map<String, Object> env, List<String> services) {
        env.put(Constants.ENV_MTA_SERVICES, services);
    }

    protected void addAttributes(Map<String, Object> env, List<String> descriptorDefinedUris, Map<String, Object> properties) {
        Map<String, Object> attributes = new TreeMap<>(properties);
        attributes.keySet()
            .retainAll(SupportedParameters.APP_ATTRIBUTES);
        resolveUrlsInAppAttributes(attributes);
        addDescriptorDefinedUris(attributes, descriptorDefinedUris);
        if (!attributes.isEmpty()) {
            env.put(Constants.ENV_DEPLOY_ATTRIBUTES, attributes);
        }
        Boolean checkDeployId = (Boolean) attributes.get(SupportedParameters.CHECK_DEPLOY_ID);
        if (checkDeployId != null && checkDeployId == true) {
            env.put(Constants.ENV_DEPLOY_ID, deployId);
        }
    }

    private void addDescriptorDefinedUris(Map<String, Object> attributes, List<String> descriptorDefinedUris) {
        Set<String> sortedDescriptorDefinedUris = new TreeSet<>(descriptorDefinedUris);
        attributes.put(Constants.ATTR_DESCRIPTOR_DEFINED_URIS, sortedDescriptorDefinedUris);
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

    protected void addDependencies(Map<String, Object> env, Module module) {
        Map<String, List<Object>> groupsMap = new TreeMap<>();
        for (String dependency : module.getRequiredDependencies1_0()) {
            addDependency(dependency, module, env, groupsMap);
        }
        env.putAll(groupsMap);
    }

    protected void addDependency(String dependency, Module module, Map<String, Object> env, Map<String, List<Object>> groupsMap) {
        Pair<Resource, ProvidedDependency> pair = handler.findDependency(deploymentDescriptor, dependency);
        addProvidedDependency(env, groupsMap, pair._2);
        addResource(env, groupsMap, pair._1);
    }

    protected void addProvidedDependency(Map<String, Object> env, Map<String, List<Object>> groupsMap,
        ProvidedDependency providedDependency) {
        if (providedDependency != null) {
            addToGroupsOrEnvironment(env, groupsMap, providedDependency.getGroups(), providedDependency.getName(),
                gatherProperties(providedDependency));
        }
    }

    protected Map<String, Object> gatherProperties(ProvidedDependency providedDependency) {
        return removeSpecialApplicationProperties(mergeProperties(getPropertiesList(providedDependency)));
    }

    protected void addResource(Map<String, Object> env, Map<String, List<Object>> groups, Resource resource) {
        if (resource != null && !CloudModelBuilderUtil.isService(resource, getHandlerFactory().getPropertiesAccessor())) {
            addToGroupsOrEnvironment(env, groups, resource.getGroups(), resource.getName(), gatherProperties(resource));
        }
    }

    protected Map<String, Object> gatherProperties(Resource resource) {
        return removeSpecialApplicationProperties(mergeProperties(getPropertiesList(resource)));
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

    protected void addToGroupsOrEnvironment(Map<String, Object> env, Map<String, List<Object>> groups, List<String> destinationGroups,
        String subgroupName, Map<String, Object> properties) {
        if (!destinationGroups.isEmpty()) {
            addToGroups(groups, destinationGroups, subgroupName, properties);
        } else {
            properties.forEach((key, value) -> env.put(key, value));
        }
    }

    protected void addToGroups(Map<String, List<Object>> groups, List<String> destinationGroups, String subgroupName,
        Map<String, Object> properties) {
        for (String group : destinationGroups) {
            addToGroup(groups, group, subgroupName, properties);
        }
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

    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

}
