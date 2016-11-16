package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.common.util.JsonUtil.convertJsonToList;
import static com.sap.cloud.lm.sl.common.util.JsonUtil.convertJsonToMap;
import static com.sap.cloud.lm.sl.common.util.JsonUtil.toJson;
import static com.sap.cloud.lm.sl.common.util.ListUtil.merge;
import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.DummyConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.helpers.ReferencingPropertiesVisitor;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolver;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ModuleDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.RequiredDependencyDto;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
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
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("updateSubscribersStep")
public class UpdateSubscribersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSubscribersStep.class);

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
    private static final int MAJOR_SCHEMA_VERSION = 2;

    private static final String DUMMY_VERSION = "1.0.0";

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    public static StepMetadata getMetadata() {
        return new StepMetadata("updateSubscribersTask", "Update Subscribers", "Update Subscribers");
    }

    @Inject
    private ConfigurationSubscriptionDao subscriptionsDao;
    @Inject
    private ConfigurationEntryDao entriesDao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.UPDATING_SUBSCRIBERS, LOGGER);
            List<ConfigurationEntry> publishedEntries = StepsUtil.getPublishedEntries(context);
            List<ConfigurationEntry> deletedEntries = StepsUtil.getDeletedEntries(context);
            List<ConfigurationEntry> updatedEntries = merge(publishedEntries, deletedEntries);

            boolean noRestartSubscribedApps = ContextUtil.getVariable(context, Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS, false);
            CloudFoundryOperations currentSpaceClient = getCloudFoundryClient(context, LOGGER);

            for (ConfigurationSubscription subscription : subscriptionsDao.findAll(updatedEntries)) {
                String appName = subscription.getAppName();
                String mtaId = subscription.getMtaId();
                try {
                    updateSubscriber(subscription, getClient(subscription, currentSpaceClient, context), noRestartSubscribedApps, context);
                } catch (CloudFoundryException | SLException e) {
                    warn(context, format(Messages.COULD_NOT_UPDATE_SUBSCRIBER, appName, mtaId), e, LOGGER);
                }
            }
            debug(context, Messages.SUBSCRIBERS_UPDATED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_UPDATING_SUBSCRIBERS, e, LOGGER);
            throw e;
        }
    }

    private void updateSubscriber(ConfigurationSubscription subscription, CloudFoundryOperations client, boolean doNotRestartSubscribers,
        DelegateExecution context) throws SLException {
        ClientExtensions clientExtensions = null;
        if (client instanceof ClientExtensions) {
            clientExtensions = (ClientExtensions) client;
        }

        HandlerFactory handlerFactory = new HandlerFactory(MAJOR_SCHEMA_VERSION);

        DeploymentDescriptor dummyDescriptor = buildDummyDescriptor(subscription, handlerFactory);
        debug(context, format(com.sap.cloud.lm.sl.cf.core.message.Messages.DEPLOYMENT_DESCRIPTOR, toJson(dummyDescriptor, true)), LOGGER);

        ConfigurationReferencesResolver resolver = handlerFactory.getConfigurationReferencesResolver(entriesDao,
            new DummyConfigurationFilterParser(subscription.getFilter()));
        resolver.resolve(dummyDescriptor);
        debug(context,
            format(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(dummyDescriptor)),
            LOGGER);
        dummyDescriptor = handlerFactory.getDescriptorReferenceResolver(dummyDescriptor).resolve();
        debug(context,
            format(com.sap.cloud.lm.sl.cf.core.message.Messages.RESOLVED_DEPLOYMENT_DESCRIPTOR, secureSerializer.toJson(dummyDescriptor)),
            LOGGER);

        CloudModelBuilder cloudModelBuilder = handlerFactory.getCloudModelBuilder(dummyDescriptor, getEmptySystemParameters(), false,
            shouldUsePretttyPrinting(), false, false, false, "", new XsPlaceholderResolver());

        String moduleName = dummyDescriptor.getModules1_0().get(0).getName();
        Set<String> moduleNamesSet = new TreeSet<>(Arrays.asList(moduleName));

        CloudApplicationExtended application = cloudModelBuilder.getApplications(moduleNamesSet, moduleNamesSet,
            Collections.emptySet()).get(0);
        CloudApplication existingApplication = client.getApplication(subscription.getAppName());

        Map<String, String> updatedEnvironment = application.getEnvAsMap();
        Map<String, String> currentEnvironment = existingApplication.getEnvAsMap();

        boolean neededToBeUpdated = updateCurrentEnvironment(currentEnvironment, updatedEnvironment,
            getPropertiesToTransfer(subscription, resolver));

        if (!neededToBeUpdated) {
            return;
        }

        info(context, format(Messages.UPDATING_SUBSCRIBER, subscription.getAppName(), subscription.getMtaId()), LOGGER);
        client.updateApplicationEnv(existingApplication.getName(), currentEnvironment);

        if (doNotRestartSubscribers) {
            return;
        }

        info(context, format(Messages.STOPPING_APP, existingApplication.getName()), LOGGER);
        client.stopApplication(existingApplication.getName());
        info(context, format(Messages.STARTING_APP, existingApplication.getName()), LOGGER);
        if (clientExtensions != null) {
            clientExtensions.startApplication(existingApplication.getName(), false);
        } else {
            client.startApplication(existingApplication.getName());
        }
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

    private CloudFoundryOperations getClient(ConfigurationSubscription subscription, CloudFoundryOperations client,
        DelegateExecution context) throws SLException {
        Pair<String, String> orgAndSpace = computeOrgAndSpace(subscription.getSpaceId(), client);
        if (orgAndSpace == null) {
            throw new SLException(Messages.COULD_NOT_COMPUTE_ORG_AND_SPACE, subscription.getSpaceId());
        }
        return getCloudFoundryClient(context, LOGGER, orgAndSpace._1, orgAndSpace._2);
    }

    protected Pair<String, String> computeOrgAndSpace(String spaceIdd, CloudFoundryOperations client) {
        for (CloudSpace space : client.getSpaces()) {
            if (space.getMeta().getGuid().equals(UUID.fromString(spaceIdd))) {
                return new Pair<>(space.getOrganization().getName(), space.getName());
            }
        }
        return null;
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

    protected boolean shouldUsePretttyPrinting() {
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
