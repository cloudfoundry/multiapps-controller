package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.helpers.ClientHelper;
import org.cloudfoundry.multiapps.controller.core.helpers.DummyConfigurationFilterParser;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.helpers.ReferencingPropertiesVisitor;
import org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationReferencesResolver;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription.ModuleDto;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription.RequiredDependencyDto;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.helpers.VisitableObject;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.parsers.v2.DeploymentDescriptorParser;
import org.cloudfoundry.multiapps.mta.parsers.v2.ModuleParser;
import org.cloudfoundry.multiapps.mta.resolvers.Reference;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencePattern;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.util.NameUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;

@Named("updateSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSubscribersStep extends SyncFlowableStep {

    /*
     * This schema version will be used only for the handling of the subscription entities and it should always be the same as the latest
     * version that is supported by the deploy service, as it is assumed that the latest version of the MTA specification will always
     * support a superset of the features supported by the previous versions.
     * 
     * The major schema version of the MTA that is currently being deployed should NOT be used instead of this one, as problems could occur
     * if the subscriber has a different major schema version. If, for example, the current MTA has a major schema version 1, and the
     * subscriber has a major schema version 2, then this would result in the creation of a handler factory for version 1. That would cause
     * the update of the subscriber to fail, as required dependency entities do not exist in version 1 of the MTA specification and
     * therefore cannot be parsed by the version 1 parser that would be returned by that handler factory.
     */
    // FIXME: Either store the major schema version in the subscriptions table
    // or change this to "3" and verify that everything is
    // working...
    private static final int MAJOR_SCHEMA_VERSION = 2;
    private static final String SCHEMA_VERSION = "2.1.0";

    private static final String DUMMY_VERSION = "1.0.0";

    protected BiFunction<ClientHelper, String, CloudSpace> targetCalculator = ClientHelper::attemptToFindSpace;

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private ConfigurationEntryService configurationEntryService;
    @Inject
    private FlowableFacade flowableFacade;
    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;
    @Inject
    private CloudControllerClientFactory clientFactory;
    @Inject
    private TokenService tokenService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.UPDATING_SUBSCRIBERS);
        List<ConfigurationEntry> publishedEntries = StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacade);
        List<ConfigurationEntry> deletedEntries = StepsUtil.getDeletedEntriesFromAllProcesses(context, flowableFacade);
        List<ConfigurationEntry> updatedEntries = ListUtils.union(publishedEntries, deletedEntries);

        List<CloudApplication> updatedSubscribers = new ArrayList<>();
        List<CloudApplication> updatedServiceBrokerSubscribers = new ArrayList<>();
        List<ConfigurationSubscription> subscriptions = configurationSubscriptionService.createQuery()
                                                                                        .onSelectMatching(updatedEntries)
                                                                                        .list();
        ClientHelper clientHelper = new ClientHelper(createSpaceClient(context));
        for (ConfigurationSubscription subscription : subscriptions) {
            CloudSpace target = targetCalculator.apply(clientHelper, subscription.getSpaceId());
            if (target == null) {
                getStepLogger().warn(Messages.COULD_NOT_COMPUTE_ORG_AND_SPACE, subscription.getSpaceId());
                continue;
            }
            CloudControllerClient client = getClient(context, target);
            CloudApplication subscriberApp = client.getApplication(subscription.getAppName());
            Map<String, String> appEnv = client.getApplicationEnvironment(subscriberApp.getGuid());
            CloudApplication updatedApplication = updateSubscriber(context, subscription, client, subscriberApp, appEnv);
            if (updatedApplication != null) {
                addApplicationToProperList(updatedSubscribers, updatedServiceBrokerSubscribers, updatedApplication, appEnv);
            }
        }
        context.setVariable(Variables.UPDATED_SUBSCRIBERS, removeDuplicates(updatedSubscribers));
        context.setVariable(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS, updatedServiceBrokerSubscribers);
        getStepLogger().debug(Messages.SUBSCRIBERS_UPDATED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_UPDATING_SUBSCRIBERS;
    }

    private CloudSpaceClient createSpaceClient(ProcessContext context) {
        var user = context.getVariable(Variables.USER);
        var token = tokenService.getToken(user);
        return clientFactory.createSpaceClient(token);
    }

    private void addApplicationToProperList(List<CloudApplication> updatedSubscribers,
                                            List<CloudApplication> updatedServiceBrokerSubscribers, CloudApplication updatedApplication,
                                            Map<String, String> appEnv) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(updatedApplication, appEnv);

        if (appAttributes.get(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            updatedServiceBrokerSubscribers.add(updatedApplication);
        } else {
            updatedSubscribers.add(updatedApplication);
        }
    }

    private List<CloudApplication> removeDuplicates(List<CloudApplication> applications) {
        Map<UUID, CloudApplication> applicationsMap = new LinkedHashMap<>();
        for (CloudApplication application : applications) {
            applicationsMap.put(application.getGuid(), application);
        }
        return new ArrayList<>(applicationsMap.values());
    }

    private CloudApplication updateSubscriber(ProcessContext context, ConfigurationSubscription subscription, CloudControllerClient client,
                                              CloudApplication subscriberApp, Map<String, String> appEnv) {
        try {
            return attemptToUpdateSubscriber(context, client, subscription, subscriberApp, appEnv);
        } catch (CloudOperationException | SLException e) {
            String appName = subscription.getAppName();
            String mtaId = subscription.getMtaId();
            String subscriptionName = getRequiredDependency(subscription).getName();
            getStepLogger().warn(e, Messages.COULD_NOT_UPDATE_SUBSCRIBER, appName, mtaId, subscriptionName);
            return null;
        }
    }

    private CloudApplication attemptToUpdateSubscriber(ProcessContext context, CloudControllerClient client,
                                                       ConfigurationSubscription subscription, CloudApplication subscriberApp,
                                                       Map<String, String> appEnv) {
        CloudHandlerFactory handlerFactory = CloudHandlerFactory.forSchemaVersion(MAJOR_SCHEMA_VERSION);

        DeploymentDescriptor dummyDescriptor = buildDummyDescriptor(subscription, handlerFactory);
        getStepLogger().debug(org.cloudfoundry.multiapps.controller.core.Messages.DEPLOYMENT_DESCRIPTOR,
                              SecureSerialization.toJson(dummyDescriptor));

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(configurationEntryService,
                                                                                                     new DummyConfigurationFilterParser(subscription.getFilter()),
                                                                                                     new CloudTarget(context.getVariable(Variables.ORGANIZATION_NAME),
                                                                                                                     context.getVariable(Variables.SPACE_NAME)),
                                                                                                     configuration);
        resolver.resolve(dummyDescriptor);
        getStepLogger().debug(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, SecureSerialization.toJson(dummyDescriptor));
        dummyDescriptor = handlerFactory.getDescriptorReferenceResolver(dummyDescriptor, new ResolverBuilder(), new ResolverBuilder(),
                                                                        new ResolverBuilder(),
                                                                        SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS)
                                        .resolve();
        getStepLogger().debug(Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, SecureSerialization.toJson(dummyDescriptor));

        ApplicationCloudModelBuilder applicationCloudModelBuilder = handlerFactory.getApplicationCloudModelBuilder(dummyDescriptor,
                                                                                                                   shouldUsePrettyPrinting(),
                                                                                                                   null, "",
                                                                                                                   context.getVariable(Variables.MTA_NAMESPACE),
                                                                                                                   getStepLogger(),
                                                                                                                   StepsUtil.getAppSuffixDeterminer(context),
                                                                                                                   client);

        Module module = dummyDescriptor.getModules()
                                       .get(0);

        CloudApplicationExtended application = applicationCloudModelBuilder.build(module, moduleToDeployHelper);

        Map<String, String> updatedEnvironment = application.getEnv();
        Map<String, String> currentEnvironment = new LinkedHashMap<>(appEnv);

        boolean neededToBeUpdated = updateCurrentEnvironment(currentEnvironment, updatedEnvironment,
                                                             getPropertiesToTransfer(subscription, resolver));

        if (!neededToBeUpdated) {
            return null;
        }

        getStepLogger().info(Messages.UPDATING_SUBSCRIBER, subscription.getAppName(), subscription.getMtaId(),
                             getRequiredDependency(subscription).getName());
        client.updateApplicationEnv(subscriberApp.getName(), currentEnvironment);
        return subscriberApp;
    }

    private boolean updateCurrentEnvironment(Map<String, String> currentEnvironment, Map<String, String> updatedEnvironment,
                                             List<String> propertiesToTransfer) {
        boolean neededToBeUpdated = false;
        for (String propertyToTransfer : propertiesToTransfer) {
            String currentProperty = currentEnvironment.get(propertyToTransfer);
            String updatedProperty = updatedEnvironment.get(propertyToTransfer);
            if (!Objects.equals(currentProperty, updatedProperty)) {
                neededToBeUpdated = true;
                currentEnvironment.put(propertyToTransfer, updatedEnvironment.get(propertyToTransfer));
            }
        }
        return neededToBeUpdated;
    }

    private List<String> getPropertiesToTransfer(ConfigurationSubscription subscription, ConfigurationReferencesResolver resolver) {
        List<String> result = new ArrayList<>(getFirstComponents(resolver.getExpandedProperties()));
        String listName = getRequiredDependency(subscription).getList();
        if (listName == null) {
            result.addAll(getPropertiesWithReferencesToConfigurationResource(subscription));
            result.addAll(getRequiredDependency(subscription).getProperties()
                                                             .keySet());
        } else {
            result.add(listName);
        }
        return result;
    }

    private List<String> getPropertiesWithReferencesToConfigurationResource(ConfigurationSubscription subscription) {
        ReferenceDetector detector = new ReferenceDetector(getRequiredDependency(subscription).getName());
        new VisitableObject(subscription.getModuleDto()
                                        .getProperties()).accept(detector);
        return getFirstComponents(detector.getRelevantProperties());
    }

    private RequiredDependencyDto getRequiredDependency(ConfigurationSubscription subscription) {
        return subscription.getModuleDto()
                           .getRequiredDependencies()
                           .get(0);
    }

    private List<String> getFirstComponents(List<String> properties) {
        return properties.stream()
                         .map(this::getFirstComponent)
                         .collect(Collectors.toList());
    }

    private String getFirstComponent(String propertyName) {
        int index = propertyName.indexOf(NameUtil.DEFAULT_PREFIX_SEPARATOR);
        if (index != -1) {
            return propertyName.substring(0, index);
        }
        return propertyName;
    }

    private CloudControllerClient getClient(ProcessContext context, CloudSpace space) {
        return context.getControllerClient(space.getGuid()
                                                .toString());
    }

    private DeploymentDescriptor buildDummyDescriptor(ConfigurationSubscription subscription, CloudHandlerFactory handlerFactory) {
        ModuleDto moduleDto = subscription.getModuleDto();
        String resourceJson = JsonUtil.toJson(subscription.getResourceDto());
        Map<String, Object> resourceMap = JsonUtil.convertJsonToMap(resourceJson);

        Map<String, Object> moduleMap = new TreeMap<>();

        moduleMap.put(ModuleParser.NAME, moduleDto.getName());
        moduleMap.put(ModuleParser.TYPE, moduleDto.getName());
        moduleMap.put(ModuleParser.PROPERTIES, moduleDto.getProperties());
        moduleMap.put(ModuleParser.PROVIDES, JsonUtil.convertJsonToList(JsonUtil.toJson(moduleDto.getProvidedDependencies())));
        moduleMap.put(ModuleParser.REQUIRES, JsonUtil.convertJsonToList(JsonUtil.toJson(moduleDto.getRequiredDependencies())));

        Map<String, Object> dummyDescriptorMap = new TreeMap<>();
        dummyDescriptorMap.put(DeploymentDescriptorParser.SCHEMA_VERSION, SCHEMA_VERSION);
        dummyDescriptorMap.put(DeploymentDescriptorParser.ID, subscription.getMtaId());
        dummyDescriptorMap.put(DeploymentDescriptorParser.MODULES, Collections.singletonList(moduleMap));
        dummyDescriptorMap.put(DeploymentDescriptorParser.VERSION, DUMMY_VERSION);
        dummyDescriptorMap.put(DeploymentDescriptorParser.RESOURCES, Collections.singletonList(resourceMap));

        return handlerFactory.getDescriptorParser()
                             .parseDeploymentDescriptor(dummyDescriptorMap);
    }

    protected boolean shouldUsePrettyPrinting() {
        return true;
    }

    private static class ReferenceDetector extends ReferencingPropertiesVisitor {

        public ReferenceDetector(String name) {
            super(ReferencePattern.FULLY_QUALIFIED, reference -> name.equals(reference.getDependencyName()));
        }

        private final List<String> relevantProperties = new ArrayList<>();

        @Override
        protected Object visit(String key, String value, List<Reference> references) {
            relevantProperties.add(key);
            return value;
        }

        public List<String> getRelevantProperties() {
            return relevantProperties;
        }

    }

}
