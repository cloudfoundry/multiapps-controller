package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.core.cf.DeploymentMode;
import org.cloudfoundry.multiapps.controller.core.cf.HandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.parser.ApplicationAttributeUpdateStrategyParser;
import org.cloudfoundry.multiapps.controller.core.parser.DockerInfoParser;
import org.cloudfoundry.multiapps.controller.core.parser.MemoryParametersParser;
import org.cloudfoundry.multiapps.controller.core.parser.ParametersParser;
import org.cloudfoundry.multiapps.controller.core.parser.RestartParametersParser;
import org.cloudfoundry.multiapps.controller.core.parser.StagingParametersParser;
import org.cloudfoundry.multiapps.controller.core.parser.TaskParametersParser;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.cloudfoundry.multiapps.mta.util.ValidatorUtil;

public class ApplicationCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 2;

    protected final DescriptorHandler handler;
    protected final DeploymentDescriptor deploymentDescriptor;
    protected final String namespace;
    protected final boolean prettyPrinting;
    protected final ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder;
    protected final DeployedMta deployedMta;
    protected final UserMessageLogger stepLogger;

    protected final ParametersChainBuilder parametersChainBuilder;

    public ApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting, DeployedMta deployedMta,
                                        String deployId, String namespace, UserMessageLogger stepLogger) {
        HandlerFactory handlerFactory = createHandlerFactory();
        this.handler = handlerFactory.getDescriptorHandler();
        this.deploymentDescriptor = deploymentDescriptor;
        this.namespace = namespace;
        this.prettyPrinting = prettyPrinting;
        this.applicationEnvCloudModelBuilder = new ApplicationEnvironmentCloudModelBuilder(deploymentDescriptor,
                                                                                           deployId,
                                                                                           namespace,
                                                                                           prettyPrinting);
        this.deployedMta = deployedMta;
        this.parametersChainBuilder = new ParametersChainBuilder(deploymentDescriptor);
        this.stepLogger = stepLogger;
    }

    protected HandlerFactory createHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }

    public CloudApplicationExtended build(Module moduleToDeploy, ModuleToDeployHelper moduleToDeployHelper) {
        if (moduleToDeployHelper.isApplication(moduleToDeploy)) {
            return getApplication(moduleToDeploy);
        }
        return null;
    }

    protected CloudApplicationExtended getApplication(Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        ApplicationUrisCloudModelBuilder urisCloudModelBuilder = getApplicationUrisCloudModelBuilder(parametersList);
        List<String> uris = getApplicationUris(module);
        List<String> idleUris = urisCloudModelBuilder.getIdleApplicationUris(module, parametersList);
        return ImmutableCloudApplicationExtended.builder()
                                                .name(NameUtil.getApplicationName(module))
                                                .moduleName(module.getName())
                                                .staging(parseParameters(parametersList, new StagingParametersParser()))
                                                .diskQuota(parseParameters(parametersList,
                                                                           new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0")))
                                                .memory(parseParameters(parametersList,
                                                                        new MemoryParametersParser(SupportedParameters.MEMORY, "0")))
                                                .instances((Integer) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.INSTANCES, 0))
                                                .uris(uris)
                                                .idleUris(idleUris)
                                                .services(getAllApplicationServices(module))
                                                .serviceKeysToInject(getServicesKeysToInject(module))
                                                .env(applicationEnvCloudModelBuilder.build(module, getApplicationServices(module)))
                                                .bindingParameters(getBindingParameters(module))
                                                .tasks(getTasks(parametersList))
                                                .domains(getApplicationDomains(parametersList, module))
                                                .restartParameters(parseParameters(parametersList, new RestartParametersParser()))
                                                .dockerInfo(parseParameters(parametersList, new DockerInfoParser()))
                                                .attributesUpdateStrategy(getApplicationAttributesUpdateStrategy(parametersList))
                                                .v3Metadata(ApplicationMetadataBuilder.build(deploymentDescriptor, namespace, module,
                                                                                             getApplicationServices(module)))
                                                .build();
    }

    private AttributeUpdateStrategy getApplicationAttributesUpdateStrategy(List<Map<String, Object>> parametersList) {
        return parseParameters(parametersList, new ApplicationAttributeUpdateStrategyParser());
    }

    private ApplicationUrisCloudModelBuilder getApplicationUrisCloudModelBuilder(List<Map<String, Object>> parametersList) {
        return new ApplicationUrisCloudModelBuilder(deploymentDescriptor, getApplicationAttributesUpdateStrategy(parametersList));
    }

    public List<String> getApplicationUris(Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        DeployedMtaApplication deployedApplication = findDeployedApplication(module);
        return getApplicationUrisCloudModelBuilder(parametersList).getApplicationUris(module, parametersList, deployedApplication);
    }

    private DeployedMtaApplication findDeployedApplication(Module module) {
        return deployedMta == null ? null
            : findDeployedApplication(module.getName(), deployedMta, DeployedMtaApplication.ProductizationState.LIVE);
    }

    private DeployedMtaApplication findDeployedApplication(String moduleName, DeployedMta deployedMta,
                                                           DeployedMtaApplication.ProductizationState productizationState) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(application -> application.getModuleName()
                                                            .equalsIgnoreCase(moduleName))
                          .filter(application -> application.getProductizationState()
                                                            .equals(productizationState))
                          .findFirst()
                          .orElse(null);
    }

    protected <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
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
        return parseParameters(propertiesList, getTasksParametersParser());
    }

    private TaskParametersParser getTasksParametersParser() {
        return new TaskParametersParser(SupportedParameters.TASKS);
    }

    protected Map<String, Map<String, Object>> getBindingParameters(Module module) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (RequiredDependency dependency : module.getRequiredDependencies()) {
            addBindingParameters(result, dependency, module);
        }
        return result;
    }

    protected void addBindingParameters(Map<String, Map<String, Object>> result, RequiredDependency dependency, Module module) {
        Resource resource = getResource(dependency.getName());
        if (resource != null) {
            MapUtil.addNonNull(result, NameUtil.getServiceName(resource), getBindingParameters(dependency, module.getName()));
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
        return MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.INVALID_TYPE_FOR_KEY,
                                    ValidatorUtil.getPrefixedName(prefix, SupportedParameters.SERVICE_BINDING_CONFIG),
                                    Map.class.getSimpleName(), bindingParameters.getClass()
                                                                                .getSimpleName());
    }

    protected List<String> getApplicationServices(Module module, Predicate<ResourceAndResourceType> filterRule) {
        Set<String> services = new LinkedHashSet<>();
        for (RequiredDependency dependency : module.getRequiredDependencies()) {
            ResourceAndResourceType resourceWithType = getResourceWithType(dependency.getName());
            if (resourceWithType != null && filterRule.test(resourceWithType)) {
                CollectionUtils.addIgnoreNull(services, NameUtil.getServiceName(resourceWithType.getResource()));
            }
        }
        return new ArrayList<>(services);
    }

    protected ResourceAndResourceType getResourceWithType(String dependencyName) {
        Resource resource = getResource(dependencyName);
        if (resource != null && CloudModelBuilderUtil.isService(resource)) {
            ResourceType serviceType = CloudModelBuilderUtil.getResourceType(resource.getParameters());
            return new ResourceAndResourceType(resource, serviceType);
        }
        return null;
    }

    protected List<ServiceKeyToInject> getServicesKeysToInject(Module module) {
        return module.getRequiredDependencies()
                     .stream()
                     .map(this::getServiceKeyToInject)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
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

    public List<String> getApplicationDomains(List<Map<String, Object>> parametersList, Module module) {
        return getApplicationUrisCloudModelBuilder(parametersList).getApplicationDomains(module,
                                                                                         parametersChainBuilder.buildModuleChain(module.getName()));
    }

    protected Resource getResource(String dependencyName) {
        return handler.findResource(deploymentDescriptor, dependencyName);
    }

    public DeploymentMode getDeploymentMode() {
        return DeploymentMode.SEQUENTIAL;
    }
}
