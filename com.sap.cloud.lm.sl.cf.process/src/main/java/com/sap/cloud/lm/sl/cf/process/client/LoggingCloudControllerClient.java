package com.sap.cloud.lm.sl.cf.process.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.ResponseErrorHandler;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerialization;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.cf.process.Messages;

public class LoggingCloudControllerClient implements CloudControllerClient {

    private final CloudControllerClient delegate;
    private final UserMessageLogger logger;

    public LoggingCloudControllerClient(CloudControllerClient delegate, UserMessageLogger logger) {
        this.delegate = delegate;
        this.logger = logger;
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
    public void bindServiceInstance(String applicationName, String serviceInstanceName) {
        logger.debug(Messages.BINDING_APPLICATION_0_TO_SERVICE_INSTANCE_1, applicationName, serviceInstanceName);
        delegate.bindServiceInstance(applicationName, serviceInstanceName);
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName, Map<String, Object> parameters,
                                    ApplicationServicesUpdateCallback updateServicesCallback) {
        logger.debug(Messages.BINDING_APPLICATION_0_TO_SERVICE_INSTANCE_1_WITH_PARAMETERS_2, applicationName, serviceInstanceName,
                     SecureSerialization.toJson(parameters));
        delegate.bindServiceInstance(applicationName, serviceInstanceName, parameters, updateServicesCallback);
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris) {
        logger.debug(Messages.CREATING_APPLICATION_0_WITH_MEMORY_1_URIS_2_AND_STAGING_3, applicationName, memory, uris,
                     SecureSerialization.toJson(staging));
        delegate.createApplication(applicationName, staging, memory, uris);
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris,
                                  DockerInfo dockerInfo) {
        logger.debug(Messages.CREATING_APPLICATION_0_WITH_DISK_QUOTA_1_MEMORY_2_URIS_3_AND_STAGING_4, applicationName, disk, memory, uris,
                     SecureSerialization.toJson(staging));
        delegate.createApplication(applicationName, staging, disk, memory, uris, dockerInfo);
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.CREATING_SERVICE_INSTANCE_0, SecureSerialization.toJson(serviceInstance));
        delegate.createServiceInstance(serviceInstance);
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        logger.debug(Messages.CREATING_SERVICE_BROKER_0, SecureSerialization.toJson(serviceBroker));
        delegate.createServiceBroker(serviceBroker);
    }

    @Override
    public CloudServiceKey createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        logger.debug(Messages.CREATING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1_WITH_PARAMETERS_2, serviceKeyName, serviceInstanceName,
                     SecureSerialization.toJson(parameters));
        return delegate.createServiceKey(serviceInstanceName, serviceKeyName, parameters);
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials) {
        logger.debug(Messages.CREATING_USER_PROVIDED_SERVICE_INSTANCE_0, SecureSerialization.toJson(serviceInstance));
        delegate.createUserProvidedServiceInstance(serviceInstance, credentials);
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials,
                                                  String syslogDrainUrl) {
        logger.debug(Messages.CREATING_USER_PROVIDED_SERVICE_INSTANCE_0, SecureSerialization.toJson(serviceInstance));
        delegate.createUserProvidedServiceInstance(serviceInstance, credentials, syslogDrainUrl);
    }

    @Override
    public void deleteAllApplications() {
        logger.debug(Messages.DELETING_ALL_APPLICATIONS);
        delegate.deleteAllApplications();
    }

    @Override
    public void deleteAllServiceInstances() {
        logger.debug(Messages.DELETING_ALL_SERVICE_INSTANCES);
        delegate.deleteAllServiceInstances();
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
    public List<CloudRoute> deleteOrphanedRoutes() {
        logger.debug(Messages.DELETING_ORPHANED_ROUTES);
        return delegate.deleteOrphanedRoutes();
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
    public void deleteServiceBroker(String name) {
        logger.debug(Messages.DELETING_SERVICE_BROKER_0, name);
        delegate.deleteServiceBroker(name);
    }

    @Override
    public void deleteServiceKey(String serviceInstanceName, String serviceKeyName) {
        logger.debug(Messages.DELETING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1, serviceKeyName, serviceInstanceName);
        delegate.deleteServiceKey(serviceInstanceName, serviceKeyName);
    }

    @Override
    public void deleteServiceKey(CloudServiceKey serviceKey) {
        logger.debug(Messages.DELETING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1, serviceKey, serviceKey.getServiceInstance()
                                                                                                   .getName());
        delegate.deleteServiceKey(serviceKey);
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
    public CloudApplication getApplication(UUID guid) {
        logger.debug(Messages.GETTING_APPLICATION_0, guid);
        return delegate.getApplication(guid);
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
    public InstancesInfo getApplicationInstances(String applicationName) {
        logger.debug(Messages.GETTING_INSTANCES_OF_APPLICATION_0, applicationName);
        return delegate.getApplicationInstances(applicationName);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        logger.debug(Messages.GETTING_INSTANCES_OF_APPLICATION_0, app.getName());
        return delegate.getApplicationInstances(app);
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
    public CloudOrganization getOrganization(String organizationName) {
        logger.debug(Messages.GETTING_ORGANIZATION_0, organizationName);
        return delegate.getOrganization(organizationName);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        logger.debug(Messages.GETTING_ORGANIZATION_0, organizationName);
        return delegate.getOrganization(organizationName, required);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        logger.debug(Messages.GETTING_ORGANIZATIONS);
        return delegate.getOrganizations();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        logger.debug(Messages.GETTING_PRIVATE_DOMAINS);
        return delegate.getPrivateDomains();
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        logger.debug(Messages.GETTING_RECENT_LOGS_OF_APPLICATION_0, applicationName);
        return delegate.getRecentLogs(applicationName);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        logger.debug(Messages.GETTING_RECENT_LOGS_OF_APPLICATION_0, applicationGuid);
        return delegate.getRecentLogs(applicationGuid);
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
    public List<CloudServiceBinding> getServiceBindings(UUID serviceInstanceGuid) {
        logger.debug(Messages.GETTING_BINDINGS_OF_SERVICE_INSTANCE_0, serviceInstanceGuid);
        return delegate.getServiceBindings(serviceInstanceGuid);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        logger.debug(Messages.GETTING_PARAMETERS_OF_SERVICE_INSTANCE_0, guid);
        return delegate.getServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        logger.debug(Messages.GETTING_PARAMETERS_OF_SERVICE_BINDING_0, guid);
        return delegate.getServiceBindingParameters(guid);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        logger.debug(Messages.GETTING_SERVICE_KEYS_FOR_SERVICE_INSTANCE_0, serviceInstanceName);
        return delegate.getServiceKeys(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        logger.debug(Messages.GETTING_SERVICE_KEYS_FOR_SERVICE_INSTANCE_0, serviceInstance.getName());
        return delegate.getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        logger.debug(Messages.GETTING_SERVICE_OFFERINGS);
        return delegate.getServiceOfferings();
    }

    @Override
    public List<CloudServiceInstance> getServiceInstances() {
        logger.debug(Messages.GETTING_SERVICE_INSTANCES);
        return delegate.getServiceInstances();
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        logger.debug(Messages.GETTING_SHARED_DOMAINS);
        return delegate.getSharedDomains();
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        logger.debug(Messages.GETTING_SPACE_0, spaceGuid);
        return delegate.getSpace(spaceGuid);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        logger.debug(Messages.GETTING_SPACE_IN_ORGANIZATION_0, spaceName, organizationName);
        return delegate.getSpace(organizationName, spaceName);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        logger.debug(Messages.GETTING_SPACE_IN_ORGANIZATION_0, spaceName, organizationName);
        return delegate.getSpace(organizationName, spaceName, required);
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        logger.debug(Messages.GETTING_SPACE_0, spaceName);
        return delegate.getSpace(spaceName);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        logger.debug(Messages.GETTING_SPACE_0, spaceName);
        return delegate.getSpace(spaceName, required);
    }

    @Override
    public List<UUID> getSpaceAuditors() {
        logger.debug(Messages.GETTING_SPACE_AUDITORS);
        return delegate.getSpaceAuditors();
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        logger.debug(Messages.GETTING_SPACE_AUDITORS_FOR_SPACE_0, spaceName);
        return delegate.getSpaceAuditors(spaceName);
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        logger.debug(Messages.GETTING_SPACE_AUDITORS_FOR_SPACE_0_IN_ORGANIZATION_1, spaceName, organizationName);
        return delegate.getSpaceAuditors(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        logger.debug(Messages.GETTING_SPACE_AUDITORS_FOR_SPACE_0, spaceGuid);
        return delegate.getSpaceAuditors(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceDevelopers() {
        logger.debug(Messages.GETTING_SPACE_DEVELOPERS);
        return delegate.getSpaceDevelopers();
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        logger.debug(Messages.GETTING_SPACE_DEVELOPERS_FOR_SPACE_0, spaceName);
        return delegate.getSpaceDevelopers(spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        logger.debug(Messages.GETTING_SPACE_DEVELOPERS_FOR_SPACE_0_IN_ORGANIZATION_1, spaceName, organizationName);
        return delegate.getSpaceDevelopers(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        logger.debug(Messages.GETTING_SPACE_DEVELOPERS_FOR_SPACE_0, spaceGuid);
        return delegate.getSpaceDevelopers(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceManagers() {
        logger.debug(Messages.GETTING_SPACE_MANAGERS);
        return delegate.getSpaceManagers();
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        logger.debug(Messages.GETTING_SPACE_MANAGERS_FOR_SPACE_0, spaceName);
        return delegate.getSpaceManagers(spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        logger.debug(Messages.GETTING_SPACE_MANAGERS_FOR_SPACE_0_IN_ORGANIZATION_1, spaceName, organizationName);
        return delegate.getSpaceManagers(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        logger.debug(Messages.GETTING_SPACE_MANAGERS_FOR_SPACE_0, spaceGuid);
        return delegate.getSpaceManagers(spaceGuid);
    }

    @Override
    public List<CloudSpace> getSpaces() {
        logger.debug(Messages.GETTING_SPACES);
        return delegate.getSpaces();
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        logger.debug(Messages.GETTING_SPACES_IN_ORGANIZATION_0, organizationName);
        return delegate.getSpaces(organizationName);
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
    public StartingInfo restartApplication(String applicationName) {
        logger.debug(Messages.RESTARTING_APPLICATION_0, applicationName);
        return delegate.restartApplication(applicationName);
    }

    @Override
    public StartingInfo startApplication(String applicationName) {
        logger.debug(Messages.STARTING_APPLICATION_0, applicationName);
        return delegate.startApplication(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        logger.debug(Messages.STOPPING_APPLICATION_0, applicationName);
        delegate.stopApplication(applicationName);
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName,
                                      ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        logger.debug(Messages.UNBINDING_APPLICATION_0_FROM_SERVICE_INSTANCE_1, applicationName, serviceInstanceName);
        delegate.unbindServiceInstance(applicationName, serviceInstanceName, applicationServicesUpdateCallback);
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName) {
        logger.debug(Messages.UNBINDING_APPLICATION_0_FROM_SERVICE_INSTANCE_1, applicationName, serviceInstanceName);
        delegate.unbindServiceInstance(applicationName, serviceInstanceName);
    }

    @Override
    public void unbindServiceInstance(CloudApplication application, CloudServiceInstance serviceInstance) {
        logger.debug(Messages.UNBINDING_APPLICATION_0_FROM_SERVICE_INSTANCE_1, application.getName(), serviceInstance.getName());
        delegate.unbindServiceInstance(application, serviceInstance);
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
    public void updateApplicationUris(String applicationName, List<String> uris) {
        logger.debug(Messages.UPDATING_URIS_OF_APPLICATION_0_TO_1, applicationName, uris);
        delegate.updateApplicationUris(applicationName, uris);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        logger.debug(Messages.UPDATING_SERVICE_BROKER_TO_0, SecureSerialization.toJson(serviceBroker));
        delegate.updateServiceBroker(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        logger.debug(Messages.UPDATING_PUBLIC_SERVICE_PLAN_VISIBILITY_OF_SERVICE_BROKER_0_TO_1, name, visibility);
        delegate.updateServicePlanVisibilityForBroker(name, visibility);
    }

    @Override
    public void uploadApplication(String applicationName, String file) throws IOException {
        logger.debug(Messages.SYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        delegate.uploadApplication(applicationName, file);
    }

    @Override
    public void uploadApplication(String applicationName, File file) throws IOException {
        logger.debug(Messages.SYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        delegate.uploadApplication(applicationName, file);
    }

    @Override
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        logger.debug(Messages.SYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        delegate.uploadApplication(applicationName, file, callback);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream) throws IOException {
        logger.debug(Messages.SYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        delegate.uploadApplication(applicationName, inputStream);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        logger.debug(Messages.SYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        delegate.uploadApplication(applicationName, inputStream, callback);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file) throws IOException {
        logger.debug(Messages.ASYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        return delegate.asyncUploadApplication(applicationName, file);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        logger.debug(Messages.ASYNCHRONOUSLY_UPLOADING_APPLICATION_0, applicationName);
        return delegate.asyncUploadApplication(applicationName, file, callback);
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
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        logger.debug(Messages.UPDATING_METADATA_OF_APPLICATION_0_TO_1, guid, SecureSerialization.toJson(metadata));
        delegate.updateApplicationMetadata(guid, metadata);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        logger.debug(Messages.GETTING_SERVICE_INSTANCES_BY_METADATA_LABEL_SELECTOR_0, labelSelector);
        return delegate.getServiceInstancesByMetadataLabelSelector(labelSelector);
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        logger.debug(Messages.UPDATING_METADATA_OF_SERVICE_INSTANCE_0_TO_1, guid, SecureSerialization.toJson(metadata));
        delegate.updateServiceInstanceMetadata(guid, metadata);
    }

    @Override
    public URL getCloudControllerUrl() {
        return delegate.getCloudControllerUrl();
    }

    @Override
    public CloudInfo getCloudInfo() {
        return delegate.getCloudInfo();
    }

    @Override
    public OAuth2AccessToken login() {
        return delegate.login();
    }

    @Override
    public void logout() {
        delegate.logout();
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        delegate.registerRestLogListener(callBack);
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        delegate.setResponseErrorHandler(errorHandler);
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        delegate.unRegisterRestLogListener(callBack);
    }

}
