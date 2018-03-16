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
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.DomainValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ModuleSystemParameterCopier;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.TasksValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.v3_1.VisibilityValidator;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.resolvers.NullPropertiesResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;

public class MtaDescriptorPropertiesResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaDescriptorPropertiesResolver.class);

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade().setFormattedOutput(true);

    private final HandlerFactory handlerFactory;
    private final Platform platform;
    private final Target target;
    private final SystemParameters systemParameters;
    private BiFunction<String, String, String> spaceIdSupplier;
    private final ConfigurationEntryDao dao;
    private final CloudTarget cloudTarget;
    private List<ConfigurationSubscription> subscriptions;

    public MtaDescriptorPropertiesResolver(HandlerFactory handlerFactory, Platform platform, Target target,
        SystemParameters systemParameters, BiFunction<String, String, String> spaceIdSupplier, ConfigurationEntryDao dao,
        CloudTarget cloudTarget) {
        this.handlerFactory = handlerFactory;
        this.platform = platform;
        this.target = target;
        this.systemParameters = systemParameters;
        this.spaceIdSupplier = spaceIdSupplier;
        this.dao = dao;
        this.cloudTarget = cloudTarget;
    }

    public List<ParameterValidator> getValidatorsList() {
        return Arrays.asList(new PortValidator(), new HostValidator(), new DomainValidator(),
            new ModuleSystemParameterCopier(SupportedParameters.APP_NAME, systemParameters), new TasksValidator(),
            new VisibilityValidator());
    }

    public DeploymentDescriptor resolve(DeploymentDescriptor descriptor) throws SLException {
        // Resolve placeholders in parameters:
        descriptor = (DeploymentDescriptor) handlerFactory
            .getDescriptorPlaceholderResolver(descriptor, platform, target, systemParameters, new NullPropertiesResolverBuilder(),
                new ResolverBuilder())
            .resolve();

        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, getValidatorsList())
            .validate();
        LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR_AFTER_PARAMETER_CORRECTION, secureSerializer.toJson(descriptor)));

        // Resolve placeholders in properties:
        descriptor = (DeploymentDescriptor) handlerFactory
            .getDescriptorPlaceholderResolver(descriptor, platform, target, systemParameters, new ResolverBuilder(),
                new NullPropertiesResolverBuilder())
            .resolve();

        DeploymentDescriptor descriptorWithUnresolvedReferences = descriptor.copyOf();

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(descriptor, platform, target,
            spaceIdSupplier, dao, cloudTarget);
        resolver.resolve(descriptor);
        LOGGER.debug(format(Messages.DEPLOYMENT_DESCRIPTOR_AFTER_CROSS_MTA_DEPENDENCY_RESOLUTION, secureSerializer.toJson(descriptor)));
        LOGGER.debug(format(Messages.SUBSCRIPTIONS, secureSerializer.toJson(subscriptions)));

        subscriptions = createSubscriptions(descriptorWithUnresolvedReferences, resolver.getResolvedReferences());

        descriptor = (DeploymentDescriptor) handlerFactory
            .getDescriptorReferenceResolver(descriptor, new ResolverBuilder(), new ResolverBuilder(), new ResolverBuilder())
            .resolve();
        LOGGER.debug(format(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(descriptor)));

        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, getValidatorsList(), true)
            .validate();

        return descriptor;
    }

    private List<ConfigurationSubscription> createSubscriptions(DeploymentDescriptor descriptorWithUnresolvedReferences,
        Map<String, ResolvedConfigurationReference> resolvedResources) {
        Pair<String, String> currentOrgAndSpace = handlerFactory.getOrgAndSpaceHelper(target, platform)
            .getOrgAndSpace();
        String spaceId = spaceIdSupplier.apply(currentOrgAndSpace._1, currentOrgAndSpace._2);
        return handlerFactory.getConfigurationSubscriptionFactory()
            .create(descriptorWithUnresolvedReferences, resolvedResources, spaceId);
    }

    public List<ConfigurationSubscription> getSubscriptions() {
        return subscriptions;
    }

}
