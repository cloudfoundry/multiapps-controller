package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.isValidName;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getAll;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertiesList;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.mergeProperties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyImpl;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.ObjectToEnvironmentValueConverter;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.MemoryParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.StagingParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.TempUriParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.UriParametersParser;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil.NameRequirements;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.builders.v1_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class CloudModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudModelBuilder.class);

    public enum ServiceType {
        MANAGED("managed-service"), USER_PROVIDED("user-provided-service"), EXISTING("existing-service");

        private String value;

        ServiceType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static ServiceType get(String value) {
            for (ServiceType v : values()) {
                if (v.value.equals(value))
                    return v;
            }
            throw new IllegalArgumentException();
        }
    }

    // Metadata attributes
    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_PROVIDER = "provider";
    public static final String ATTR_COPYRIGHT = "copyright";

    // Metadata environment variables
    public static final String ENV_MTA_METADATA = "MTA_METADATA";
    public static final String ENV_MTA_MODULE_METADATA = "MTA_MODULE_METADATA";
    public static final String ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES = "MTA_MODULE_PROVIDED_DEPENDENCIES";
    public static final String ENV_MTA_PROPERTIES = "MTA_PROPERTIES";
    public static final String ENV_MTA_SERVICES = "MTA_SERVICES";
    public static final String ENV_DEPLOY_ATTRIBUTES = "DEPLOY_ATTRIBUTES";
    public static final String ENV_DEPLOY_ID = "DEPLOY_ID";

    public static final String DEPENDECY_TYPE_SOFT = "soft";
    public static final String DEPENDECY_TYPE_HARD = "hard";

    private static final int MTA_MAJOR_VERSION = 1;

    protected final DeploymentDescriptor deploymentDescriptor;
    protected final SystemParameters systemParameters;
    protected final boolean portBasedRouting;
    protected final boolean prettyPrinting;
    protected final boolean useNamespaces;
    protected final boolean useNamespacesForServices;
    protected final boolean allowInvalidEnvNames;
    protected final String deployId;
    protected final XsPlaceholderResolver xsPlaceholderResolver;

    protected final DescriptorHandler handler;
    protected final PropertiesChainBuilder propertiesChainBuilder;
    protected final PropertiesAccessor propertiesAccessor;

    public CloudModelBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters, boolean portBasedRouting,
        boolean prettyPrinting, boolean useNamespaces, boolean useNamespacesForServices, boolean allowInvalidEnvNames, String deployId,
        XsPlaceholderResolver xsPlaceholderResolver) {
        this(deploymentDescriptor, systemParameters, portBasedRouting, prettyPrinting, useNamespaces, useNamespacesForServices,
            allowInvalidEnvNames, deployId, new DescriptorHandler(), new PropertiesChainBuilder(deploymentDescriptor),
            xsPlaceholderResolver);
    }

    protected CloudModelBuilder(DeploymentDescriptor deploymentDescriptor, SystemParameters systemParameters, boolean portBasedRouting,
        boolean prettyPrinting, boolean useNamespaces, boolean useNamespacesForServices, boolean allowInvalidEnvNames, String deployId,
        DescriptorHandler handler, PropertiesChainBuilder propertiesChainBuilder, XsPlaceholderResolver xsPlaceholderResolver) {
        this.deploymentDescriptor = deploymentDescriptor;
        this.systemParameters = systemParameters;
        this.portBasedRouting = portBasedRouting;
        this.prettyPrinting = prettyPrinting;
        this.useNamespaces = useNamespaces;
        this.useNamespacesForServices = useNamespacesForServices;
        this.allowInvalidEnvNames = allowInvalidEnvNames;
        this.deployId = deployId;
        this.handler = handler;
        this.propertiesChainBuilder = propertiesChainBuilder;
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.propertiesAccessor = getHandlerFactory().getPropertiesAccessor();
    }

    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    public List<String> getCustomDomains() {
        Set<String> domains = new TreeSet<>();
        String defaultDomain = (String) systemParameters.getGeneralParameters().getOrDefault(SupportedParameters.DEFAULT_DOMAIN, null);
        for (Module module : deploymentDescriptor.getModules1_0()) {
            domains.addAll(getDomains(module));
        }
        if (xsPlaceholderResolver.getDefaultDomain() != null) {
            domains.remove(xsPlaceholderResolver.getDefaultDomain());
        }
        if (defaultDomain != null) {
            domains.remove(defaultDomain);
        }
        return new ArrayList<>(domains);
    }

    protected List<String> getDomains(Module module) {
        List<Map<String, Object>> propertiesList = propertiesChainBuilder.buildModuleChain(module.getName());
        return getAll(propertiesList, SupportedParameters.DOMAIN, SupportedParameters.DOMAINS);
    }

    public List<CloudServiceExtended> getServices(Set<String> modules) throws SLException {
        List<CloudServiceExtended> services = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource)) {
                ListUtil.addNonNull(services, getService(resource));
            }
        }
        return services;
    }

    public Map<String, List<ServiceKey>> getServiceKeys() throws ContentException {
        Map<String, List<ServiceKey>> serviceKeys = new HashMap<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource)) {
                serviceKeys.put(resource.getName(), getServiceKeysForService(resource));
            }
        }
        return serviceKeys;
    }

    protected boolean isService(Resource resource) {
        return resource.getType() != null; // Typed resource = service;
    }

    public List<CloudApplicationExtended> getApplications(Set<String> mtaModulesInArchive, Set<String> allMtaModules,
        Set<String> deployedModules) throws SLException {
        List<CloudApplicationExtended> apps = new ArrayList<>();
        SortedSet<String> unresolvedMtaModules = new TreeSet<>(allMtaModules);
        initializeModulesDependecyTypes(deploymentDescriptor);
        for (Module module : handler.getSortedModules(deploymentDescriptor, SupportedParameters.DEPENDENCY_TYPE, DEPENDECY_TYPE_HARD)) {
            if (!mtaModulesInArchive.contains(module.getName()) || module.getType() == null) {
                continue;
            }
            if (allMtaModules.contains(module.getName())) {
                ListUtil.addNonNull(apps, getApplication(module));
                unresolvedMtaModules.remove(module.getName());
            } else {
                throw new ContentException(Messages.ARCHIVE_MODULE_NOT_INTENDED_FOR_DEPLOYMENT, module.getName());
            }
        }
        unresolvedMtaModules.removeAll(deployedModules);
        if (!unresolvedMtaModules.isEmpty()) {
            throw new ContentException(Messages.UNRESOLVED_MTA_MODULES, unresolvedMtaModules);
        }
        return apps;
    }

    protected void initializeModulesDependecyTypes(DeploymentDescriptor deploymentDescriptor) {
        for (Module module : deploymentDescriptor.getModules1_0()) {
            String dependencyType = getDependencyType(module);
            Map<String, Object> moduleProperties = propertiesAccessor.getParameters(module);
            moduleProperties.put(SupportedParameters.DEPENDENCY_TYPE, dependencyType);
            propertiesAccessor.setParameters(module, moduleProperties);
        }
    }

    protected String getDependencyType(Module module) {
        return (String) propertiesAccessor.getParameters(module).getOrDefault(SupportedParameters.DEPENDENCY_TYPE, DEPENDECY_TYPE_SOFT);
    }

    protected CloudServiceExtended getService(Resource resource) throws SLException {
        Map<String, Object> parameters = propertiesAccessor.getParameters(resource);
        ServiceType serviceType = getServiceType(parameters);
        CloudServiceExtended service = createService(parameters, getServiceName(resource, serviceType), serviceType);
        if (service != null) {
            service.setResourceName(resource.getName());
        }
        return service;
    }

    protected ServiceType getServiceType(Map<String, Object> properties) {
        String type = (String) properties.getOrDefault(SupportedParameters.TYPE, ServiceType.MANAGED.toString());
        return ServiceType.get(type);
    }

    protected CloudServiceExtended createService(Map<String, Object> properties, String serviceName, ServiceType serviceType)
        throws ContentException {
        if (serviceType.equals(ServiceType.MANAGED)) {
            return createManagedService(serviceName, properties);
        } else if (serviceType.equals(ServiceType.USER_PROVIDED)) {
            return createUserProvidedService(serviceName, properties);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceExtended createManagedService(String serviceName, Map<String, Object> properties) throws ContentException {
        String label = (String) properties.get(SupportedParameters.SERVICE);
        String plan = (String) properties.get(SupportedParameters.SERVICE_PLAN);
        String provider = (String) properties.get(SupportedParameters.SERVICE_PROVIDER);
        String version = (String) properties.get(SupportedParameters.SERVICE_VERSION);
        List<String> serviceTags = (List<String>) properties.getOrDefault(SupportedParameters.SERVICE_TAGS, Collections.emptyList());
        Map<String, Object> credentials = getServiceConfigParameters(properties, serviceName);

        return createCloudService(serviceName, label, plan, provider, version, credentials, serviceTags);
    }

    protected CloudServiceExtended createUserProvidedService(String serviceName, Map<String, Object> properties) throws ContentException {
        Map<String, Object> credentials = getServiceConfigParameters(properties, serviceName);
        String label = (String) properties.get(SupportedParameters.SERVICE);
        if (label != null) {
            LOGGER.warn(MessageFormat.format(Messages.IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE, label, serviceName));
        }
        return createCloudService(serviceName, null, null, null, null, credentials, Collections.emptyList());
    }

    protected CloudApplicationExtended getApplication(Module module) throws SLException {
        List<Map<String, Object>> propertiesList = propertiesChainBuilder.buildModuleChain(module.getName());
        Staging staging = parseParameters(propertiesList, new StagingParametersParser());
        int diskQuota = parseParameters(propertiesList, new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0"));
        int memory = parseParameters(propertiesList, new MemoryParametersParser(SupportedParameters.MEMORY, "0"));
        int instances = (Integer) getPropertyValue(propertiesList, SupportedParameters.INSTANCES, 0);
        List<String> uris = getApplicationUris(module, propertiesList);
        List<String> tempUris = getTempApplicationUris(module, propertiesList);
        List<String> services = getApplicationServices(module, true);
        Set<String> specialModuleProperties = buildSpecialModulePropertiesSet();
        Map<String, Object> moduleProperties = propertiesAccessor.getProperties(module, specialModuleProperties);
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters(module, specialModuleProperties);
        Map<Object, Object> env = getApplicationEnv(module, moduleProperties, moduleParameters, getApplicationServices(module, false));
        return createCloudApplication(getApplicationName(module), module.getName(), staging, diskQuota, memory, instances, uris, tempUris,
            services, env);
    }

    private Set<String> buildSpecialModulePropertiesSet() {
        Set<String> result = new HashSet<>();
        result.addAll(SupportedParameters.APP_PROPS);
        result.addAll(SupportedParameters.APP_ATTRIBUTES);
        result.addAll(SupportedParameters.SPECIAL_MT_PROPS);
        return result;
    }

    protected List<String> getApplicationUris(Module module, List<Map<String, Object>> propertiesList) {
        String defaultHost = (String) systemParameters.getModuleParameters().getOrDefault(module.getName(),
            Collections.emptyMap()).getOrDefault(SupportedParameters.DEFAULT_HOST, null);
        int defaultPort = (Integer) systemParameters.getModuleParameters().getOrDefault(module.getName(),
            Collections.emptyMap()).getOrDefault(SupportedParameters.DEFAULT_PORT, 0);
        String routePath = (String) propertiesAccessor.getParameters(module).getOrDefault(SupportedParameters.ROUTE_PATH, null);
        List<String> uris = parseParameters(propertiesList,
            new UriParametersParser(portBasedRouting, defaultHost, xsPlaceholderResolver.getDefaultDomain(), defaultPort, routePath));

        return replaceXsaPlaceholders(uris);
    }

    protected List<String> replaceXsaPlaceholders(List<String> uris) {
        return uris.stream().map((uri) -> xsPlaceholderResolver.resolve(uri)).collect(Collectors.toList());
    }

    protected List<String> getTempApplicationUris(Module module, List<Map<String, Object>> propertiesList) {
        String defaultTempDomain = null;
        if (systemParameters.getGeneralParameters().containsKey(SupportedParameters.DEFAULT_TEMP_DOMAIN)) {
            defaultTempDomain = xsPlaceholderResolver.getDefaultDomain();
        }
        String defaultTempHost = (String) systemParameters.getModuleParameters().getOrDefault(module.getName(),
            Collections.emptyMap()).getOrDefault(SupportedParameters.DEFAULT_TEMP_HOST, null);
        int defaultTempPort = (Integer) systemParameters.getModuleParameters().getOrDefault(module.getName(),
            Collections.emptyMap()).getOrDefault(SupportedParameters.DEFAULT_TEMP_PORT, 0);
        String routePath = (String) propertiesAccessor.getParameters(module).getOrDefault(SupportedParameters.ROUTE_PATH, null);
        List<String> uris = parseParameters(propertiesList,
            new TempUriParametersParser(portBasedRouting, defaultTempHost, defaultTempDomain, defaultTempPort, routePath));
        return replaceXsaPlaceholders(uris);
    }

    protected String getApplicationName(Module module) {
        return (String) propertiesAccessor.getParameters(module).get(SupportedParameters.APP_NAME);
    }

    protected List<String> getApplicationServices(Module module, boolean addExisting) throws SLException {
        List<String> services = new ArrayList<>();
        for (String dependencyName : module.getRequiredDependencies1_0()) {
            Pair<Resource, ServiceType> pair = getApplicationService(dependencyName);
            if (pair != null && shouldAddServiceToList(pair._2, addExisting)) {
                ListUtil.addNonNull(services, getServiceName(pair._1, pair._2));
            }
        }
        return services;
    }

    protected Pair<Resource, ServiceType> getApplicationService(String dependencyName) {
        Resource resource = getResource(dependencyName);
        if (resource != null && isService(resource)) {
            ServiceType serviceType = getServiceType(resource.getProperties());
            return new Pair<>(resource, serviceType);
        }
        return null;
    }

    protected Resource getResource(String dependencyName) {
        return handler.findDependency(deploymentDescriptor, dependencyName)._1;
    }

    protected boolean shouldAddServiceToList(ServiceType serviceType, boolean addExisting) {
        return !serviceType.equals(ServiceType.EXISTING) || addExisting;
    }

    protected String getServiceName(Resource resource, ServiceType serviceType) throws SLException {
        Map<String, Object> parameters = propertiesAccessor.getParameters(resource);
        String overwritingName = (String) parameters.get(SupportedParameters.SERVICE_NAME);

        String shortServiceName = overwritingName != null ? overwritingName : resource.getName();
        if (serviceType.equals(ServiceType.EXISTING)) {
            return shortServiceName;
        }
        return getServiceName(shortServiceName);
    }

    protected String getServiceName(String name) throws SLException {
        return NameUtil.getServiceName(name, deploymentDescriptor.getId(), useNamespaces, useNamespacesForServices);
    }

    protected Map<Object, Object> getApplicationEnv(Module module, Map<String, Object> properties, Map<String, Object> parameters,
        List<String> services) throws ContentException {
        Map<String, Object> env = new TreeMap<>();
        addMetadata(env, module);
        addServices(env, services);
        addAttributes(env, new TreeMap<>(parameters));
        addProperties(env, new TreeMap<>(properties));
        addDependencies(env, module);
        return asEnv(removeInvalidEnvNames(env));
    }

    protected void addMetadata(Map<String, Object> env, Module module) {
        addMtaMetadata(env);
        addMtaModuleMetadata(env, module);
        addProvidedDependenciesMetadata(env, module);
    }

    protected void addMtaMetadata(Map<String, Object> env) {
        Map<String, Object> mtaMetadata = new TreeMap<>();
        MapUtil.addNonNull(mtaMetadata, ATTR_ID, deploymentDescriptor.getId());
        MapUtil.addNonNull(mtaMetadata, ATTR_VERSION, deploymentDescriptor.getVersion());
        MapUtil.addNonNull(mtaMetadata, ATTR_DESCRIPTION, deploymentDescriptor.getDescription());
        MapUtil.addNonNull(mtaMetadata, ATTR_PROVIDER, deploymentDescriptor.getProvider());
        MapUtil.addNonNull(mtaMetadata, ATTR_COPYRIGHT, deploymentDescriptor.getCopyright());
        env.put(ENV_MTA_METADATA, mtaMetadata);
    }

    protected void addMtaModuleMetadata(Map<String, Object> env, Module module) {
        Map<String, Object> mtaModuleMetadata = new TreeMap<>();
        MapUtil.addNonNull(mtaModuleMetadata, ATTR_NAME, module.getName());
        MapUtil.addNonNull(mtaModuleMetadata, ATTR_DESCRIPTION, module.getDescription());
        env.put(ENV_MTA_MODULE_METADATA, mtaModuleMetadata);
    }

    protected void addProvidedDependenciesMetadata(Map<String, Object> env, Module module) {
        List<String> mtaModuleProvidedDependencies = new ArrayList<>();
        for (ProvidedDependency providedDependency : module.getProvidedDependencies1_0()) {
            if (CloudModelBuilderUtil.isPublic(providedDependency)) {
                mtaModuleProvidedDependencies.add(providedDependency.getName());
            }
        }
        env.put(ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES, mtaModuleProvidedDependencies);
    }

    protected void addServices(Map<String, Object> env, List<String> services) {
        env.put(ENV_MTA_SERVICES, services);
    }

    protected void addAttributes(Map<String, Object> env, Map<String, Object> properties) {
        properties.keySet().retainAll(SupportedParameters.APP_ATTRIBUTES);
        resolveUrlsInAppAttributes(properties);
        if (!properties.isEmpty()) {
            env.put(ENV_DEPLOY_ATTRIBUTES, properties);
        }
        Boolean checkDeployId = (Boolean) properties.get(SupportedParameters.CHECK_DEPLOY_ID);
        if (checkDeployId != null && checkDeployId == true) {
            env.put(ENV_DEPLOY_ID, deployId);
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
        if (resource != null && !isService(resource)) {
            addToGroupsOrEnvironment(env, groups, resource.getGroups(), resource.getName(), gatherProperties(resource));
        }
    }

    protected Map<String, Object> gatherProperties(Resource resource) {
        return removeSpecialApplicationProperties(mergeProperties(getPropertiesList(resource)));
    }

    protected static Map<String, Object> removeSpecialApplicationProperties(Map<String, Object> properties) {
        properties.keySet().removeAll(SupportedParameters.APP_ATTRIBUTES);
        properties.keySet().removeAll(SupportedParameters.APP_PROPS);
        properties.keySet().removeAll(SupportedParameters.SPECIAL_MT_PROPS);
        return properties;
    }

    protected static Map<String, Object> removeSpecialServiceProperties(Map<String, Object> properties) {
        properties.keySet().removeAll(SupportedParameters.SPECIAL_RT_PROPS);
        properties.keySet().removeAll(SupportedParameters.SERVICE_PROPS);
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
        groups.computeIfAbsent(group, key -> new ArrayList<>()).add(createExtendedProperties(name, properties));
    }

    protected static Map<String, Object> createExtendedProperties(String name, Map<String, Object> properties) {
        Map<String, Object> extendedProperties = new TreeMap<>();
        extendedProperties.put(ATTR_NAME, name);
        extendedProperties.putAll(properties);
        return extendedProperties;
    }

    protected Map<String, Object> removeInvalidEnvNames(Map<String, Object> env) throws ContentException {
        Map<String, Object> result = new TreeMap<>();
        Map<String, Object> properties = new TreeMap<>();
        for (String key : env.keySet()) {
            if (isValidName(key, NameRequirements.ENVIRONMENT_NAME_PATTERN)) {
                result.put(key, env.get(key));
            } else if (allowInvalidEnvNames) {
                properties.put(key, env.get(key));
            } else {
                throw new ContentException(Messages.INVALID_ENVIRONMENT_VARIABLE_NAME, key);
            }
        }
        if (!properties.isEmpty()) {
            result.put(ENV_MTA_PROPERTIES, properties);
        }
        return result;
    }

    protected Map<Object, Object> asEnv(Map<String, Object> env) {
        ObjectToEnvironmentValueConverter transformer = new ObjectToEnvironmentValueConverter(prettyPrinting);
        Map<Object, Object> result = new TreeMap<>();
        for (String key : env.keySet()) {
            Object v = env.get(key);
            String s = transformer.convert(v);
            result.put(key, s);
        }
        return result;
    }

    protected static CloudServiceExtended createCloudService(String name, String label, String plan, String provider, String version,
        Map<String, Object> credentials, List<String> serviceTags) {
        CloudServiceExtended service = new CloudServiceExtended(null, name);
        service.setLabel(label);
        service.setPlan(plan);
        service.setProvider(provider);
        service.setVersion(version);
        service.setCredentials(credentials);
        service.setTags(serviceTags);

        return service;
    }

    protected static CloudApplicationExtended createCloudApplication(String name, String moduleName, Staging staging, int diskQuota,
        int memory, int instances, List<String> uris, List<String> tempUris, List<String> services, Map<Object, Object> env) {
        CloudApplicationExtended app = new CloudApplicationExtended(null, name);
        app.setModuleName(moduleName);
        app.setStaging(staging);
        app.setDiskQuota(diskQuota);
        app.setMemory(memory);
        app.setInstances(instances);
        app.setUris(uris);
        app.setTempUris(tempUris);
        app.setServices(services);
        app.setEnv(env);
        return app;
    }

    protected <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getServiceConfigParameters(Map<String, Object> properties, String serviceName) throws ContentException {
        Object serviceConfig = properties.get(SupportedParameters.SERVICE_CONFIG);
        if (serviceConfig == null) {
            return Collections.emptyMap();
        }
        if (!(serviceConfig instanceof Map)) {
            throw new ContentException(getInvalidServiceConfigTypeErrorMessage(serviceName, serviceConfig));
        }
        return new TreeMap<>((Map<String, Object>) serviceConfig);
    }

    protected String getInvalidServiceConfigTypeErrorMessage(String serviceName, Object serviceConfig) {
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
            ValidatorUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_CONFIG), Map.class.getSimpleName(),
            serviceConfig.getClass().getSimpleName());
    }

    protected List<ServiceKey> getServiceKeysForService(Resource resource) throws ContentException {
        List<Map<String, Object>> serviceKeysMaps = getServiceKeysMaps(resource);
        return serviceKeysMaps.stream().map(keysMap -> getServiceKey(resource, keysMap)).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected ServiceKeyImpl getServiceKey(Resource resource, Map<String, Object> serviceKeyMap) {
        String serviceKeyName = (String) serviceKeyMap.get(SupportedParameters.SERVICE_KEY_NAME);
        Map<String, Object> parameters = (Map<String, Object>) serviceKeyMap.get(SupportedParameters.SERVICE_KEY_CONFIG);
        if (parameters == null) {
            parameters = Collections.emptyMap();
        }
        return new ServiceKeyImpl(serviceKeyName, parameters, Collections.emptyMap(), null,
            new CloudServiceExtended(null, resource.getName()));
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getServiceKeysMaps(Resource resource) throws ContentException {
        Object serviceKeys = propertiesAccessor.getParameters(resource).get(SupportedParameters.SERVICE_KEYS);
        if (serviceKeys == null) {
            return Collections.emptyList();
        }
        if (!(serviceKeys instanceof List)) {
            throw new ContentException(getInvalidServiceKeysErrorMessage(resource.getName(), serviceKeys));
        }
        return (List<Map<String, Object>>) serviceKeys;
    }

    protected String getInvalidServiceKeysErrorMessage(String serviceName, Object serviceConfig) {
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
            ValidatorUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_KEYS), Map.class.getSimpleName(),
            serviceConfig.getClass().getSimpleName());
    }

}
