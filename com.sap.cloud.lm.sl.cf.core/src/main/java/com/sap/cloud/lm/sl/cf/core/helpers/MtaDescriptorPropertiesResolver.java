package com.sap.cloud.lm.sl.cf.core.helpers;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.DomainValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ModuleSystemParameterCopier;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;
import com.sap.cloud.lm.sl.mta.resolvers.NullPropertiesPlaceholderResolver;
import com.sap.cloud.lm.sl.mta.resolvers.PropertiesPlaceholderResolver;

public class MtaDescriptorPropertiesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaDescriptorPropertiesResolver.class);

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade().setFormattedOutput(true);

    private final HandlerFactory handlerFactory;
    private final TargetPlatformType platformType;
    private final TargetPlatform platform;
    private final SystemParameters systemParameters;
    private BiFunction<String, String, String> spaceIdSupplier;
    private final ConfigurationEntryDao dao;

    private List<ConfigurationSubscription> subscriptions;

    public MtaDescriptorPropertiesResolver(HandlerFactory handlerFactory, TargetPlatformType platformType, TargetPlatform platform,
        SystemParameters systemParameters, BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao) {
        this.handlerFactory = handlerFactory;
        this.platformType = platformType;
        this.platform = platform;
        this.systemParameters = systemParameters;
        this.spaceIdSupplier = spaceIdSupplier;
        this.dao = dao;
    }

    public DeploymentDescriptor resolve(DeploymentDescriptor descriptor) throws SLException {
        // Resolve placeholders in parameters:
        descriptor = (DeploymentDescriptor) handlerFactory.getDescriptorPlaceholderResolver(descriptor, platformType, platform,
            systemParameters, new NullPropertiesPlaceholderResolver(), new PropertiesPlaceholderResolver()).resolve();

        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, Arrays.asList(new PortValidator(), new HostValidator(),
            new DomainValidator(), new ModuleSystemParameterCopier(SupportedParameters.APP_NAME, systemParameters))).validate();
        LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR_AFTER_PARAMETER_CORRECTION, secureSerializer.toJson(descriptor)));

        // Resolve placeholders in properties:
        descriptor = (DeploymentDescriptor) handlerFactory.getDescriptorPlaceholderResolver(descriptor, platformType, platform,
            systemParameters, new PropertiesPlaceholderResolver(), new NullPropertiesPlaceholderResolver()).resolve();

        DeploymentDescriptor descriptorWithUnresolvedReferences = descriptor.copyOf();

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(descriptor, platformType, platform,
            spaceIdSupplier, dao);
        resolver.resolve(descriptor);
        LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR_AFTER_CROSS_MTA_DEPENDENCY_RESOLUTION, secureSerializer.toJson(descriptor)));
        LOGGER.debug(format(Messages.SUBSCRIPTIONS, secureSerializer.toJson(subscriptions)));

        subscriptions = createSubscriptions(descriptorWithUnresolvedReferences, resolver.getResolvedReferences());

        descriptor = (DeploymentDescriptor) handlerFactory.getDescriptorReferenceResolver(descriptor).resolve();
        LOGGER.debug(format(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(descriptor)));

        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, Arrays.asList(new PortValidator(), new HostValidator(),
            new DomainValidator(), new ModuleSystemParameterCopier(SupportedParameters.APP_NAME, systemParameters)), true).validate();

        return descriptor;
    }

    private List<ConfigurationSubscription> createSubscriptions(DeploymentDescriptor descriptorWithUnresolvedReferences,
        Map<String, ResolvedConfigurationReference> resolvedResources) {
        Pair<String, String> currentOrgAndSpace = handlerFactory.getOrgAndSpaceHelper(platform, platformType).getOrgAndSpace();
        String spaceId = spaceIdSupplier.apply(currentOrgAndSpace._1, currentOrgAndSpace._2);
        return handlerFactory.getConfigurationSubscriptionFactory().create(descriptorWithUnresolvedReferences, resolvedResources, spaceId);
    }

    public List<ConfigurationSubscription> getSubscriptions() {
        return subscriptions;
    }

}
