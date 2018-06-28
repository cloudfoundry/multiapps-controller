package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.cloudfoundry.client.lib.domain.Staging;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.UrisClassifier;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.MemoryParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.StagingParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.TaskParametersParser;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.builders.v1_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class ApplicationsCloudModelBuilder {
    public static final String DEPENDECY_TYPE_SOFT = "soft";
    public static final String DEPENDECY_TYPE_HARD = "hard";

    private static final int MTA_MAJOR_VERSION = 1;

    protected DescriptorHandler handler;
    protected PropertiesChainBuilder propertiesChainBuilder;
    protected PropertiesAccessor propertiesAccessor;
    protected DeploymentDescriptor deploymentDescriptor;

    protected CloudModelConfiguration configuration;
    protected ApplicationUrisCloudModelBuilder urisCloudModelBuilder;
    protected ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder;
    protected CloudServiceNameMapper cloudServiceNameMapper;
    protected XsPlaceholderResolver xsPlaceholderResolver;
    protected DeployedMta deployedMta;
    protected UserMessageLogger userMessageLogger;

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        this(deploymentDescriptor, configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId, null);
    }

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId,
        UserMessageLogger userMessageLogger) {
        this(new DescriptorHandler(), new PropertiesChainBuilder(deploymentDescriptor), deploymentDescriptor, configuration,
            new ApplicationEnvironmentCloudModelBuilder(configuration, deploymentDescriptor, xsPlaceholderResolver, new DescriptorHandler(),
                deployId),
            deployedMta, systemParameters, xsPlaceholderResolver, userMessageLogger);
    }

    public ApplicationsCloudModelBuilder(DescriptorHandler handler, PropertiesChainBuilder propertiesChainBuilder,
        DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver, UserMessageLogger userMessageLogger) {
        this.handler = handler;
        this.propertiesChainBuilder = propertiesChainBuilder;
        this.propertiesAccessor = getHandlerFactory().getPropertiesAccessor();
        this.deploymentDescriptor = deploymentDescriptor;
        this.configuration = configuration;
        this.urisCloudModelBuilder = new ApplicationUrisCloudModelBuilder(configuration.isPortBasedRouting(), systemParameters,
            getHandlerFactory().getPropertiesAccessor());
        this.applicationEnvCloudModelBuilder = applicationEnvCloudModelBuilder;
        this.cloudServiceNameMapper = new CloudServiceNameMapper(configuration, propertiesAccessor, deploymentDescriptor);
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.deployedMta = deployedMta;
        this.userMessageLogger = userMessageLogger;
    }

    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    public List<CloudApplicationExtended> build(Set<String> mtaModulesInArchive, Set<String> allMtaModules, Set<String> deployedModules)
        throws SLException {
        List<CloudApplicationExtended> apps = new ArrayList<>();
        SortedSet<String> unresolvedMtaModules = new TreeSet<>(allMtaModules);
        initializeModulesDependecyTypes(deploymentDescriptor);
        for (Module module : handler.getSortedModules(deploymentDescriptor, SupportedParameters.DEPENDENCY_TYPE, DEPENDECY_TYPE_HARD)) {
            if (!mtaModulesInArchive.contains(module.getName()) || module.getType() == null) {
                if (deployedModules.contains(module.getName())) {
                    printMTAModuleNotFoundWarning(module.getName());
                }
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

    private void printMTAModuleNotFoundWarning(String moduleName) {

        if (userMessageLogger != null) {
            userMessageLogger.warn(Messages.NOT_DESCRIBED_MODULE, moduleName);
        }
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
        return (String) propertiesAccessor.getParameters(module)
            .getOrDefault(SupportedParameters.DEPENDENCY_TYPE, DEPENDECY_TYPE_SOFT);
    }

    protected CloudApplicationExtended getApplication(Module module) throws SLException {
        DeployedMtaModule deployedModule = findDeployedModule(deployedMta, module);
        List<Map<String, Object>> propertiesList = propertiesChainBuilder.buildModuleChain(module.getName());
        Staging staging = parseParameters(propertiesList, new StagingParametersParser());
        int diskQuota = parseParameters(propertiesList, new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0"));
        int memory = parseParameters(propertiesList, new MemoryParametersParser(SupportedParameters.MEMORY, "0"));
        int instances = (Integer) getPropertyValue(propertiesList, SupportedParameters.INSTANCES, 0);
        List<String> uris = urisCloudModelBuilder.getApplicationUris(module, propertiesList);
        List<String> idleUris = urisCloudModelBuilder.getIdleApplicationUris(module, propertiesList);
        List<String> resolvedUris = xsPlaceholderResolver.resolve(uris);
        List<String> resolvedIdleUris = xsPlaceholderResolver.resolve(idleUris);
        List<String> customUris = new UrisClassifier(xsPlaceholderResolver).getCustomUris(deployedModule);
        List<String> fullResolvedUris = ListUtil.merge(resolvedUris, customUris);
        List<String> allServices = getAllApplicationServices(module);
        List<ServiceKeyToInject> serviceKeysToInject = getServicesKeysToInject(module);
        Set<String> specialModuleProperties = buildSpecialModulePropertiesSet();
        Map<String, Object> moduleProperties = propertiesAccessor.getProperties(module, specialModuleProperties);
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters(module, specialModuleProperties);
        Map<Object, Object> env = applicationEnvCloudModelBuilder.build(module, uris, getApplicationServices(module),
            getSharedApplicationServices(module), moduleProperties, moduleParameters);
        List<CloudTask> tasks = getTasks(propertiesList);
        return createCloudApplication(getApplicationName(module), module.getName(), staging, diskQuota, memory, instances, fullResolvedUris,
            resolvedIdleUris, allServices, serviceKeysToInject, env, tasks);
    }

    protected DeployedMtaModule findDeployedModule(DeployedMta deployedMta, Module module) {
        return deployedMta == null ? null : deployedMta.findDeployedModule(module.getName());
    }

    private Set<String> buildSpecialModulePropertiesSet() {
        Set<String> result = new HashSet<>();
        result.addAll(SupportedParameters.APP_PROPS);
        result.addAll(SupportedParameters.APP_ATTRIBUTES);
        result.addAll(SupportedParameters.SPECIAL_MT_PROPS);
        return result;
    }

    protected String getApplicationName(Module module) {
        return (String) propertiesAccessor.getParameters(module)
            .get(SupportedParameters.APP_NAME);
    }

    protected <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    protected List<String> getAllApplicationServices(Module module) {
        return getApplicationServices(module, this::allServicesRule);
    }

    protected List<String> getApplicationServices(Module module) {
        return getApplicationServices(module, this::filterExistingServicesRule);
    }

    protected List<String> getSharedApplicationServices(Module module) {
        return getApplicationServices(module, this::onlySharedServicesRule);
    }

    protected List<String> getApplicationServices(Module module, Predicate<ResourceAndResourceType> filterRule) throws SLException {
        List<String> services = new ArrayList<>();
        for (String dependencyName : module.getRequiredDependencies1_0()) {
            ResourceAndResourceType resourceAndResourceType = getApplicationService(dependencyName);
            if (resourceAndResourceType != null && filterRule.test(resourceAndResourceType)) {
                ListUtil.addNonNull(services, cloudServiceNameMapper.mapServiceName(resourceAndResourceType.getResource(),
                    resourceAndResourceType.getResourceType()));
            }
        }
        return ListUtil.removeDuplicates(services);
    }

    protected boolean allServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return true;
    }

    protected boolean filterExistingServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return !isExistingService(resourceAndResourceType.getResourceType());
    }

    protected boolean onlySharedServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return !isExistingService(resourceAndResourceType.getResourceType()) && isSharedService(resourceAndResourceType.getResource());
    }

    private boolean isExistingService(ResourceType resourceType) {
        return resourceType.equals(ResourceType.EXISTING_SERVICE);
    }

    private boolean isSharedService(Resource resource) {
        return (boolean) propertiesAccessor.getParameters(resource)
            .getOrDefault(SupportedParameters.SHARED, false);
    }

    protected ResourceAndResourceType getApplicationService(String dependencyName) {
        Resource resource = getResource(dependencyName);
        if (resource != null && CloudModelBuilderUtil.isService(resource, propertiesAccessor)) {
            ResourceType serviceType = CloudModelBuilderUtil.getResourceType(resource.getProperties());
            return new ResourceAndResourceType(resource, serviceType);
        }
        return null;
    }

    protected List<ServiceKeyToInject> getServicesKeysToInject(Module module) {
        List<ServiceKeyToInject> serviceKeysToInject = new ArrayList<>();
        for (String dependencyName : module.getRequiredDependencies1_0()) {
            ServiceKeyToInject serviceKeyToInject = getServiceKeyToInject(dependencyName);
            ListUtil.addNonNull(serviceKeysToInject, serviceKeyToInject);
        }
        return serviceKeysToInject;
    }

    protected ServiceKeyToInject getServiceKeyToInject(String dependencyName) {
        Resource resource = getResource(dependencyName);
        if (resource != null && CloudModelBuilderUtil.isServiceKey(resource, propertiesAccessor)) {
            Map<String, Object> resourceParameters = propertiesAccessor.getParameters(resource);
            String serviceName = PropertiesUtil.getRequiredParameter(resourceParameters, SupportedParameters.SERVICE_NAME);
            String serviceKeyName = (String) resourceParameters.getOrDefault(SupportedParameters.SERVICE_KEY_NAME, resource.getName());
            return new ServiceKeyToInject(serviceKeyName, serviceName, serviceKeyName);
        }
        return null;
    }

    protected Resource getResource(String dependencyName) {
        return handler.findDependency(deploymentDescriptor, dependencyName)._1;
    }

    protected List<CloudTask> getTasks(List<Map<String, Object>> propertiesList) {
        return parseParameters(propertiesList, new TaskParametersParser(SupportedParameters.TASKS, configuration.isPrettyPrinting()));
    }

    protected static CloudApplicationExtended createCloudApplication(String name, String moduleName, Staging staging, int diskQuota,
        int memory, int instances, List<String> uris, List<String> idleUris, List<String> services,
        List<ServiceKeyToInject> serviceKeysToInject, Map<Object, Object> env, List<CloudTask> tasks) {
        CloudApplicationExtended app = new CloudApplicationExtended(null, name);
        app.setModuleName(moduleName);
        app.setStaging(staging);
        app.setDiskQuota(diskQuota);
        app.setMemory(memory);
        app.setInstances(instances);
        app.setUris(uris);
        app.setIdleUris(idleUris);
        app.setServices(services);
        app.setServiceKeysToInject(serviceKeysToInject);
        app.setEnv(env);
        app.setTasks(tasks);
        return app;
    }

}
