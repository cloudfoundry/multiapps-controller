package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationReferencesResolver;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.MtaDescriptorPropertiesResolverContext;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ApplicationNameValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.DomainValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.HostValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.IdleRoutesValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParameterValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RestartOnEnvChangeValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ServiceNameValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.TasksValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.v3.VisibilityValidator;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.resolvers.NullPropertiesResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencesUnescaper;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;

public class MtaDescriptorPropertiesResolver {

    public static final String IDLE_DOMAIN_PLACEHOLDER = "${" + SupportedParameters.IDLE_DOMAIN + "}";
    public static final String IDLE_HOST_PLACEHOLDER = "${" + SupportedParameters.IDLE_HOST + "}";

    private final MtaDescriptorPropertiesResolverContext context;
    private List<ConfigurationSubscription> subscriptions;
    private Set<DynamicResolvableParameter> dynamicResolvableParameters;

    public MtaDescriptorPropertiesResolver(MtaDescriptorPropertiesResolverContext context) {
        this.context = context;
    }

    public List<ParameterValidator> getValidatorsList() {
        return List.of(new HostValidator(), new DomainValidator(), new RoutesValidator(context.getNamespace(), context.applyNamespace()),
                       new IdleRoutesValidator(context.getNamespace(), context.applyNamespace()), new TasksValidator(),
                       new VisibilityValidator(), new RestartOnEnvChangeValidator());
    }

    public DeploymentDescriptor resolve(DeploymentDescriptor descriptor) {
        descriptor = correctEntityNames(descriptor);
        // Resolve placeholders in parameters:
        CloudHandlerFactory handlerFactory = context.getHandlerFactory();
        descriptor = handlerFactory.getDescriptorPlaceholderResolver(descriptor, new NullPropertiesResolverBuilder(), new ResolverBuilder(),
                                                                     SupportedParameters.SINGULAR_PLURAL_MAPPING,
                                                                     SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS)
                                   .resolve();

        if (context.shouldReserveTemporaryRoute()) {
            // temporary placeholders should be set at this point, since they are needed for provides/requires placeholder resolution
            editRoutesSetTemporaryPlaceholders(descriptor);

            // Resolve again due to new temporary routes
            descriptor = handlerFactory.getDescriptorPlaceholderResolver(descriptor, new NullPropertiesResolverBuilder(),
                                                                         new ResolverBuilder(), SupportedParameters.SINGULAR_PLURAL_MAPPING,
                                                                         SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS)
                                       .resolve();
        }

        List<ParameterValidator> validatorsList = getValidatorsList();
        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, validatorsList)
                                   .validate();

        // Resolve placeholders in properties:
        descriptor = handlerFactory.getDescriptorPlaceholderResolver(descriptor, new ResolverBuilder(), new NullPropertiesResolverBuilder(),
                                                                     SupportedParameters.SINGULAR_PLURAL_MAPPING,
                                                                     SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS)
                                   .resolve();

        DeploymentDescriptor descriptorWithUnresolvedReferences = DeploymentDescriptor.copyOf(descriptor);

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(descriptor,
                                                                                                     context.getConfigurationEntryService(),
                                                                                                     context.getCloudTarget(),
                                                                                                     context.getApplicationConfiguration(),
                                                                                                     context.getNamespace());

        resolver.resolve(descriptor);

        subscriptions = createSubscriptions(descriptorWithUnresolvedReferences, resolver.getResolvedReferences(),
                                            SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS);

        descriptor = handlerFactory.getDescriptorReferenceResolver(descriptor, new ResolverBuilder(), new ResolverBuilder(),
                                                                   new ResolverBuilder(), SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS)
                                   .resolve();

        descriptor = handlerFactory.getDescriptorParametersValidator(descriptor, validatorsList, true)
                                   .validate();

        dynamicResolvableParameters = createDynamicResolvableParameters(descriptor);

        unescapeEscapedReferences(descriptor);

        return descriptor;
    }

    private void unescapeEscapedReferences(DeploymentDescriptor descriptor) {
        new ReferencesUnescaper().unescapeReferences(descriptor);
    }

    private DeploymentDescriptor correctEntityNames(DeploymentDescriptor descriptor) {
        List<ParameterValidator> correctors = Arrays.asList(new ApplicationNameValidator(context.getNamespace(), context.applyNamespace()),
                                                            new ServiceNameValidator(context.getNamespace(), context.applyNamespace()));
        return context.getHandlerFactory()
                      .getDescriptorParametersValidator(descriptor, correctors)
                      .validate();
    }

    private void editRoutesSetTemporaryPlaceholders(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules()) {
            Map<String, Object> moduleParameters = module.getParameters();

            List<Map<String, Object>> routes = RoutesValidator.applyRoutesType(moduleParameters.get(SupportedParameters.ROUTES));

            for (Map<String, Object> routeMap : routes) {
                Object routeValue = routeMap.get(SupportedParameters.ROUTE);
                boolean noHostname = MapUtil.parseBooleanFlag(routeMap, SupportedParameters.NO_HOSTNAME, false);
                String protocol = (String) routeMap.get(SupportedParameters.ROUTE_PROTOCOL);
                if (routeValue instanceof String) {
                    routeMap.put(SupportedParameters.ROUTE, replacePartsWithIdlePlaceholders((String) routeValue, noHostname, protocol));
                }

                if (routeMap.containsKey(SupportedParameters.NO_HOSTNAME)) {
                    // remove no-hostname value, since it will be ignored for idle routes
                    routeMap.remove(SupportedParameters.NO_HOSTNAME);
                }
            }
        }
    }

    private String replacePartsWithIdlePlaceholders(String uriString, boolean noHostname, String protocol) {
        ApplicationURI uri = new ApplicationURI(uriString, noHostname, protocol);
        uri.setDomain(IDLE_DOMAIN_PLACEHOLDER);
        uri.setHost(IDLE_HOST_PLACEHOLDER);
        return uri.toString();
    }

    private List<ConfigurationSubscription> createSubscriptions(DeploymentDescriptor descriptorWithUnresolvedReferences,
                                                                Map<String, ResolvedConfigurationReference> resolvedResources,
                                                                Set<String> dynamicResolvableParameters) {
        return context.getHandlerFactory()
                      .getConfigurationSubscriptionFactory(descriptorWithUnresolvedReferences, resolvedResources, dynamicResolvableParameters)
                      .create(context.getCurrentSpaceId());
    }

    private Set<DynamicResolvableParameter> createDynamicResolvableParameters(DeploymentDescriptor descriptor) {
        return context.getHandlerFactory()
                      .getDynamicResolvableParameterFactory(descriptor)
                      .create();
    }

    public List<ConfigurationSubscription> getSubscriptions() {
        return subscriptions;
    }

    public Set<DynamicResolvableParameter> getDynamicResolvableParameters() {
        return dynamicResolvableParameters;
    }

}
