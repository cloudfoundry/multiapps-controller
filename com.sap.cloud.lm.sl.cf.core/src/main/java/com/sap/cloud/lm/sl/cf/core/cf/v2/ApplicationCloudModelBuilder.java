package com.sap.cloud.lm.sl.cf.core.cf.v2;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

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

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.DeploymentMode;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.parser.ApplicationAttributeUpdateStrategyParser;
import com.sap.cloud.lm.sl.cf.core.parser.DockerInfoParser;
import com.sap.cloud.lm.sl.cf.core.parser.MemoryParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.ParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.RestartParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.StagingParametersParser;
import com.sap.cloud.lm.sl.cf.core.parser.TaskParametersParser;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ApplicationCloudModelBuilder {

    public static final String DEPENDENCY_TYPE_SOFT = "soft";
    public static final String DEPENDENCY_TYPE_HARD = "hard";

    private static final int MTA_MAJOR_VERSION = 2;

    protected final DescriptorHandler handler;
    protected final DeploymentDescriptor deploymentDescriptor;
    protected final boolean prettyPrinting;
    protected final ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder;
    protected final DeployedMta deployedMta;
    protected final UserMessageLogger stepLogger;

    protected final ParametersChainBuilder parametersChainBuilder;

    public ApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, boolean prettyPrinting, DeployedMta deployedMta,
                                        String deployId, UserMessageLogger stepLogger) {
        HandlerFactory handlerFactory = createHandlerFactory();
        this.handler = handlerFactory.getDescriptorHandler();
        this.deploymentDescriptor = deploymentDescriptor;
        this.prettyPrinting = prettyPrinting;
        this.applicationEnvCloudModelBuilder = new ApplicationEnvironmentCloudModelBuilder(deploymentDescriptor, deployId, prettyPrinting);
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
        ApplicationUrisCloudModelBuilder urisCloudModelBuilder = getApplicationUrisCloudModelBuilder(parametersList);
        List<String> uris = getApplicationUris(module);
        List<String> idleUris = urisCloudModelBuilder.getIdleApplicationUris(module, parametersList);
        List<ResourceAndResourceType> resourcesAndResourceTypes = getResourcesAndResourceTypesFromModule(module);
        return ImmutableCloudApplicationExtended.builder()
                                                .name(NameUtil.getApplicationName(module))
                                                .moduleName(module.getName())
                                                .staging(parseParameters(parametersList, new StagingParametersParser()))
                                                .diskQuota(parseParameters(parametersList,
                                                                           new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0")))
                                                .memory(parseParameters(parametersList,
                                                                        new MemoryParametersParser(SupportedParameters.MEMORY, "0")))
                                                .instances((Integer) getPropertyValue(parametersList, SupportedParameters.INSTANCES, 0))
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
                                                .v3Metadata(ApplicationMetadataBuilder.build(deploymentDescriptor, module,
                                                                                             resourcesAndResourceTypes))
                                                .build();
    }

    private List<ResourceAndResourceType> getResourcesAndResourceTypesFromModule(Module module) {
        return module.getRequiredDependencies()
                     .stream()
                     .map(dependency -> getResourceWithType(dependency.getName()))
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
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
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
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
