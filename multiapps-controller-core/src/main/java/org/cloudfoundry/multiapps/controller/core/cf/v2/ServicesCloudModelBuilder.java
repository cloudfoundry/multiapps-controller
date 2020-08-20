package org.cloudfoundry.multiapps.controller.core.cf.v2;

import static org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil.getResourceType;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
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
                                 .map(this::getService)
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList());
    }

    protected CloudServiceInstanceExtended getService(Resource resource) {
        boolean isOptional = isOptional(resource);
        boolean shouldIgnoreUpdateErrors = (boolean) resource.getParameters()
                                                             .getOrDefault(SupportedParameters.IGNORE_UPDATE_ERRORS, false);
        return createService(resource, isOptional, shouldIgnoreUpdateErrors);
    }

    protected boolean isOptional(Resource resource) {
        return false;
    }

    protected CloudServiceInstanceExtended createService(Resource resource, boolean isOptional, boolean shouldIgnoreUpdateErrors) {
        String serviceName = NameUtil.getServiceName(resource);
        ResourceType serviceType = getResourceType(resource.getParameters());
        if (serviceType.equals(ResourceType.MANAGED_SERVICE)) {
            return createManagedService(resource, serviceName, isOptional, shouldIgnoreUpdateErrors);
        } else if (serviceType.equals(ResourceType.USER_PROVIDED_SERVICE)) {
            return createUserProvidedService(resource, serviceName, isOptional, shouldIgnoreUpdateErrors);
        } else if (serviceType.equals(ResourceType.EXISTING_SERVICE)) {
            return createExistingService(resource, serviceName, isOptional, shouldIgnoreUpdateErrors);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceInstanceExtended createManagedService(Resource resource, String serviceName, boolean isOptional,
                                                                boolean shouldIgnoreUpdateErrors) {
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
                                                    .alternativeLabels((List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_ALTERNATIVES,
                                                                                                              Collections.emptyList()))
                                                    .isOptional(isOptional)
                                                    .isManaged(true)
                                                    .shouldIgnoreUpdateErrors(shouldIgnoreUpdateErrors)
                                                    .v3Metadata(ServiceMetadataBuilder.build(deploymentDescriptor, namespace, resource))
                                                    .build();
    }

    protected CloudServiceInstanceExtended createUserProvidedService(Resource resource, String serviceName, boolean isOptional,
                                                                     boolean shouldIgnoreUpdateErrors) {
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
                                                    .isOptional(isOptional)
                                                    .isManaged(true)
                                                    .shouldIgnoreUpdateErrors(shouldIgnoreUpdateErrors)
                                                    .build();
    }

    protected CloudServiceInstanceExtended createExistingService(Resource resource, String serviceName, boolean isOptional,
                                                                 boolean shouldIgnoreUpdateErrors) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceName)
                                                    .resourceName(resource.getName())
                                                    .isOptional(isOptional)
                                                    .shouldIgnoreUpdateErrors(shouldIgnoreUpdateErrors)
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

}
