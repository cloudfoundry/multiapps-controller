package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.NameUtil.ensureValidEnvName;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
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

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        this(new DescriptorHandler(), new PropertiesChainBuilder(deploymentDescriptor), deploymentDescriptor, configuration,
            new ApplicationEnvironmentCloudModelBuilder(configuration, deploymentDescriptor, xsPlaceholderResolver, new DescriptorHandler(),
                deployId),
            deployedMta, systemParameters, xsPlaceholderResolver);
    }

    public ApplicationsCloudModelBuilder(DescriptorHandler handler, PropertiesChainBuilder propertiesChainBuilder,
        DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
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
        List<String> services = getApplicationServices(module, true);
        List<ServiceKeyToInject> serviceKeysToInject = getServicesKeysToInject(module);
        Set<String> specialModuleProperties = buildSpecialModulePropertiesSet();
        Map<String, Object> moduleProperties = propertiesAccessor.getProperties(module, specialModuleProperties);
        Map<String, Object> moduleParameters = propertiesAccessor.getParameters(module, specialModuleProperties);
        Map<Object, Object> env = applicationEnvCloudModelBuilder.build(module, uris, getApplicationServices(module, false),
            moduleProperties, moduleParameters);
        List<CloudTask> tasks = getTasks(propertiesList);
        return createCloudApplication(getApplicationName(module), module.getName(), staging, diskQuota, memory, instances, fullResolvedUris,
            resolvedIdleUris, services, serviceKeysToInject, env, tasks);
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
        return (String) propertiesAccessor.getParameters(module).get(SupportedParameters.APP_NAME);
    }

    protected <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    protected List<String> getApplicationServices(Module module, boolean addExisting) throws SLException {
        List<String> services = new ArrayList<>();
        for (String dependencyName : module.getRequiredDependencies1_0()) {
            Pair<Resource, ResourceType> pair = getApplicationService(dependencyName);
            if (pair != null && shouldAddServiceToList(pair._2, addExisting)) {
                ListUtil.addNonNull(services, cloudServiceNameMapper.mapServiceName(pair._1, pair._2));
            }
        }
        return ListUtil.removeDuplicates(services);
    }

    protected boolean shouldAddServiceToList(ResourceType serviceType, boolean addExisting) {
        return !serviceType.equals(ResourceType.EXISTING_SERVICE) || addExisting;
    }

    protected Pair<Resource, ResourceType> getApplicationService(String dependencyName) {
        Resource resource = getResource(dependencyName);
        if (resource != null && CloudModelBuilderUtil.isService(resource)) {
            ResourceType serviceType = CloudModelBuilderUtil.getServiceType(resource.getProperties());
            return new Pair<>(resource, serviceType);
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
        if (resource != null && CloudModelBuilderUtil.isServiceKey(resource)) {
            Map<String, Object> resourceParameters = propertiesAccessor.getParameters(resource);
            String serviceName = PropertiesUtil.getRequiredParameter(resourceParameters, SupportedParameters.SERVICE_KEY_SERVICE_NAME);
            String serviceKeyName = (String) resourceParameters.getOrDefault(SupportedParameters.SERVICE_KEY_NAME, resource.getName());
            ensureValidEnvName(serviceKeyName, configuration.shouldAllowInvalidEnvNames());
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
