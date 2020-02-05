package com.sap.cloud.lm.sl.cf.core.cf.v2;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.getResourceType;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.SpecialResourceTypesRequiredParametersUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ServicesCloudModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesCloudModelBuilder.class);

    protected final DeploymentDescriptor deploymentDescriptor;

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        this.deploymentDescriptor = deploymentDescriptor;
    }

    public List<CloudServiceExtended> build(List<Resource> resourcesToProcess) {
        return resourcesToProcess.stream()
                                 .map(this::getService)
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList());
    }

    protected CloudServiceExtended getService(Resource resource) {
        boolean isOptional = isOptional(resource);
        boolean shouldIgnoreUpdateErrors = (boolean) resource.getParameters()
                                                             .getOrDefault(SupportedParameters.IGNORE_UPDATE_ERRORS, false);
        return createService(resource, isOptional, shouldIgnoreUpdateErrors);
    }

    protected boolean isOptional(Resource resource) {
        return false;
    }

    protected CloudServiceExtended createService(Resource resource, boolean isOptional, boolean shouldIgnoreUpdateErrors) {
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
    protected CloudServiceExtended createManagedService(Resource resource, String serviceName, boolean isOptional,
                                                        boolean shouldIgnoreUpdateErrors) {
        Map<String, Object> parameters = resource.getParameters();
        SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(serviceName, ResourceType.MANAGED_SERVICE, parameters);
        return ImmutableCloudServiceExtended.builder()
                                            .name(serviceName)
                                            .resourceName(resource.getName())
                                            .label((String) parameters.get(SupportedParameters.SERVICE))
                                            .plan((String) parameters.get(SupportedParameters.SERVICE_PLAN))
                                            .provider((String) parameters.get(SupportedParameters.SERVICE_PROVIDER))
                                            .version((String) parameters.get(SupportedParameters.SERVICE_VERSION))
                                            .tags((List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_TAGS,
                                                                                         Collections.emptyList()))
                                            .credentials(getServiceParameters(serviceName, parameters))
                                            .alternativeLabels((List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_ALTERNATIVES,
                                                                                                      Collections.emptyList()))
                                            .isOptional(isOptional)
                                            .isManaged(true)
                                            .shouldIgnoreUpdateErrors(shouldIgnoreUpdateErrors)
                                            .v3Metadata(ServiceMetadataBuilder.build(deploymentDescriptor, resource))
                                            .build();
    }

    protected CloudServiceExtended createUserProvidedService(Resource resource, String serviceName, boolean isOptional,
                                                             boolean shouldIgnoreUpdateErrors) {
        Map<String, Object> parameters = resource.getParameters();
        SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(serviceName, ResourceType.USER_PROVIDED_SERVICE, parameters);
        Map<String, Object> credentials = getServiceParameters(serviceName, parameters);
        String label = (String) parameters.get(SupportedParameters.SERVICE);
        if (label != null) {
            LOGGER.warn(MessageFormat.format(Messages.IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE, label, serviceName));
        }
        return ImmutableCloudServiceExtended.builder()
                                            .name(serviceName)
                                            .resourceName(resource.getName())
                                            .credentials(credentials)
                                            .isOptional(isOptional)
                                            .isManaged(true)
                                            .shouldIgnoreUpdateErrors(shouldIgnoreUpdateErrors)
                                            .build();
    }

    protected CloudServiceExtended createExistingService(Resource resource, String serviceName, boolean isOptional,
                                                         boolean shouldIgnoreUpdateErrors) {
        return ImmutableCloudServiceExtended.builder()
                                            .name(serviceName)
                                            .resourceName(resource.getName())
                                            .isOptional(isOptional)
                                            .shouldIgnoreUpdateErrors(shouldIgnoreUpdateErrors)
                                            .v3Metadata(ServiceMetadataBuilder.build(deploymentDescriptor, resource))
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
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
                                    ValidatorUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_CONFIG),
                                    Map.class.getSimpleName(), serviceParameters.getClass()
                                                                                .getSimpleName());
    }

}
