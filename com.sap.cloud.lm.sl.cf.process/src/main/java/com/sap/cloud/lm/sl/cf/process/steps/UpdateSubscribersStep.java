package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.common.util.JsonUtil.convertJsonToList;
import static com.sap.cloud.lm.sl.common.util.JsonUtil.convertJsonToMap;
import static com.sap.cloud.lm.sl.common.util.JsonUtil.toJson;
import static com.sap.cloud.lm.sl.common.util.ListUtil.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.DummyConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.ReferencingPropertiesVisitor;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ModuleDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.RequiredDependencyDto;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.helpers.VisitableObject;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.DeploymentDescriptorParser;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.ModuleParser;
import com.sap.cloud.lm.sl.mta.resolvers.Reference;
import com.sap.cloud.lm.sl.mta.resolvers.ReferencePattern;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

@Component("updateSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSubscribersStep extends SyncActivitiStep {

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

    private static final String DUMMY_VERSION = "1.0.0";

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    protected BiFunction<CloudFoundryOperations, String, Pair<String, String>> orgAndSpaceCalculator = (client,
        spaceId) -> new ClientHelper(client).computeOrgAndSpace(spaceId);

    @Inject
    private ConfigurationSubscriptionDao subscriptionsDao;
    @Inject
    private ConfigurationEntryDao entriesDao;
    @Inject
    private ActivitiFacade activitiFacade;

    @Override
    protected ExecutionStatus executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().info(Messages.UPDATING_SUBSCRIBERS);
            List<ConfigurationEntry> publishedEntries = StepsUtil.getPublishedEntriesFromSubProcesses(execution.getContext(),
                activitiFacade);
            List<ConfigurationEntry> deletedEntries = StepsUtil.getDeletedEntriesFromAllProcesses(execution.getContext(), activitiFacade);
            List<ConfigurationEntry> updatedEntries = merge(publishedEntries, deletedEntries);

            CloudFoundryOperations clientForCurrentSpace = execution.getCloudFoundryClient();

            List<CloudApplication> updatedSubscribers = new ArrayList<>();
            List<CloudApplication> updatedServiceBrokerSubscribers = new ArrayList<>();
            for (ConfigurationSubscription subscription : subscriptionsDao.findAll(updatedEntries)) {
                Pair<String, String> orgAndSpace = orgAndSpaceCalculator.apply(clientForCurrentSpace, subscription.getSpaceId());
                if (orgAndSpace == null) {
                    LOGGER.getLoggerImpl().warn(Messages.COULD_NOT_COMPUTE_ORG_AND_SPACE, subscription.getSpaceId());
                    continue;
                }
                CloudApplication updatedApplication = updateSubscriber(execution, orgAndSpace, subscription);
                if (updatedApplication != null) {
                    addOrgAndSpaceIfNecessary(updatedApplication, orgAndSpace);
                    addApplicationToProperList(updatedSubscribers, updatedServiceBrokerSubscribers, updatedApplication);
                }
            }
            StepsUtil.setUpdatedSubscribers(execution.getContext(), removeDuplicates(updatedSubscribers));
            StepsUtil.setUpdatedServiceBrokerSubscribers(execution.getContext(), updatedServiceBrokerSubscribers);
            getStepLogger().debug(Messages.SUBSCRIBERS_UPDATED);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UPDATING_SUBSCRIBERS);
            throw e;
        }
    }

    private void addApplicationToProperList(List<CloudApplication> updatedSubscribers,
        List<CloudApplication> updatedServiceBrokerSubscribers, CloudApplication updatedApplication) {
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(updatedApplication);

        if (attributesGetter.getAttribute(SupportedParameters.CREATE_SERVICE_BROKER, Boolean.class, false)) {
            updatedServiceBrokerSubscribers.add(updatedApplication);
        } else {
            updatedSubscribers.add(updatedApplication);
        }
    }

    private void addOrgAndSpaceIfNecessary(CloudApplication application, Pair<String, String> orgAndSpace) {
        // The entity returned by the getApplication(String appName) method of
        // the CF Java client does not contain a CloudOrganization,
        // because the value of the 'inline-relations-depth' is hardcoded to 1
        // (see the findApplicationResource method of
        // org.cloudfoundry.client.lib.rest.CloudControllerClientImpl).
        if (application.getSpace() == null || application.getSpace().getOrganization() == null) {
            CloudSpace space = createDummySpace(orgAndSpace);
            application.setSpace(space);
        }
    }

    private CloudSpace createDummySpace(Pair<String, String> orgAndSpace) {
        CloudOrganization org = createDummyOrg(orgAndSpace._1);
        return new CloudSpace(null, orgAndSpace._2, org);
    }

    private CloudOrganization createDummyOrg(String orgName) {
        return new CloudOrganization(null, orgName);
    }

    private List<CloudApplication> removeDuplicates(List<CloudApplication> applications) {
        Map<UUID, CloudApplication> applicationsMap = new LinkedHashMap<>();
        for (CloudApplication application : applications) {
            applicationsMap.put(application.getMeta().getGuid(), application);
        }
        return new ArrayList<>(applicationsMap.values());
    }

    private CloudApplication updateSubscriber(ExecutionWrapper execution, Pair<String, String> orgAndSpace,
        ConfigurationSubscription subscription) {
        String appName = subscription.getAppName();
        String mtaId = subscription.getMtaId();
        String subscriptionName = getRequiredDependency(subscription).getName();
        try {
            return attemptToUpdateSubscriber(execution.getContext(), getClient(execution, orgAndSpace), subscription);
        } catch (CloudFoundryException | SLException e) {
            getStepLogger().warn(e, Messages.COULD_NOT_UPDATE_SUBSCRIBER, appName, mtaId, subscriptionName);
            return null;
        }
    }

    private CloudApplication attemptToUpdateSubscriber(DelegateExecution context, CloudFoundryOperations client,
        ConfigurationSubscription subscription) throws SLException {
        HandlerFactory handlerFactory = new HandlerFactory(MAJOR_SCHEMA_VERSION);

        DeploymentDescriptor dummyDescriptor = buildDummyDescriptor(subscription, handlerFactory);
        getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.DEPLOYMENT_DESCRIPTOR, toJson(dummyDescriptor, true));

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(entriesDao,
            new DummyConfigurationFilterParser(subscription.getFilter()),
            new CloudTarget(StepsUtil.getOrg(context), StepsUtil.getSpace(context)));
        resolver.resolve(dummyDescriptor);
        getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR,
            secureSerializer.toJson(dummyDescriptor));
        dummyDescriptor = handlerFactory.getDescriptorReferenceResolver(dummyDescriptor, new ResolverBuilder(), new ResolverBuilder(),
            new ResolverBuilder()).resolve();
        getStepLogger().debug(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR,
            secureSerializer.toJson(dummyDescriptor));

        ApplicationsCloudModelBuilder appsCloudModelBuilder = handlerFactory.getApplicationsCloudModelBuilder(dummyDescriptor,
            StepsUtil.getCloudBuilderConfiguration(context, shouldUsePrettyPrinting()), null, getEmptySystemParameters(),
            new XsPlaceholderResolver(), "");

        String moduleName = dummyDescriptor.getModules1_0().get(0).getName();
        Set<String> moduleNamesSet = new TreeSet<>(Arrays.asList(moduleName));

        CloudApplicationExtended application = appsCloudModelBuilder.build(moduleNamesSet, moduleNamesSet, Collections.emptySet()).get(0);
        CloudApplication existingApplication = client.getApplication(subscription.getAppName());

        Map<String, String> updatedEnvironment = application.getEnvAsMap();
        Map<String, String> currentEnvironment = existingApplication.getEnvAsMap();

        boolean neededToBeUpdated = updateCurrentEnvironment(currentEnvironment, updatedEnvironment,
            getPropertiesToTransfer(subscription, resolver));

        if (!neededToBeUpdated) {
            return null;
        }

        getStepLogger().info(Messages.UPDATING_SUBSCRIBER, subscription.getAppName(), subscription.getMtaId(),
            getRequiredDependency(subscription).getName());
        client.updateApplicationEnv(existingApplication.getName(), currentEnvironment);
        return existingApplication;
    }

    private List<String> getPropertiesToTransfer(ConfigurationSubscription subscription, ConfigurationReferencesResolver resolver) {
        List<String> result = new ArrayList<>(getFirstComponents(resolver.getExpandedProperties()));
        String listName = getRequiredDependency(subscription).getList();
        if (listName == null) {
            result.addAll(getPropertiesWithReferencesToConfigurationResource(subscription));
            result.addAll(getRequiredDependency(subscription).getProperties().keySet());
        } else {
            result.add(listName);
        }
        return result;
    }

    private List<String> getPropertiesWithReferencesToConfigurationResource(ConfigurationSubscription subscription) {
        ReferenceDetector detector = new ReferenceDetector(getRequiredDependency(subscription).getName());
        new VisitableObject(subscription.getModuleDto().getProperties()).accept(detector);
        return getFirstComponents(detector.getRelevantProperties());
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

    private List<String> getFirstComponents(List<String> properties) {
        return properties.stream().map((propertyName) -> getFirstComponent(propertyName)).collect(Collectors.toList());
    }

    private String getFirstComponent(String propertyName) {
        int index = propertyName.indexOf(ValidatorUtil.DEFAULT_SEPARATOR);
        if (index != -1) {
            return propertyName.substring(0, index);
        }
        return propertyName;
    }

    private RequiredDependencyDto getRequiredDependency(ConfigurationSubscription subscription) {
        return subscription.getModuleDto().getRequiredDependencies().get(0);
    }

    private SystemParameters getEmptySystemParameters() {
        return new SystemParameters(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private CloudFoundryOperations getClient(ExecutionWrapper execution, Pair<String, String> orgAndSpace) throws SLException {
        return execution.getCloudFoundryClient(orgAndSpace._1, orgAndSpace._2);
    }

    private DeploymentDescriptor buildDummyDescriptor(ConfigurationSubscription subscription, HandlerFactory handlerFactory)
        throws SLException {
        ModuleDto moduleDto = subscription.getModuleDto();
        String resourceJson = toJson(subscription.getResourceDto());
        Map<String, Object> resourceMap = convertJsonToMap(resourceJson);

        Map<String, Object> moduleMap = new TreeMap<>();

        moduleMap.put(ModuleParser.NAME, moduleDto.getName());
        moduleMap.put(ModuleParser.TYPE, moduleDto.getName());
        moduleMap.put(ModuleParser.PROPERTIES, moduleDto.getProperties());
        moduleMap.put(ModuleParser.PROVIDES, convertJsonToList(toJson(moduleDto.getProvidedDependencies())));
        moduleMap.put(ModuleParser.REQUIRES, convertJsonToList(toJson(moduleDto.getRequiredDependencies())));

        Map<String, Object> dummyDescriptorMap = new TreeMap<>();
        dummyDescriptorMap.put(DeploymentDescriptorParser.ID, subscription.getMtaId());
        dummyDescriptorMap.put(DeploymentDescriptorParser.MODULES, Arrays.asList(moduleMap));
        dummyDescriptorMap.put(DeploymentDescriptorParser.VERSION, DUMMY_VERSION);
        dummyDescriptorMap.put(DeploymentDescriptorParser.RESOURCES, Arrays.asList(resourceMap));

        return handlerFactory.getDescriptorParser().parseDeploymentDescriptor(dummyDescriptorMap);
    }

    protected boolean shouldUsePrettyPrinting() {
        return true;
    }

    private static class ReferenceDetector extends ReferencingPropertiesVisitor {

        public ReferenceDetector(String name) {
            super(ReferencePattern.FULLY_QUALIFIED, (reference) -> name.equals(reference.getDependencyName()));
        }

        private List<String> relevantProperties = new ArrayList<>();

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
