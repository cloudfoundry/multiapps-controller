package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.getResourceType;
import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ServicesCloudModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesCloudModelBuilder.class);

    private CloudServiceNameMapper cloudServiceNameMapper;
    protected final PropertiesAccessor propertiesAccessor;
    protected final DeploymentDescriptor deploymentDescriptor;
    protected UserMessageLogger userMessageLogger;

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration) {
        this.propertiesAccessor = propertiesAccessor;
        this.deploymentDescriptor = deploymentDescriptor;
        this.cloudServiceNameMapper = new CloudServiceNameMapper(configuration, propertiesAccessor, deploymentDescriptor);
    }

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration, UserMessageLogger userMessageLogger) {
        this.propertiesAccessor = propertiesAccessor;
        this.deploymentDescriptor = deploymentDescriptor;
        this.cloudServiceNameMapper = new CloudServiceNameMapper(configuration, propertiesAccessor, deploymentDescriptor);
        this.userMessageLogger = userMessageLogger;
    }

    public List<CloudServiceExtended> build(Set<String> modules) throws SLException {
        List<CloudServiceExtended> services = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource, propertiesAccessor)) {
                ListUtil.addNonNull(services, getService(resource));
            } else {
                warnInvalidResourceType(resource);
            }
        }
        return services;
    }

    private void warnInvalidResourceType(Resource resource) {
        if (userMessageLogger == null || resource.getType() == null) {
            return;
        }
        userMessageLogger.warn("Optional resource \"{0}\" it will be not created because it's not a service", resource.getName());
    }

    protected CloudServiceExtended getService(Resource resource) throws SLException {
        Map<String, Object> parameters = propertiesAccessor.getParameters(resource);
        ResourceType serviceType = getResourceType(parameters);
        boolean isOptional = isOptional(resource);
        boolean shouldIgnoreUpdateErrors = (boolean) parameters.getOrDefault(SupportedParameters.IGNORE_UPDATE_ERRORS, false);
        CloudServiceExtended service = createService(cloudServiceNameMapper.mapServiceName(resource, serviceType), serviceType, isOptional,
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
        boolean shouldIgnoreUpdateErrors, Map<String, Object> parameters) throws ContentException {
        if (serviceType.equals(ResourceType.MANAGED_SERVICE)) {
            return createManagedService(serviceName, isOptional, shouldIgnoreUpdateErrors, parameters);
        } else if (serviceType.equals(ResourceType.USER_PROVIDED_SERVICE)) {
            return createUserProvidedService(serviceName, isOptional, shouldIgnoreUpdateErrors, parameters);
        } else if (serviceType.equals(ResourceType.EXISTING_SERVICE)) {
            return createExistingService(serviceName, isOptional, shouldIgnoreUpdateErrors, parameters);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceExtended createManagedService(String serviceName, boolean isOptional, boolean shouldIgnoreUpdateErrors,
        Map<String, Object> parameters) throws ContentException {
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
        Map<String, Object> parameters) throws ContentException {
        Map<String, Object> credentials = getServiceParameters(serviceName, parameters);
        String label = (String) parameters.get(SupportedParameters.SERVICE);
        if (label != null) {
            LOGGER.warn(MessageFormat.format(Messages.IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE, label, serviceName));
        }
        return createCloudService(serviceName, null, null, null, null, null, credentials, Collections.emptyList(), isOptional, true,
            shouldIgnoreUpdateErrors);
    }

    protected CloudServiceExtended createExistingService(String serviceName, boolean isOptional, boolean shouldIgnoreUpdateErrors,
        Map<String, Object> parameters) throws ContentException {
        return createCloudService(serviceName, null, null, null, null, null, Collections.emptyMap(), Collections.emptyList(), isOptional,
            false, shouldIgnoreUpdateErrors);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getServiceParameters(String serviceName, Map<String, Object> parameters) throws ContentException {
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
            serviceParameters.getClass().getSimpleName());
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
