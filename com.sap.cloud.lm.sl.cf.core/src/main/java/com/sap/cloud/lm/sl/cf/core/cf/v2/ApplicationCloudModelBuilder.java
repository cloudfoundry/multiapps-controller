package com.sap.cloud.lm.sl.cf.core.cf.v2;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.mergeProperties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort.ApplicationPortType;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.DockerInfoParser;
import com.sap.cloud.lm.sl.cf.core.parser.MemoryParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.RestartParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.StagingParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.TaskParametersParser;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ApplicationCloudModelBuilder {

    public static final String DEPENDENCY_TYPE_SOFT = "soft";
    public static final String DEPENDENCY_TYPE_HARD = "hard";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCloudModelBuilder.class);

    private static final int MTA_MAJOR_VERSION = 2;

    protected DescriptorHandler handler;
    protected DeploymentDescriptor deploymentDescriptor;

    protected CloudModelConfiguration configuration;
    protected ApplicationUrisCloudModelBuilder urisCloudModelBuilder;
    protected ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder;
    protected CloudServiceNameMapper cloudServiceNameMapper;
    protected XsPlaceholderResolver xsPlaceholderResolver;
    protected DeployedMta deployedMta;
    protected UserMessageLogger stepLogger;

    protected ParametersChainBuilder parametersChainBuilder;

    public ApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId, UserMessageLogger stepLogger) {
        HandlerFactory handlerFactory = createHandlerFactory();
        this.handler = handlerFactory.getDescriptorHandler();
        this.deploymentDescriptor = deploymentDescriptor;
        this.configuration = configuration;
        this.urisCloudModelBuilder = new ApplicationUrisCloudModelBuilder(configuration.isPortBasedRouting(), systemParameters);
        this.applicationEnvCloudModelBuilder = new ApplicationEnvironmentCloudModelBuilder(configuration, deploymentDescriptor,
            xsPlaceholderResolver, deployId);
        this.cloudServiceNameMapper = new CloudServiceNameMapper(configuration, deploymentDescriptor);
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.deployedMta = deployedMta;
        this.parametersChainBuilder = new ParametersChainBuilder(deploymentDescriptor);
        this.stepLogger = stepLogger;
    }

    protected HandlerFactory createHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    public CloudApplicationExtended build(Module moduleToDeploy, ModuleToDeployHelper moduleToDeployHelper) {
        if (isApplication(moduleToDeploy, moduleToDeployHelper)) {
            return getApplication(moduleToDeploy);
        }
        return null;
    }

    private boolean isApplication(Module moduleToDeploy, ModuleToDeployHelper moduleToDeployHelper) {
        return moduleToDeployHelper.isApplication(moduleToDeploy);
    }

    protected CloudApplicationExtended getApplication(Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        warnAboutUnsupportedParameters(parametersList);
        Staging staging = parseParameters(parametersList, new StagingParametersParser());
        int diskQuota = parseParameters(parametersList, new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0"));
        int memory = parseParameters(parametersList, new MemoryParametersParser(SupportedParameters.MEMORY, "0"));
        int instances = (Integer) getPropertyValue(parametersList, SupportedParameters.INSTANCES, 0);
        DockerInfo dockerInfo = parseParameters(parametersList, new DockerInfoParser());
        List<String> uris = getApplicationUris(module);
        List<String> idleUris = urisCloudModelBuilder.getIdleApplicationUris(module, parametersList);
        List<String> resolvedUris = xsPlaceholderResolver.resolve(uris);
        List<String> resolvedIdleUris = xsPlaceholderResolver.resolve(idleUris);
        List<String> services = getAllApplicationServices(module);
        List<ServiceKeyToInject> serviceKeys = getServicesKeysToInject(module);
        Map<Object, Object> env = applicationEnvCloudModelBuilder.build(module, getApplicationServices(module));
        List<CloudTask> tasks = getTasks(parametersList);
        Map<String, Map<String, Object>> bindingParameters = getBindingParameters(module);
        List<ApplicationPort> applicationPorts = getApplicationPorts(module, parametersList);
        List<String> applicationDomains = getApplicationDomains(module);
        RestartParameters restartParameters = parseParameters(parametersList, new RestartParametersParser());
        return createCloudApplication(getApplicationName(module), module.getName(), staging, diskQuota, memory, instances, resolvedUris,
            resolvedIdleUris, services, serviceKeys, env, bindingParameters, tasks, applicationPorts, applicationDomains, restartParameters,
            dockerInfo);
    }

    public List<String> getApplicationUris(Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        DeployedMtaModule deployedModule = findDeployedModule(deployedMta, module);
        return urisCloudModelBuilder.getApplicationUris(module, parametersList, deployedModule);
    }

    protected <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    protected DeployedMtaModule findDeployedModule(DeployedMta deployedMta, Module module) {
        return deployedMta == null ? null : deployedMta.findDeployedModule(module.getName());
    }

    public String getApplicationName(Module module) {
        return (String) module.getParameters()
            .get(SupportedParameters.APP_NAME);
    }

    public List<String> getAllApplicationServices(Module module) {
        return getApplicationServices(module, this::allServicesRule);
    }

    protected List<String> getApplicationServices(Module module) {
        return getApplicationServices(module, this::filterExistingServicesRule);
    }

    protected boolean allServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return true;
    }

    protected boolean filterExistingServicesRule(ResourceAndResourceType resourceAndResourceType) {
        return !isExistingService(resourceAndResourceType.getResourceType());
    }

    private boolean isExistingService(ResourceType resourceType) {
        return resourceType.equals(ResourceType.EXISTING_SERVICE);
    }

    protected List<CloudTask> getTasks(List<Map<String, Object>> propertiesList) {
        return parseParameters(propertiesList, new TaskParametersParser(SupportedParameters.TASKS, configuration.isPrettyPrinting()));
    }

    protected CloudApplicationExtended createCloudApplication(String name, String moduleName, Staging staging, int diskQuota, int memory,
        int instances, List<String> uris, List<String> idleUris, List<String> services, List<ServiceKeyToInject> serviceKeys,
        Map<Object, Object> env, Map<String, Map<String, Object>> bindingParameters, List<CloudTask> tasks,
        List<ApplicationPort> applicationPorts, List<String> applicationDomains, RestartParameters restartParameters,
        DockerInfo dockerInfo) {
        CloudApplicationExtended app = createCloudApplication(name, moduleName, staging, diskQuota, memory, instances, uris, idleUris,
            services, serviceKeys, env, tasks, dockerInfo);
        if (bindingParameters != null) {
            app.setBindingParameters(bindingParameters);
        }
        app.setApplicationPorts(applicationPorts);
        app.setDomains(applicationDomains);
        app.setRestartParameters(restartParameters);
        return app;
    }

    protected static CloudApplicationExtended createCloudApplication(String name, String moduleName, Staging staging, int diskQuota,
        int memory, int instances, List<String> uris, List<String> idleUris, List<String> services,
        List<ServiceKeyToInject> serviceKeysToInject, Map<Object, Object> env, List<CloudTask> tasks, DockerInfo dockerInfo) {
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
        app.setDockerInfo(dockerInfo);
        return app;
    }

    protected void warnAboutUnsupportedParameters(List<Map<String, Object>> fullParametersList) {
        Map<String, Object> merged = mergeProperties(fullParametersList);
        applicationEnvCloudModelBuilder.removeSpecialApplicationProperties(merged);
        applicationEnvCloudModelBuilder.removeSpecialServiceProperties(merged);
        for (String parameterName : merged.keySet()) {
            String warningMessage = MessageFormat.format(Messages.UNSUPPORTED_PARAMETER, parameterName);
            stepLogger.warnWithoutProgressMessage(warningMessage);
            LOGGER.debug(warningMessage);
        }
    }

    protected Map<String, Map<String, Object>> getBindingParameters(Module module) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2()) {
            addBindingParameters(result, dependency, module);
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    protected void addBindingParameters(Map<String, Map<String, Object>> result, RequiredDependency dependency, Module module) {
        Resource resource = getResource(dependency.getName());
        if (resource != null) {
            MapUtil.addNonNull(result, cloudServiceNameMapper.getServiceName(resource.getName()),
                getBindingParameters(dependency, module.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getBindingParameters(RequiredDependency dependency, String moduleName) {
        Object bindingParameters = dependency.getParameters()
            .get(SupportedParameters.SERVICE_BINDING_CONFIG);
        if (bindingParameters == null) {
            return null;
        }
        if (!(bindingParameters instanceof Map)) {
            throw new ContentException(getInvalidServiceBindingConfigTypeErrorMessage(moduleName, dependency.getName(), bindingParameters));
        }
        return (Map<String, Object>) bindingParameters;
    }

    protected String getInvalidServiceBindingConfigTypeErrorMessage(String moduleName, String dependencyName, Object bindingParameters) {
        String prefix = ValidatorUtil.getPrefixedName(moduleName, dependencyName);
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
            ValidatorUtil.getPrefixedName(prefix, SupportedParameters.SERVICE_BINDING_CONFIG), Map.class.getSimpleName(),
            bindingParameters.getClass()
                .getSimpleName());
    }

    protected List<String> getApplicationServices(Module module, Predicate<ResourceAndResourceType> filterRule) {
        List<String> services = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2()) {
            ResourceAndResourceType pair = getApplicationService(dependency.getName());
            if (pair != null && filterRule.test(pair)) {
                CollectionUtils.addIgnoreNull(services, cloudServiceNameMapper.mapServiceName(pair.getResource(), pair.getResourceType()));
            }
        }
        return ListUtil.removeDuplicates(services);
    }

    protected ResourceAndResourceType getApplicationService(String dependencyName) {
        Resource resource = getResource(dependencyName);
        if (resource != null && CloudModelBuilderUtil.isService(resource)) {
            ResourceType serviceType = CloudModelBuilderUtil.getResourceType(resource.getParameters());
            return new ResourceAndResourceType(resource, serviceType);
        }
        return null;
    }

    protected List<ServiceKeyToInject> getServicesKeysToInject(Module module) {
        List<ServiceKeyToInject> serviceKeysToInject = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2()) {
            ServiceKeyToInject serviceKey = getServiceKeyToInject(dependency);
            CollectionUtils.addIgnoreNull(serviceKeysToInject, serviceKey);
        }
        return serviceKeysToInject;
    }

    protected ServiceKeyToInject getServiceKeyToInject(RequiredDependency dependency) {
        Resource resource = getResource(dependency.getName());
        if (resource != null && CloudModelBuilderUtil.isServiceKey(resource)) {
            return buildServiceKeyToInject(dependency, resource);
        }
        return null;
    }

    protected ServiceKeyToInject buildServiceKeyToInject(RequiredDependency dependency, Resource resource) {
        Map<String, Object> resourceParameters = resource.getParameters();
        String serviceName = PropertiesUtil.getRequiredParameter(resourceParameters, SupportedParameters.SERVICE_NAME);
        String serviceKeyName = (String) resourceParameters.getOrDefault(SupportedParameters.SERVICE_KEY_NAME, resource.getName());
        String envVarName = (String) dependency.getParameters()
            .getOrDefault(SupportedParameters.ENV_VAR_NAME, serviceKeyName);
        return new ServiceKeyToInject(envVarName, serviceName, serviceKeyName);
    }


    protected List<ApplicationPort> getApplicationPorts(Module module, List<Map<String, Object>> parametersList) {
        List<Integer> ports = urisCloudModelBuilder.getApplicationPorts(module, parametersList);
        ApplicationPortType portType = getType(module.getParameters());
        return getApplicationPorts(ports, portType);
    }

    protected List<ApplicationPort> getApplicationPorts(List<Integer> ports, ApplicationPortType portType) {
        List<ApplicationPort> applicationRoutes = new ArrayList<>();
        for (int portNumber : ports) {
            applicationRoutes.add(new ApplicationPort(portNumber, portType));
        }
        return applicationRoutes;
    }

    public List<String> getApplicationDomains(Module module) {
        List<String> applicationDomains = urisCloudModelBuilder.getApplicationDomains(module,
            parametersChainBuilder.buildModuleChain(module.getName()));
        return xsPlaceholderResolver.resolve(applicationDomains);
    }

    private ApplicationPortType getType(Map<String, Object> moduleParameters) {
        boolean isTcpRoute = (boolean) moduleParameters.getOrDefault(SupportedParameters.TCP, false);
        boolean isTcpsRoute = (boolean) moduleParameters.getOrDefault(SupportedParameters.TCPS, false);
        if (isTcpRoute && isTcpsRoute) {
            throw new ContentException(Messages.INVALID_TCP_ROUTE);
        }
        if (isTcpRoute) {
            return ApplicationPortType.TCP;
        } else if (isTcpsRoute) {
            return ApplicationPortType.TCPS;
        }
        return ApplicationPortType.HTTP;
    }

    protected Resource getResource(String dependencyName) {
        return handler.findDependency(deploymentDescriptor, dependencyName)._1;
    }

    public DeploymentMode getDeploymentMode() {
        return DeploymentMode.SEQUENTIAL;
    }
}
