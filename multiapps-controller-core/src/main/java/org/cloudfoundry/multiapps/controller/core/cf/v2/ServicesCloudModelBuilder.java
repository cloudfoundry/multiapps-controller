package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.core.util.SpecialResourceTypesRequiredParametersUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicesCloudModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesCloudModelBuilder.class);

    protected final DeploymentDescriptor deploymentDescriptor;
    protected final String namespace;

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String namespace) {
        this.deploymentDescriptor = deploymentDescriptor;
        this.namespace = namespace;
    }

    public List<CloudServiceInstanceExtended> build(List<Resource> resourcesToProcess) {
        return resourcesToProcess.stream()
                                 .map(this::createService)
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList());
    }

    protected CloudServiceInstanceExtended createService(Resource resource) {
        ResourceType serviceType = CloudModelBuilderUtil.getResourceType(resource.getParameters());
        CommonServiceParameters commonServiceParameters = getCommonServiceParameters(resource);

        switch (serviceType) {
            case MANAGED_SERVICE:
                return createManagedService(resource, commonServiceParameters);
            case EXISTING_SERVICE:
                return createExistingService(resource, commonServiceParameters);
            case USER_PROVIDED_SERVICE:
                return createUserProvidedService(resource, commonServiceParameters);
            default:
                return null;
        }
    }

    protected CommonServiceParameters getCommonServiceParameters(Resource resource) {
        return new CommonServiceParameters(resource);
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceInstanceExtended createManagedService(Resource resource, CommonServiceParameters commonServiceParameters) {
        String serviceName = commonServiceParameters.getServiceName();
        Map<String, Object> parameters = resource.getParameters();
        SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(serviceName, ResourceType.MANAGED_SERVICE, parameters);

        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceName)
                                                    .resourceName(resource.getName())
                                                    .label((String) parameters.get(SupportedParameters.SERVICE))
                                                    .plan((String) parameters.get(SupportedParameters.SERVICE_PLAN))
                                                    .type(ServiceInstanceType.MANAGED)
                                                    .provider((String) parameters.get(SupportedParameters.SERVICE_PROVIDER))
                                                    .broker((String) parameters.get(SupportedParameters.SERVICE_BROKER))
                                                    .version((String) parameters.get(SupportedParameters.SERVICE_VERSION))
                                                    .tags((List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_TAGS,
                                                                                                 Collections.emptyList()))
                                                    .credentials(getServiceParameters(serviceName, parameters))
                                                    .isOptional(commonServiceParameters.isOptional())
                                                    .isManaged(true)
                                                    .shouldSkipParametersUpdate(commonServiceParameters.shouldSkipParametersUpdate())
                                                    .shouldSkipTagsUpdate(commonServiceParameters.shouldSkipTagsUpdate())
                                                    .shouldSkipPlanUpdate(commonServiceParameters.shouldSkipPlanUpdate())
                                                    .shouldSkipSyslogUrlUpdate(commonServiceParameters.shouldSkipSyslogUrlUpdate())
                                                    .v3Metadata(ServiceMetadataBuilder.build(deploymentDescriptor, namespace, resource))
                                                    .build();
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceInstanceExtended createUserProvidedService(Resource resource, CommonServiceParameters commonServiceParameters) {
        String serviceName = commonServiceParameters.getServiceName();
        Map<String, Object> parameters = resource.getParameters();
        SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(serviceName, ResourceType.USER_PROVIDED_SERVICE, parameters);
        Map<String, Object> credentials = getServiceParameters(serviceName, parameters);
        String label = (String) parameters.get(SupportedParameters.SERVICE);
        if (label != null) {
            LOGGER.warn(MessageFormat.format(Messages.IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE, label, serviceName));
        }
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceName)
                                                    .resourceName(resource.getName())
                                                    .type(ServiceInstanceType.USER_PROVIDED)
                                                    .credentials(credentials)
                                                    .syslogDrainUrl((String) parameters.get(SupportedParameters.SYSLOG_DRAIN_URL))
                                                    .tags((List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_TAGS,
                                                                                                 Collections.emptyList()))
                                                    .isOptional(commonServiceParameters.isOptional())
                                                    .isManaged(true)
                                                    .shouldSkipParametersUpdate(commonServiceParameters.shouldSkipParametersUpdate())
                                                    .shouldSkipTagsUpdate(commonServiceParameters.shouldSkipTagsUpdate())
                                                    .shouldSkipPlanUpdate(commonServiceParameters.shouldSkipPlanUpdate())
                                                    .shouldSkipSyslogUrlUpdate(commonServiceParameters.shouldSkipSyslogUrlUpdate())
                                                    .v3Metadata(ServiceMetadataBuilder.build(deploymentDescriptor, namespace, resource))
                                                    .build();
    }

    protected CloudServiceInstanceExtended createExistingService(Resource resource, CommonServiceParameters commonServiceParameters) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(commonServiceParameters.getServiceName())
                                                    .resourceName(resource.getName())
                                                    .isOptional(commonServiceParameters.isOptional())
                                                    .shouldSkipParametersUpdate(commonServiceParameters.shouldSkipParametersUpdate())
                                                    .shouldSkipTagsUpdate(commonServiceParameters.shouldSkipTagsUpdate())
                                                    .shouldSkipPlanUpdate(commonServiceParameters.shouldSkipPlanUpdate())
                                                    .shouldSkipSyslogUrlUpdate(commonServiceParameters.shouldSkipSyslogUrlUpdate())
                                                    .v3Metadata(ServiceMetadataBuilder.build(deploymentDescriptor, namespace, resource))
                                                    .build();
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getServiceParameters(String serviceName, Map<String, Object> parameters) {
        Object serviceParameters = parameters.get(SupportedParameters.SERVICE_CONFIG);
        if (serviceParameters == null) {
            return Collections.emptyMap();
        }
        if (!(serviceParameters instanceof Map)) {
            throw new ContentException(getInvalidServiceConfigTypeErrorMessage(serviceName, serviceParameters));
        }
        return new TreeMap<>((Map<String, Object>) serviceParameters);
    }

    protected String getInvalidServiceConfigTypeErrorMessage(String serviceName, Object serviceParameters) {
        return MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.INVALID_TYPE_FOR_KEY,
                                    org.cloudfoundry.multiapps.mta.util.NameUtil.getPrefixedName(serviceName,
                                                                                                 SupportedParameters.SERVICE_CONFIG),
                                    Map.class.getSimpleName(), serviceParameters.getClass()
                                                                                .getSimpleName());
    }

    protected static class CommonServiceParameters {
        protected final Resource resource;
        private final Map<String, Boolean> shouldSkipUpdates;

        @SuppressWarnings("unchecked")
        protected CommonServiceParameters(Resource resource) {
            this.resource = resource;
            this.shouldSkipUpdates = (Map<String, Boolean>) resource.getParameters()
                                                                    .getOrDefault(SupportedParameters.SKIP_SERVICE_UPDATES,
                                                                                  Collections.emptyMap());
        }

        private String getServiceName() {
            return NameUtil.getServiceName(resource);
        }

        protected boolean isOptional() {
            return false;
        }

        private boolean shouldSkipParametersUpdate() {
            return shouldSkipUpdates.getOrDefault("parameters", false);
        }

        private boolean shouldSkipTagsUpdate() {
            return shouldSkipUpdates.getOrDefault("tags", false);
        }

        private boolean shouldSkipPlanUpdate() {
            return shouldSkipUpdates.getOrDefault("plan", false);
        }

        private boolean shouldSkipSyslogUrlUpdate() {
            return shouldSkipUpdates.getOrDefault("syslog-drain-url", false);
        }

    }

}
