package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.getServiceType;
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

    public ServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, PropertiesAccessor propertiesAccessor,
        CloudModelConfiguration configuration) {
        this.propertiesAccessor = propertiesAccessor;
        this.deploymentDescriptor = deploymentDescriptor;
        this.cloudServiceNameMapper = new CloudServiceNameMapper(configuration, propertiesAccessor, deploymentDescriptor);
    }

    public List<CloudServiceExtended> build(Set<String> modules) throws SLException {
        List<CloudServiceExtended> services = new ArrayList<>();
        for (Resource resource : deploymentDescriptor.getResources1_0()) {
            if (isService(resource)) {
                ListUtil.addNonNull(services, getService(resource));
            }
        }
        return services;
    }

    protected CloudServiceExtended getService(Resource resource) throws SLException {
        Map<String, Object> parameters = propertiesAccessor.getParameters(resource);
        ServiceType serviceType = getServiceType(parameters);
        boolean isOptionalResource = isOptionalResource(resource);
        CloudServiceExtended service = createService(parameters, cloudServiceNameMapper.mapServiceName(resource, serviceType),
            isOptionalResource, serviceType);
        if (service != null) {
            service.setResourceName(resource.getName());
        }
        return service;
    }

    protected boolean isOptionalResource(Resource resource) {
        return false;
    }

    protected CloudServiceExtended createService(Map<String, Object> properties, String serviceName, boolean isOptionalResource,
        ServiceType serviceType) throws ContentException {
        if (serviceType.equals(ServiceType.MANAGED)) {
            return createManagedService(serviceName, isOptionalResource, properties);
        } else if (serviceType.equals(ServiceType.USER_PROVIDED)) {
            return createUserProvidedService(serviceName, isOptionalResource, properties);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected CloudServiceExtended createManagedService(String serviceName, boolean isOptional, Map<String, Object> properties)
        throws ContentException {
        String label = (String) properties.get(SupportedParameters.SERVICE);
        List<String> serviceAlternatives = (List<String>) properties.getOrDefault(SupportedParameters.SERVICE_ALTERNATIVES,
            Collections.emptyList());
        String plan = (String) properties.get(SupportedParameters.SERVICE_PLAN);
        String provider = (String) properties.get(SupportedParameters.SERVICE_PROVIDER);
        String version = (String) properties.get(SupportedParameters.SERVICE_VERSION);
        List<String> serviceTags = (List<String>) properties.getOrDefault(SupportedParameters.SERVICE_TAGS, Collections.emptyList());
        Map<String, Object> credentials = getServiceConfigParameters(properties, serviceName);

        return createCloudService(serviceName, label, serviceAlternatives, plan, provider, version, isOptional, credentials, serviceTags);
    }

    protected CloudServiceExtended createUserProvidedService(String serviceName, boolean isOptional, Map<String, Object> properties)
        throws ContentException {
        Map<String, Object> credentials = getServiceConfigParameters(properties, serviceName);
        String label = (String) properties.get(SupportedParameters.SERVICE);
        if (label != null) {
            LOGGER.warn(MessageFormat.format(Messages.IGNORING_LABEL_FOR_USER_PROVIDED_SERVICE, label, serviceName));
        }
        return createCloudService(serviceName, null, null, null, null, null, isOptional, credentials, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getServiceConfigParameters(Map<String, Object> properties, String serviceName) throws ContentException {
        Object serviceConfig = properties.get(SupportedParameters.SERVICE_CONFIG);
        if (serviceConfig == null) {
            return Collections.emptyMap();
        }
        if (!(serviceConfig instanceof Map)) {
            throw new ContentException(getInvalidServiceConfigTypeErrorMessage(serviceName, serviceConfig));
        }
        return new TreeMap<>((Map<String, Object>) serviceConfig);
    }

    protected String getInvalidServiceConfigTypeErrorMessage(String serviceName, Object serviceConfig) {
        return MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_TYPE_FOR_KEY,
            ValidatorUtil.getPrefixedName(serviceName, SupportedParameters.SERVICE_CONFIG), Map.class.getSimpleName(),
            serviceConfig.getClass().getSimpleName());
    }

    protected CloudServiceExtended createCloudService(String name, String label, List<String> serviceAlternatives, String plan,
        String provider, String version, boolean isOptional, Map<String, Object> credentials, List<String> serviceTags) {
        CloudServiceExtended service = new CloudServiceExtended(null, name);
        service.setLabel(label);
        service.setServiceAlternatives(serviceAlternatives);
        service.setPlan(plan);
        service.setProvider(provider);
        service.setVersion(version);
        service.setCredentials(credentials);
        service.setTags(serviceTags);
        service.setOptional(isOptional);
        return service;
    }
}
