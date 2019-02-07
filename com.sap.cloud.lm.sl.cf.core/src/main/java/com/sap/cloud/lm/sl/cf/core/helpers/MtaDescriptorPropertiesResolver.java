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
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ApplicationNameValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.DomainValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RestartOnEnvChangeValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RoutesValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ServiceNameValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.TasksValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v3.VisibilityValidator;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.NullPropertiesResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;

public class MtaDescriptorPropertiesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaDescriptorPropertiesResolver.class);

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade().setFormattedOutput(true);

    private final HandlerFactory handlerFactory;
    private BiFunction<String, String, String> spaceIdSupplier;
    private final ConfigurationEntryDao dao;
    private final CloudTarget cloudTarget;
    private List<ConfigurationSubscription> subscriptions;
    private final ApplicationConfiguration configuration;
    private final boolean useNamespaces;
    private final boolean useNamespacesForServices;

    public MtaDescriptorPropertiesResolver(HandlerFactory handlerFactory, BiFunction<String, String, String> spaceIdSupplier,
        ConfigurationEntryDao dao, CloudTarget cloudTarget, ApplicationConfiguration configuration, boolean useNamespaces,
        boolean useNamespacesForServices) {
        this.handlerFactory = handlerFactory;
        this.spaceIdSupplier = spaceIdSupplier;
        this.dao = dao;
        this.cloudTarget = cloudTarget;
        this.configuration = configuration;
        this.useNamespaces = useNamespaces;
        this.useNamespacesForServices = useNamespacesForServices;
    }

    public List<ParameterValidator> getValidatorsList() {
        return Arrays.asList(new PortValidator(), new HostValidator(), new DomainValidator(), new RoutesValidator(), new TasksValidator(),
            new VisibilityValidator(), new RestartOnEnvChangeValidator());
    }

    public DeploymentDescriptor resolve(DeploymentDescriptor descriptor) {
        descriptor = correctEntityNames(descriptor);
        // Resolve placeholders in parameters:
        descriptor = handlerFactory
            .getDescriptorPlaceholderResolver(descriptor, new NullPropertiesResolverBuilder(), new ResolverBuilder(),
                SupportedParameters.SINGULAR_PLURAL_MAPPING)
            .resolve();

        List<ParameterValidator> validatorsList = getValidatorsList();
        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, validatorsList)
            .validate();
        LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR_AFTER_PARAMETER_CORRECTION, secureSerializer.toJson(descriptor)));

        // Resolve placeholders in properties:
        descriptor = handlerFactory
            .getDescriptorPlaceholderResolver(descriptor, new ResolverBuilder(), new NullPropertiesResolverBuilder(),
                SupportedParameters.SINGULAR_PLURAL_MAPPING)
            .resolve();

        DeploymentDescriptor descriptorWithUnresolvedReferences = descriptor.copyOf();

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(descriptor, spaceIdSupplier, dao,
            cloudTarget, configuration);
        resolver.resolve(descriptor);
        LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR_AFTER_CROSS_MTA_DEPENDENCY_RESOLUTION, secureSerializer.toJson(descriptor)));

        subscriptions = createSubscriptions(descriptorWithUnresolvedReferences, resolver.getResolvedReferences());
        LOGGER.debug(format(Messages.SUBSCRIPTIONS, secureSerializer.toJson(subscriptions)));

        descriptor = handlerFactory
            .getDescriptorReferenceResolver(descriptor, new ResolverBuilder(), new ResolverBuilder(), new ResolverBuilder())
            .resolve();
        LOGGER.debug(format(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(descriptor)));

        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, validatorsList, true)
            .validate();

        return descriptor;
    }

    private DeploymentDescriptor correctEntityNames(DeploymentDescriptor descriptor) {
        List<ParameterValidator> correctors = Arrays.asList(new ApplicationNameValidator(descriptor.getId(), useNamespaces),
            new ServiceNameValidator(descriptor.getId(), useNamespaces, useNamespacesForServices));
        return handlerFactory.getDescriptorParametersValidator(descriptor, correctors)
            .validate();
    }

    private List<ConfigurationSubscription> createSubscriptions(DeploymentDescriptor descriptorWithUnresolvedReferences,
        Map<String, ResolvedConfigurationReference> resolvedResources) {
        String spaceId = spaceIdSupplier.apply(cloudTarget.getOrg(), cloudTarget.getSpace());
        return handlerFactory.getConfigurationSubscriptionFactory()
            .create(descriptorWithUnresolvedReferences, resolvedResources, spaceId);
    }

    public List<ConfigurationSubscription> getSubscriptions() {
        return subscriptions;
    }

}
