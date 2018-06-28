package com.sap.cloud.lm.sl.cf.core.cf.v2_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.mergeProperties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort.ApplicationPortType;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ResourceAndResourceType;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ResourceType;
import com.sap.cloud.lm.sl.cf.core.helpers.UrisClassifier;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.MemoryParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.StagingParametersParser;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.builders.v2_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ApplicationsCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationsCloudModelBuilder.class);
    private static final int MTA_MAJOR_VERSION = 2;

    protected ParametersChainBuilder parametersChainBuilder;

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId) {
        this(deploymentDescriptor, configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId, null);
    }

    public ApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, CloudModelConfiguration configuration,
        DeployedMta deployedMta, SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver, String deployId,
        UserMessageLogger userMessageLogger) {
        super(deploymentDescriptor, configuration, deployedMta, systemParameters, xsPlaceholderResolver, deployId, userMessageLogger);
        this.parametersChainBuilder = new ParametersChainBuilder(deploymentDescriptor);
    }

    @Override
    protected HandlerFactory createHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    @Override
    protected PropertiesChainBuilder createPropertiesChainBuilder(
        com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor deploymentDescriptor) {
        DeploymentDescriptor v2DeploymentDescriptor = (DeploymentDescriptor) deploymentDescriptor;
        return new PropertiesChainBuilder(v2DeploymentDescriptor);
    }

    @Override
    protected ApplicationEnvironmentCloudModelBuilder createApplicationEnvironmentCloudModelBuilder(CloudModelConfiguration configuration,
        com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor deploymentDescriptor, XsPlaceholderResolver xsPlaceholderResolver,
        com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler handler,
        com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor propertiesAccessor, String deployId) {
        DeploymentDescriptor v2DeploymentDescriptor = (DeploymentDescriptor) deploymentDescriptor;
        DescriptorHandler v2Handler = (DescriptorHandler) handler;
        PropertiesAccessor v2PropertiesAccessor = (PropertiesAccessor) propertiesAccessor;
        return new ApplicationEnvironmentCloudModelBuilder(configuration, v2DeploymentDescriptor, xsPlaceholderResolver, v2Handler,
            v2PropertiesAccessor, deployId);
    }

    @Override
    protected CloudApplicationExtended getApplication(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) throws SLException {
        DeployedMtaModule deployedModule = findDeployedModule(deployedMta, module);
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        warnAboutUnsupportedParameters(parametersList);
        Staging staging = parseParameters(parametersList, new StagingParametersParser());
        int diskQuota = parseParameters(parametersList, new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0"));
        int memory = parseParameters(parametersList, new MemoryParametersParser(SupportedParameters.MEMORY, "0"));
        int instances = (Integer) getPropertyValue(parametersList, SupportedParameters.INSTANCES, 0);
        List<String> uris = urisCloudModelBuilder.getApplicationUris(module, parametersList);
        List<String> idleUris = urisCloudModelBuilder.getIdleApplicationUris(module, parametersList);
        List<String> resolvedUris = xsPlaceholderResolver.resolve(uris);
        List<String> resolvedIdleUris = xsPlaceholderResolver.resolve(idleUris);
        List<String> customUris = new UrisClassifier(xsPlaceholderResolver).getCustomUris(deployedModule);
        List<String> fullResolvedUris = ListUtil.merge(resolvedUris, customUris);
        List<String> services = getAllApplicationServices(module);
        List<ServiceKeyToInject> serviceKeys = getServicesKeysToInject(module);
        Map<Object, Object> env = applicationEnvCloudModelBuilder.build(module, uris, getApplicationServices(module),
            getSharedApplicationServices(module));
        List<CloudTask> tasks = getTasks(parametersList);
        Map<String, Map<String, Object>> bindingParameters = getBindingParameters((Module) module);
        List<ApplicationPort> applicationPorts = getApplicationPorts((Module) module, parametersList);
        List<String> applicationDomains = getApplicationDomains((Module) module, parametersList);
        return createCloudApplication(getApplicationName(module), module.getName(), staging, diskQuota, memory, instances, fullResolvedUris,
            resolvedIdleUris, services, serviceKeys, env, bindingParameters, tasks, applicationPorts, applicationDomains);
    }

    protected CloudApplicationExtended createCloudApplication(String name, String moduleName, Staging staging, int diskQuota, int memory,
        int instances, List<String> uris, List<String> idleUris, List<String> services, List<ServiceKeyToInject> serviceKeys,
        Map<Object, Object> env, Map<String, Map<String, Object>> bindingParameters, List<CloudTask> tasks,
        List<ApplicationPort> applicationPorts, List<String> applicationDomains) {
        CloudApplicationExtended app = super.createCloudApplication(name, moduleName, staging, diskQuota, memory, instances, uris, idleUris,
            services, serviceKeys, env, tasks);
        if (bindingParameters != null) {
            app.setBindingParameters(bindingParameters);
        }
        app.setApplicationPorts(applicationPorts);
        app.setDomains(applicationDomains);
        return app;
    }

    protected void warnAboutUnsupportedParameters(List<Map<String, Object>> fullParametersList) {
        Map<String, Object> merged = mergeProperties(fullParametersList);
        applicationEnvCloudModelBuilder.removeSpecialApplicationProperties(merged);
        applicationEnvCloudModelBuilder.removeSpecialServiceProperties(merged);
        for (String parameterName : merged.keySet()) {
            LOGGER.warn(MessageFormat.format(Messages.UNSUPPORTED_PARAMETER, parameterName));
        }
    }

    protected Map<String, Map<String, Object>> getBindingParameters(Module module) throws SLException {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2_0()) {
            addBindingParameters(result, dependency, module);
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    protected void addBindingParameters(Map<String, Map<String, Object>> result, RequiredDependency dependency, Module module)
        throws SLException {
        Resource resource = (Resource) getResource(dependency.getName());
        if (resource != null) {
            MapUtil.addNonNull(result, cloudServiceNameMapper.getServiceName(resource.getName()),
                getBindingParameters(dependency, module.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getBindingParameters(RequiredDependency dependency, String moduleName) throws ContentException {
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

    @Override
    protected List<String> getApplicationServices(com.sap.cloud.lm.sl.mta.model.v1_0.Module module,
        Predicate<ResourceAndResourceType> filterRule) throws SLException {
        return getApplicationServices((Module) module, filterRule);
    }

    protected List<String> getApplicationServices(Module module, Predicate<ResourceAndResourceType> filterRule) throws SLException {
        List<String> services = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2_0()) {
            ResourceAndResourceType pair = getApplicationService(dependency.getName());
            if (pair != null && filterRule.test(pair)) {
                ListUtil.addNonNull(services, cloudServiceNameMapper.mapServiceName(pair.getResource(), pair.getResourceType()));
            }
        }
        return ListUtil.removeDuplicates(services);
    }

    @Override
    protected ResourceAndResourceType getApplicationService(String dependencyName) {
        Resource resource = (Resource) getResource(dependencyName);
        if (resource != null && CloudModelBuilderUtil.isService(resource, propertiesAccessor)) {
            ResourceType serviceType = CloudModelBuilderUtil.getResourceType(resource.getParameters());
            return new ResourceAndResourceType(resource, serviceType);
        }
        return null;
    }

    @Override
    protected List<ServiceKeyToInject> getServicesKeysToInject(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        return getServicesKeysToInject((Module) module);
    }

    protected List<ServiceKeyToInject> getServicesKeysToInject(Module module) {
        List<ServiceKeyToInject> serviceKeysToInject = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2_0()) {
            ServiceKeyToInject serviceKey = getServiceKeyToInject(dependency);
            ListUtil.addNonNull(serviceKeysToInject, serviceKey);
        }
        return serviceKeysToInject;
    }

    protected ServiceKeyToInject getServiceKeyToInject(RequiredDependency dependency) {
        Resource resource = (Resource) getResource(dependency.getName());
        if (resource != null && CloudModelBuilderUtil.isServiceKey(resource, propertiesAccessor)) {
            Map<String, Object> resourceParameters = propertiesAccessor.getParameters(resource);
            String serviceName = PropertiesUtil.getRequiredParameter(resourceParameters, SupportedParameters.SERVICE_NAME);
            String serviceKeyName = (String) resourceParameters.getOrDefault(SupportedParameters.SERVICE_KEY_NAME, resource.getName());
            String envVarName = (String) dependency.getParameters()
                .getOrDefault(SupportedParameters.ENV_VAR_NAME, serviceKeyName);
            return new ServiceKeyToInject(envVarName, serviceName, serviceKeyName);
        }
        return null;
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

    protected List<String> getApplicationDomains(Module module, List<Map<String, Object>> parametersList) {
        List<String> applicationDomains = urisCloudModelBuilder.getApplicationDomains(module, parametersList);
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
}
