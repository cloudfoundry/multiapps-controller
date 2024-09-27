package org.cloudfoundry.multiapps.controller.process.client;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.process.Messages;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.ServiceBindingOperationCallback;
import com.sap.cloudfoundry.client.facade.UploadStatusCallback;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudProcess;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceOffering;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.ServicePlanVisibility;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.Upload;
import com.sap.cloudfoundry.client.facade.domain.UserRole;
import com.sap.cloudfoundry.client.facade.dto.ApplicationToCreateDto;

public class LoggingCloudControllerClient implements CloudControllerClient {

    private final CloudControllerClient delegate;
    private final UserMessageLogger logger;

    public LoggingCloudControllerClient(CloudControllerClient delegate, UserMessageLogger logger) {
        this.delegate = delegate;
        this.logger = logger;
    }

    @Override
    public CloudSpace getTarget() {
        return delegate.getTarget();
    }

    @Override
    public void addDomain(String domainName) {
        logger.debug(Messages.ADDING_DOMAIN_0, domainName);
        delegate.addDomain(domainName);
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        logger.debug(Messages.ADDING_ROUTE_WITH_HOST_0_DOMAIN_1_AND_PATH_2, host, domainName, path);
        delegate.addRoute(host, domainName, path);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        logger.debug(Messages.BINDING_SERVICE_INSTANCE_0_TO_APPLICATION_1_WITH_BINDING_NAME_2, serviceInstanceName, applicationName,
                     bindingName);
        return delegate.bindServiceInstance(bindingName, applicationName, serviceInstanceName);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters, ApplicationServicesUpdateCallback updateServicesCallback) {
        logger.debug(Messages.BINDING_SERVICE_INSTANCE_0_TO_APPLICATION_1_WITH_PARAMETERS_2, serviceInstanceName, applicationName,
                     SecureSerialization.toJson(parameters));
        return delegate.bindServiceInstance(bindingName, applicationName, serviceInstanceName, parameters, updateServicesCallback);
    }

    @Override
    public void createApplication(ApplicationToCreateDto applicationToCreateDto) {
        logger.debug(Messages.CREATING_APPLICATION_0_WITH_DISK_1_MEMORY_2_URIS_3_AND_STAGING_4, applicationToCreateDto.getName(),
                     applicationToCreateDto.getDiskQuotaInMb(), applicationToCreateDto.getMemoryInMb(),
                     UriUtil.prettyPrintRoutes(applicationToCreateDto.getRoutes()),
                     SecureSerialization.toJson(applicationToCreateDto.getStaging()));
        delegate.createApplication(applicationToCreateDto);
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.CREATING_SERVICE_INSTANCE_0, SecureSerialization.toJson(serviceInstance));
        delegate.createServiceInstance(serviceInstance);
    }

    @Override
    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        logger.debug(Messages.CREATING_SERVICE_BROKER_0, SecureSerialization.toJson(serviceBroker));
        return delegate.createServiceBroker(serviceBroker);
    }

    @Override
    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        logger.debug(Messages.CREATING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1_WITH_PARAMETERS_2, keyModel.getName(), serviceInstanceName,
                     SecureSerialization.toJson(keyModel.getCredentials()));
        return delegate.createAndFetchServiceKey(keyModel, serviceInstanceName);
    }

    @Override
    public Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        logger.debug(Messages.CREATING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1_WITH_PARAMETERS_2, keyModel.getName(), serviceInstanceName,
                     SecureSerialization.toJson(keyModel.getCredentials()));
        return delegate.createServiceKey(keyModel, serviceInstanceName);
    }

    @Override
    public Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        logger.debug(Messages.CREATING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1_WITH_PARAMETERS_2, serviceKeyName, serviceInstanceName,
                     SecureSerialization.toJson(parameters));
        return delegate.createServiceKey(serviceInstanceName, serviceKeyName, parameters);
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.CREATING_USER_PROVIDED_SERVICE_INSTANCE_0, SecureSerialization.toJson(serviceInstance));
        delegate.createUserProvidedServiceInstance(serviceInstance);
    }

    @Override
    public void deleteApplication(String applicationName) {
        logger.debug(Messages.DELETING_APPLICATION_0, applicationName);
        delegate.deleteApplication(applicationName);
    }

    @Override
    public void deleteDomain(String domainName) {
        logger.debug(Messages.DELETING_DOMAIN_0, domainName);
        delegate.deleteDomain(domainName);
    }

    @Override
    public void deleteOrphanedRoutes() {
        logger.debug(Messages.DELETING_ORPHANED_ROUTES);
        delegate.deleteOrphanedRoutes();
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        logger.debug(Messages.DELETING_ROUTE_WITH_HOST_0_DOMAIN_1_AND_PATH_2, host, domainName, path);
        delegate.deleteRoute(host, domainName, path);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        logger.debug(Messages.DELETING_SERVICE_INSTANCE_0, serviceInstanceName);
        delegate.deleteServiceInstance(serviceInstanceName);
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.DELETING_SERVICE_INSTANCE_0, serviceInstance.getName());
        delegate.deleteServiceInstance(serviceInstance);
    }

    @Override
    public String deleteServiceBroker(String name) {
        logger.debug(Messages.DELETING_SERVICE_BROKER_0, name);
        return delegate.deleteServiceBroker(name);
    }

    @Override
    public CloudServiceBinding getServiceBinding(UUID serviceBindingId) {
        logger.debug(Messages.GETTING_SERVICE_BINDING_BY_GUID_0, serviceBindingId);
        return delegate.getServiceBinding(serviceBindingId);
    }

    @Override
    public Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName) {
        logger.debug(Messages.DELETING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1, serviceKeyName, serviceInstanceName);
        return delegate.deleteServiceBinding(serviceInstanceName, serviceKeyName);
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid, ServiceBindingOperationCallback serviceBindingOperationCallback) {
        logger.debug(Messages.DELETING_SERVICE_BINDING_0, bindingGuid.toString());
        return delegate.deleteServiceBinding(bindingGuid, serviceBindingOperationCallback);
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid) {
        logger.debug(Messages.DELETING_SERVICE_BINDING_0, bindingGuid.toString());
        return delegate.deleteServiceBinding(bindingGuid);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        logger.debug(Messages.GETTING_APPLICATION_0, applicationName);
        return delegate.getApplication(applicationName);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        logger.debug(Messages.GETTING_APPLICATION_0, applicationName);
        return delegate.getApplication(applicationName, required);
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        logger.debug(Messages.GETTING_APPLICATION_0_GUID, applicationName);
        return delegate.getApplicationGuid(applicationName);
    }

    @Override
    public String getApplicationName(UUID uuid) {
        logger.debug(Messages.GETTING_NAME_OF_APPLICATION_WITH_GUID_0, uuid.toString());
        return delegate.getApplicationName(uuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        logger.debug(Messages.GETTING_ENVIRONMENT_OF_APPLICATION_0, applicationName);
        return delegate.getApplicationEnvironment(applicationName);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        logger.debug(Messages.GETTING_ENVIRONMENT_OF_APPLICATION_0, applicationGuid);
        return delegate.getApplicationEnvironment(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        logger.debug(Messages.GETTING_EVENTS_FOR_APPLICATION_0, applicationName);
        return delegate.getApplicationEvents(applicationName);
    }

    @Override
    public List<CloudEvent> getEventsByActee(UUID uuid) {
        logger.debug(Messages.GETTING_EVENTS_BY_ACTEE_0, uuid.toString());
        return delegate.getEventsByActee(uuid);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        logger.debug(Messages.GETTING_INSTANCES_FOR_APPLICATION_0, app.getName());
        return delegate.getApplicationInstances(app);
    }

    @Override
    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        logger.debug(Messages.GETTING_INSTANCES_FOR_APPLICATION_0, applicationGuid);
        return delegate.getApplicationInstances(applicationGuid);
    }

    @Override
    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        logger.debug(Messages.GETTING_PROCESS_FOR_APPLICATION_0, applicationGuid);
        return delegate.getApplicationProcess(applicationGuid);
    }

    @Override
    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        logger.debug(Messages.GETTING_ROUTES_FOR_APPLICATION_0, applicationGuid);
        return delegate.getApplicationRoutes(applicationGuid);
    }

    @Override
    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        logger.debug(Messages.GETTING_SSH_ENABLED_FOR_APPLICATION_0, applicationGuid);
        return delegate.getApplicationSshEnabled(applicationGuid);
    }

    @Override
    public List<CloudApplication> getApplications() {
        logger.debug(Messages.GETTING_APPLICATIONS);
        return delegate.getApplications();
    }

    @Override
    public CloudDomain getDefaultDomain() {
        logger.debug(Messages.GETTING_DEFAULT_DOMAIN);
        return delegate.getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getDomains() {
        logger.debug(Messages.GETTING_DOMAINS);
        return delegate.getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        logger.debug(Messages.GETTING_DOMAINS_FOR_ORGANIZATION);
        return delegate.getDomainsForOrganization();
    }

    @Override
    public List<CloudEvent> getEvents() {
        logger.debug(Messages.GETTING_EVENTS);
        return delegate.getEvents();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        logger.debug(Messages.GETTING_PRIVATE_DOMAINS);
        return delegate.getPrivateDomains();
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        logger.debug(Messages.GETTING_ROUTES_WITH_DOMAIN_0, domainName);
        return delegate.getRoutes(domainName);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        logger.debug(Messages.GETTING_SERVICE_BROKER_0, name);
        return delegate.getServiceBroker(name);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        logger.debug(Messages.GETTING_SERVICE_BROKER_0, name);
        return delegate.getServiceBroker(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        logger.debug(Messages.GETTING_SERVICE_BROKERS);
        return delegate.getServiceBrokers();
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        logger.debug(Messages.GETTING_GUID_OF_REQUIRED_SERVICE_INSTANCE_0, name);
        return delegate.getRequiredServiceInstanceGuid(name);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCE_0, serviceInstanceName);
        return delegate.getServiceInstance(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCE_0, serviceInstanceName);
        return delegate.getServiceInstance(serviceInstanceName, required);
    }

    @Override
    public String getServiceInstanceName(UUID serviceInstanceGuid) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCE_NAME_0, serviceInstanceGuid);
        return delegate.getServiceInstanceName(serviceInstanceGuid);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCE_WITHOUT_AUXILIARY_CONTENT_0, serviceInstanceName);
        return delegate.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCE_WITHOUT_AUXILIARY_CONTENT_0, serviceInstanceName);
        return delegate.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, required);
    }

    @Override
    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        logger.debug(Messages.GETTING_BINDINGS_OF_SERVICE_INSTANCE_0, serviceInstanceGuid);
        return delegate.getServiceAppBindings(serviceInstanceGuid);
    }

    @Override
    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        logger.debug(Messages.GETTING_BINDINGS_OF_APPLICATION_0, applicationGuid);
        return delegate.getAppBindings(applicationGuid);
    }

    @Override
    public CloudServiceBinding getServiceBindingForApplication(UUID applicationGuid, UUID serviceInstanceGuid) {
        logger.debug(Messages.GETTING_BINDING_OF_SERVICE_INSTANCE_0_WITH_APPLICATION_1, serviceInstanceGuid, applicationGuid);
        return delegate.getServiceBindingForApplication(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        logger.debug(Messages.GETTING_PARAMETERS_OF_SERVICE_INSTANCE_0, guid);
        return delegate.getServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        logger.debug(Messages.GETTING_PARAMETERS_OF_USER_PROVIDED_SERVICE_INSTANCE_0, guid);
        return delegate.getUserProvidedServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        logger.debug(Messages.GETTING_PARAMETERS_OF_SERVICE_BINDING_0, guid);
        return delegate.getServiceBindingParameters(guid);
    }

    @Override
    public CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName) {
        logger.debug(Messages.GETTING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1, serviceKeyName, serviceInstanceName);
        return delegate.getServiceKey(serviceInstanceName, serviceKeyName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        logger.debug(Messages.GETTING_SERVICE_KEYS_FOR_SERVICE_INSTANCE_0, serviceInstanceName);
        return delegate.getServiceKeys(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName) {
        logger.debug(Messages.GETTING_SERVICE_KEYS_FOR_SERVICE_INSTANCE_0, serviceInstanceName);
        return delegate.getServiceKeysWithCredentials(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.GETTING_SERVICE_KEYS_FOR_SERVICE_INSTANCE_0, serviceInstance.getName());
        return delegate.getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.GETTING_SERVICE_KEYS_FOR_SERVICE_INSTANCE_0, serviceInstance.getName());
        return delegate.getServiceKeysWithCredentials(serviceInstance);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        logger.debug(Messages.GETTING_SERVICE_OFFERINGS);
        return delegate.getServiceOfferings();
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        logger.debug(Messages.GETTING_SHARED_DOMAINS);
        return delegate.getSharedDomains();
    }

    @Override
    public CloudStack getStack(String name) {
        logger.debug(Messages.GETTING_STACK_0, name);
        return delegate.getStack(name);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        logger.debug(Messages.GETTING_STACK_0, name);
        return delegate.getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks() {
        logger.debug(Messages.GETTING_STACKS);
        return delegate.getStacks();
    }

    @Override
    public void rename(String applicationName, String newName) {
        logger.debug(Messages.RENAMING_APPLICATION_0_TO_1, applicationName, newName);
        delegate.rename(applicationName, newName);
    }

    @Override
    public void restartApplication(String applicationName) {
        logger.debug(Messages.RESTARTING_APPLICATION_0, applicationName);
        delegate.restartApplication(applicationName);
    }

    @Override
    public void startApplication(String applicationName) {
        logger.debug(Messages.STARTING_APPLICATION_0, applicationName);
        delegate.startApplication(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        logger.debug(Messages.STOPPING_APPLICATION_0, applicationName);
        delegate.stopApplication(applicationName);
    }

    @Override
    public Optional<String> unbindServiceInstance(String applicationName, String serviceInstanceName,
                                                  ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        logger.debug(Messages.UNBINDING_APPLICATION_0_FROM_SERVICE_INSTANCE_1, applicationName, serviceInstanceName);
        return delegate.unbindServiceInstance(applicationName, serviceInstanceName, applicationServicesUpdateCallback);
    }

    @Override
    public Optional<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        logger.debug(Messages.UNBINDING_APPLICATION_0_FROM_SERVICE_INSTANCE_1, applicationName, serviceInstanceName);
        return delegate.unbindServiceInstance(applicationName, serviceInstanceName);
    }

    @Override
    public Optional<String> unbindServiceInstance(UUID appGuid, UUID serviceGuid) {
        logger.debug(Messages.UNBINDING_APPLICATION_0_FROM_SERVICE_INSTANCE_1, appGuid, serviceGuid);
        return delegate.unbindServiceInstance(appGuid, serviceGuid);
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        logger.debug(Messages.UPDATING_DISK_QUOTA_OF_APPLICATION_0_TO_1, applicationName, disk);
        delegate.updateApplicationDiskQuota(applicationName, disk);
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        logger.debug(Messages.UPDATING_ENVIRONMENT_OF_APPLICATION_0, applicationName);
        delegate.updateApplicationEnv(applicationName, env);
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        logger.debug(Messages.UPDATING_INSTANCES_OF_APPLICATION_0_TO_1, applicationName, instances);
        delegate.updateApplicationInstances(applicationName, instances);
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        logger.debug(Messages.UPDATING_MEMORY_OF_APPLICATION_0_TO_1, applicationName, memory);
        delegate.updateApplicationMemory(applicationName, memory);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        logger.debug(Messages.UPDATING_STAGING_OF_APPLICATION_0_TO_1, applicationName, SecureSerialization.toJson(staging));
        delegate.updateApplicationStaging(applicationName, staging);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> routes) {
        logger.debug(Messages.UPDATING_URIS_OF_APPLICATION_0_TO_1, applicationName, UriUtil.prettyPrintRoutes(routes));
        delegate.updateApplicationRoutes(applicationName, routes);
    }

    @Override
    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        logger.debug(Messages.UPDATING_SERVICE_BROKER_TO_0, SecureSerialization.toJson(serviceBroker));
        return delegate.updateServiceBroker(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        logger.debug(Messages.UPDATING_PUBLIC_SERVICE_PLAN_VISIBILITY_OF_SERVICE_BROKER_0_TO_1, name, visibility);
        delegate.updateServicePlanVisibilityForBroker(name, visibility);
    }

    @Override
    public void updateServicePlan(String serviceName, String servicePlan) {
        logger.debug(Messages.UPDATING_SERVICE_PLAN, serviceName);
        delegate.updateServicePlan(serviceName, servicePlan);
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        logger.debug(Messages.UPDATING_SERVICE_PARAMETERS, serviceName);
        delegate.updateServiceParameters(serviceName, parameters);
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        logger.debug(Messages.UPDATING_SERVICE_TAGS, serviceName);
        delegate.updateServiceTags(serviceName, tags);
    }

    @Override
    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        logger.debug(Messages.UPDATING_SERVICE_SYSLOG_URL, serviceName);
        delegate.updateServiceSyslogDrainUrl(serviceName, syslogDrainUrl);
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, Duration initialRequestTimeout) {
        logger.debug(Messages.ASYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        return delegate.asyncUploadApplication(applicationName, file, initialRequestTimeout);
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback,
                                               Duration initialRequestTimeout) {
        logger.debug(Messages.ASYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        return delegate.asyncUploadApplication(applicationName, file, callback, initialRequestTimeout);
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        logger.debug(Messages.GETTING_PACKAGE_0, packageGuid);
        return delegate.getUploadStatus(packageGuid);
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        logger.debug(Messages.GETTING_TASK_0, taskGuid);
        return delegate.getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        logger.debug(Messages.GETTING_TASKS_FOR_APPLICATION_0, applicationName);
        return delegate.getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        logger.debug(Messages.RUNNING_TASK_1_ON_APPLICATION_0, applicationName, SecureSerialization.toJson(task));
        return delegate.runTask(applicationName, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        logger.debug(Messages.CANCELLING_TASK_0, taskGuid);
        return delegate.cancelTask(taskGuid);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        logger.debug(Messages.CREATING_BUILD_FOR_PACKAGE_0, packageGuid);
        return delegate.createBuild(packageGuid);
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        logger.debug(Messages.GETTING_BUILD_0, buildGuid);
        return delegate.getBuild(buildGuid);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        logger.debug(Messages.BINDING_DROPLET_0_TO_APPLICATION_1, dropletGuid, applicationGuid);
        delegate.bindDropletToApp(dropletGuid, applicationGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        logger.debug(Messages.GETTING_BUILDS_FOR_APPLICATION_0, applicationGuid);
        return delegate.getBuildsForApplication(applicationGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        logger.debug(Messages.GETTING_BUILDS_FOR_PACKAGE_0, packageGuid);
        return delegate.getBuildsForPackage(packageGuid);
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        logger.debug(Messages.GETTING_APPLICATIONS_BY_METADATA_LABEL_SELECTOR_0, labelSelector);
        return delegate.getApplicationsByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCES_WITHOUT_AUXILIARY_CONTENT_BY_NAMES_0, names);
        return delegate.getServiceInstancesWithoutAuxiliaryContentByNames(names);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCES_BY_METADATA_LABEL_SELECTOR_0, labelSelector);
        return delegate.getServiceInstancesByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCES_WITHOUT_AUXILIARY_CONTENT_BY_METADATA_LABEL_SELECTOR_0, labelSelector);
        return delegate.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(labelSelector);
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        logger.debug(Messages.UPDATING_METADATA_OF_APPLICATION_0_TO_1, guid, SecureSerialization.toJson(metadata));
        delegate.updateApplicationMetadata(guid, metadata);
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        logger.debug(Messages.UPDATING_METADATA_OF_SERVICE_INSTANCE_0_TO_1, guid, SecureSerialization.toJson(metadata));
        delegate.updateServiceInstanceMetadata(guid, metadata);
    }

    @Override
    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        logger.debug(Messages.UPDATING_METADATA_OF_SERVICE_BINDING_0_TO_1, guid, SecureSerialization.toJson(metadata));
        delegate.updateServiceBindingMetadata(guid, metadata);
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        logger.debug(Messages.GETTING_THE_CURRENT_DROPLET_FOR_APPLICATION_0, applicationGuid);
        return delegate.getCurrentDropletForApplication(applicationGuid);
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        logger.debug(Messages.GETTING_PACKAGE_BY_ID_0, packageGuid);
        return delegate.getPackage(packageGuid);
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        logger.debug(Messages.GETTING_PACKAGES_FOR_APPLICATION_0, applicationGuid);
        return delegate.getPackagesForApplication(applicationGuid);
    }

    @Override
    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        logger.debug(Messages.GETTING_ROLES_FOR_USER_0_FOR_SPACE_1, userGuid, spaceGuid);
        return delegate.getUserRolesBySpaceAndUser(spaceGuid, userGuid);
    }

    @Override
    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        logger.debug(Messages.CREATING_DOCKER_PACKAGE_FOR_APPLICATION_0, applicationGuid);
        return delegate.createDockerPackage(applicationGuid, dockerInfo);

    }

    @Override
    public CloudAsyncJob getAsyncJob(String jobId) {
        logger.debug(Messages.GETTING_ASYNC_JOB_0, jobId);
        return delegate.getAsyncJob(jobId);
    }

}
