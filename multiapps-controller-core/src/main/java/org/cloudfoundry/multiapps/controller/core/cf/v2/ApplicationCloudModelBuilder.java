package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.DeploymentMode;
import org.cloudfoundry.multiapps.controller.core.cf.detect.AppSuffixDeterminer;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication.ProductizationState;
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

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

public class ApplicationCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 2;

    protected final DescriptorHandler handler;
    protected final DeploymentDescriptor deploymentDescriptor;
    protected final String namespace;
    protected final boolean prettyPrinting;
    protected final ApplicationEnvironmentCloudModelBuilder applicationEnvCloudModelBuilder;
    protected final DeployedMta deployedMta;
    protected final UserMessageLogger stepLogger;
    protected final AppSuffixDeterminer appSuffixDeterminer;
    protected final CloudControllerClient client;

    protected final ParametersChainBuilder parametersChainBuilder;

    protected ApplicationCloudModelBuilder(AbstractBuilder<?> builder) {
        CloudHandlerFactory handlerFactory = createCloudHandlerFactory();
        this.handler = handlerFactory.getDescriptorHandler();
        this.deploymentDescriptor = builder.deploymentDescriptor;
        this.namespace = builder.namespace;
        this.prettyPrinting = builder.prettyPrinting;
        this.applicationEnvCloudModelBuilder = new ApplicationEnvironmentCloudModelBuilder(deploymentDescriptor,
                                                                                           builder.deployId,
                                                                                           namespace,
                                                                                           prettyPrinting);
        this.deployedMta = builder.deployedMta;
        this.parametersChainBuilder = new ParametersChainBuilder(deploymentDescriptor);
        this.stepLogger = builder.userMessageLogger;
        this.appSuffixDeterminer = builder.appSuffixDeterminer;
        this.client = builder.client;
    }

    protected CloudHandlerFactory createCloudHandlerFactory() {
        return CloudHandlerFactory.forSchemaVersion(MTA_MAJOR_VERSION);
    }

    public CloudApplicationExtended build(Module moduleToDeploy, ModuleToDeployHelper moduleToDeployHelper) {
        if (moduleToDeployHelper.isApplication(moduleToDeploy)) {
            return getApplication(moduleToDeploy);
        }
        return null;
    }

    protected CloudApplicationExtended getApplication(Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        ApplicationRoutesCloudModelBuilder routesCloudModelBuilder = getApplicationRoutesCloudModelBuilder(parametersList);
        Set<CloudRoute> routes = getApplicationRoutes(module);
        Set<CloudRoute> idleRoutes = routesCloudModelBuilder.getIdleApplicationRoutes(module, parametersList);
        return ImmutableCloudApplicationExtended.builder()
                                                .name(getApplicationName(module))
                                                .moduleName(module.getName())
                                                .staging(parseParameters(parametersList, new StagingParametersParser()))
                                                .diskQuota(parseParameters(parametersList,
                                                                           new MemoryParametersParser(SupportedParameters.DISK_QUOTA, "0")))
                                                .memory(parseParameters(parametersList,
                                                                        new MemoryParametersParser(SupportedParameters.MEMORY, "0")))
                                                .instances((Integer) PropertiesUtil.getPropertyValue(parametersList,
                                                                                                     SupportedParameters.INSTANCES, 0))
                                                .routes(routes)
                                                .idleRoutes(idleRoutes)
                                                .services(getAllApplicationServices(module))
                                                .serviceKeysToInject(getServicesKeysToInject(module))
                                                .env(applicationEnvCloudModelBuilder.build(module))
                                                .bindingParameters(getBindingParameters(module))
                                                .tasks(getTasks(parametersList))
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

    public Set<CloudRoute> getApplicationRoutes(Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        DeployedMtaApplication deployedApplication = findDeployedApplication(module);
        return getApplicationRoutesCloudModelBuilder(parametersList).getApplicationRoutes(module, parametersList, deployedApplication);
    }

    private ApplicationRoutesCloudModelBuilder getApplicationRoutesCloudModelBuilder(List<Map<String, Object>> parametersList) {
        return new ApplicationRoutesCloudModelBuilder(deploymentDescriptor, client, getApplicationAttributesUpdateStrategy(parametersList));
    }

    private DeployedMtaApplication findDeployedApplication(Module module) {
        return deployedMta == null ? null : findDeployedApplication(module.getName(), deployedMta);
    }

    private DeployedMtaApplication findDeployedApplication(String moduleName, DeployedMta deployedMta) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(application -> application.getModuleName()
                                                            .equalsIgnoreCase(moduleName))
                          .filter(application -> ProductizationState.LIVE.equals(application.getProductizationState()))
                          .findFirst()
                          .orElse(null);
    }

    private String getApplicationName(Module module) {
        String applicationName = NameUtil.getApplicationName(module);
        if (appSuffixDeterminer.shouldAppendApplicationSuffix()) {
            applicationName += BlueGreenApplicationNameSuffix.IDLE.asSuffix();
        }
        return applicationName;
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

    protected boolean allServicesRule(Resource resource, ResourceType resourceType) {
        return true;
    }

    protected boolean filterExistingServicesRule(Resource resource, ResourceType resourceType) {
        return !isExistingService(resourceType);
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
            MapUtil.addNonNull(result, resource.getName(), getBindingParameters(dependency, module.getName()));
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
        String prefix = org.cloudfoundry.multiapps.mta.util.NameUtil.getPrefixedName(moduleName, dependencyName);
        return MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.INVALID_TYPE_FOR_KEY,
                                    org.cloudfoundry.multiapps.mta.util.NameUtil.getPrefixedName(prefix,
                                                                                                 SupportedParameters.SERVICE_BINDING_CONFIG),
                                    Map.class.getSimpleName(), bindingParameters.getClass()
                                                                                .getSimpleName());
    }

    protected List<String> getApplicationServices(Module module, BiPredicate<Resource, ResourceType> filterRule) {
        Set<String> services = new LinkedHashSet<>();
        for (RequiredDependency dependency : module.getRequiredDependencies()) {
            Resource resource = getResource(dependency.getName());
            if (resource == null || !CloudModelBuilderUtil.isService(resource)) {
                continue;
            }
            ResourceType serviceType = CloudModelBuilderUtil.getResourceType(resource.getParameters());
            if (filterRule.test(resource, serviceType)) {
                CollectionUtils.addIgnoreNull(services, NameUtil.getServiceName(resource));
            }
        }
        return new ArrayList<>(services);
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
        if (resource != null && CloudModelBuilderUtil.isExistingServiceKey(resource)) {
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
        return getApplicationRoutesCloudModelBuilder(parametersList).getApplicationDomains(module,
                                                                                           parametersChainBuilder.buildModuleChain(module.getName()));
    }

    protected Resource getResource(String dependencyName) {
        return handler.findResource(deploymentDescriptor, dependencyName);
    }

    public DeploymentMode getDeploymentMode() {
        return DeploymentMode.SEQUENTIAL;
    }

    protected abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {
        private DeploymentDescriptor deploymentDescriptor;
        private boolean prettyPrinting;
        private DeployedMta deployedMta;
        private String deployId;
        private String namespace;
        private UserMessageLogger userMessageLogger;
        private AppSuffixDeterminer appSuffixDeterminer;
        private CloudControllerClient client;

        public T deploymentDescriptor(DeploymentDescriptor deploymentDescriptor) {
            this.deploymentDescriptor = deploymentDescriptor;
            return self();
        }

        public T prettyPrinting(boolean prettyPrinting) {
            this.prettyPrinting = prettyPrinting;
            return self();
        }

        public T deployedMta(DeployedMta deployedMta) {
            this.deployedMta = deployedMta;
            return self();
        }

        public T deployId(String deployId) {
            this.deployId = deployId;
            return self();
        }

        public T namespace(String namespace) {
            this.namespace = namespace;
            return self();
        }

        public T userMessageLogger(UserMessageLogger userMessageLogger) {
            this.userMessageLogger = userMessageLogger;
            return self();
        }

        public T appSuffixDeterminer(AppSuffixDeterminer appSuffixDeterminer) {
            this.appSuffixDeterminer = appSuffixDeterminer;
            return self();
        }

        public T client(CloudControllerClient client) {
            this.client = client;
            return self();
        }

        protected abstract T self();

        public abstract ApplicationCloudModelBuilder build();
    }

    public static class Builder extends AbstractBuilder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ApplicationCloudModelBuilder build() {
            return new ApplicationCloudModelBuilder(self());
        }
    }

}
