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
        Map<String, Object> parameters = resource.getParameters();
        ResourceType serviceType = getResourceType(parameters);
        boolean isOptional = isOptional(resource);
        boolean shouldIgnoreUpdateErrors = (boolean) parameters.getOrDefault(SupportedParameters.IGNORE_UPDATE_ERRORS, false);
        CloudServiceExtended service = createService(NameUtil.getServiceName(resource), serviceType, isOptional,
            shouldIgnoreUpdateErrors, parameters);
        if (service != null) {
            service.setResourceName(resource.getName());
        }
        return service;
    }

    protected boolean isOptional(Resource resource) {
        return false;
    }

    protected CloudServiceExtended createService(String serviceName, ResourceType serviceType, boolean isOptional,
        boolean shouldIgnoreUpdateErrors, Map<String, Object> parameters) {
        if (serviceType.equals(ResourceType.MANAGED_SERVICE)) {
            return createManagedService(serviceName, isOptional, shouldIgnoreUpdateErrors, parameters);
        } else if (serviceType.equals(ResourceType.USER_PROVIDED_SERVICE)) {
            return createUserProvidedService(serviceName, isOptional, shouldIgnoreUpdateErrors, parameters);
        } else if (serviceType.equals(ResourceType.EXISTING_SERVICE)) {
            return createExistingService(serviceName, isOptional, shouldIgnoreUpdateErrors);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceExtended createManagedService(String serviceName, boolean isOptional, boolean shouldIgnoreUpdateErrors,
        Map<String, Object> parameters) {
        SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(serviceName, ResourceType.MANAGED_SERVICE, parameters);
        String label = (String) parameters.get(SupportedParameters.SERVICE);
        List<String> alternativeLabels = (List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_ALTERNATIVES,
            Collections.emptyList());
        String plan = (String) parameters.get(SupportedParameters.SERVICE_PLAN);
        String provider = (String) parameters.get(SupportedParameters.SERVICE_PROVIDER);
        String version = (String) parameters.get(SupportedParameters.SERVICE_VERSION);
        List<String> serviceTags = (List<String>) parameters.getOrDefault(SupportedParameters.SERVICE_TAGS, Collections.emptyList());
        Map<String, Object> credentials = getServiceParameters(serviceName, parameters);

        return createCloudService(serviceName, label, plan, provider, version, alternativeLabels, credentials, serviceTags, isOptional,
            true, shouldIgnoreUpdateErrors);
    }

    protected CloudServiceExtended createUserProvidedService(String serviceName, boolean isOptional, boolean shouldIgnoreUpdateErrors,
        Map<String, Object> parameters) {
        SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(serviceName, ResourceType.USER_PROVIDED_SERVICE, parameters);
        Map<String, Object> credentials = getServiceParameters(serviceName, parameters);
        String label = (String) parameters.get(SupportedParameters.SERVICE);
        if (label != null) {
            LOGGER.warn(MessageFormat.format(Messages.IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE, label, serviceName));
        }
        return createCloudService(serviceName, null, null, null, null, null, credentials, Collections.emptyList(), isOptional, true,
            shouldIgnoreUpdateErrors);
    }

    protected CloudServiceExtended createExistingService(String serviceName, boolean isOptional, boolean shouldIgnoreUpdateErrors) {
        return createCloudService(serviceName, null, null, null, null, null, Collections.emptyMap(), Collections.emptyList(), isOptional,
            false, shouldIgnoreUpdateErrors);
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
            ValidatorUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_CONFIG), Map.class.getSimpleName(),
            serviceParameters.getClass()
                .getSimpleName());
    }

    protected CloudServiceExtended createCloudService(String name, String label, String plan, String provider, String version,
        List<String> alternativeLabels, Map<String, Object> credentials, List<String> tags, boolean isOptional, boolean isManaged,
        boolean shouldIgnoreUpdateErrors) {
        CloudServiceExtended service = new CloudServiceExtended(null, name);
        service.setLabel(label);
        service.setPlan(plan);
        service.setProvider(provider);
        service.setVersion(version);
        service.setAlternativeLabels(alternativeLabels);
        service.setCredentials(credentials);
        service.setTags(tags);
        service.setOptional(isOptional);
        service.setManaged(isManaged);
        service.setIgnoreUpdateErrors(shouldIgnoreUpdateErrors);
        return service;
    }

}
