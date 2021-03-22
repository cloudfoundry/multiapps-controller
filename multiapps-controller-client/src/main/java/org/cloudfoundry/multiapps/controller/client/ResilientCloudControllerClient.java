package org.cloudfoundry.multiapps.controller.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.util.ResilientCloudOperationExecutor;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerClientImpl;
import com.sap.cloudfoundry.client.facade.UploadStatusCallback;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
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
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.Upload;
import com.sap.cloudfoundry.client.facade.domain.UserRole;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClient;

public class ResilientCloudControllerClient implements CloudControllerClient {

    private final CloudControllerClientImpl delegate;

    public ResilientCloudControllerClient(CloudControllerRestClient delegate) {
        this.delegate = new CloudControllerClientImpl(delegate);
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        executeWithRetry(() -> delegate.createServiceInstance(serviceInstance));
    }

    @Override
    public void addDomain(String domainName) {
        executeWithRetry(() -> delegate.addDomain(domainName));
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        executeWithRetry(() -> delegate.addRoute(host, domainName, path));
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName) {
        executeWithRetry(() -> delegate.bindServiceInstance(applicationName, serviceInstanceName));
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName, Map<String, Object> parameters,
                                    ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        executeWithRetry(() -> delegate.bindServiceInstance(applicationName, serviceInstanceName, parameters,
                                                            applicationServicesUpdateCallback));
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, Set<CloudRouteSummary> routes) {
        executeWithRetry(() -> delegate.createApplication(applicationName, staging, disk, memory, routes));
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> delegate.createServiceBroker(serviceBroker));
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials) {
        executeWithRetry(() -> delegate.createUserProvidedServiceInstance(serviceInstance, credentials));
    }

    @Override
    public void deleteApplication(String applicationName) {
        executeWithRetry(() -> delegate.deleteApplication(applicationName));
    }

    @Override
    public void deleteDomain(String domainName) {
        executeWithRetry(() -> delegate.deleteDomain(domainName));
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return executeWithRetry(delegate::deleteOrphanedRoutes, HttpStatus.NOT_FOUND);
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        executeWithRetry(() -> delegate.deleteRoute(host, domainName, path));
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        executeWithRetry(() -> delegate.deleteServiceInstance(serviceInstanceName));
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        executeWithRetry(() -> delegate.deleteServiceInstance(serviceInstance));
    }

    @Override
    public void deleteServiceBroker(String name) {
        executeWithRetry(() -> delegate.deleteServiceBroker(name));
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return executeWithRetry(() -> delegate.getApplication(applicationName));
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return executeWithRetry(() -> delegate.getApplication(applicationName, required));
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        return executeWithRetry(() -> delegate.getApplication(appGuid));
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        return executeWithRetry(() -> delegate.getApplicationGuid(applicationName));
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication application) {
        return executeWithRetry(() -> delegate.getApplicationInstances(application));
    }

    @Override
    public List<CloudApplication> getApplications() {
        return executeWithRetry(() -> delegate.getApplications(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return executeWithRetry(delegate::getDefaultDomain);
    }

    @Override
    public List<CloudDomain> getDomains() {
        return executeWithRetry(delegate::getDomains, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return executeWithRetry(delegate::getDomainsForOrganization, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName) {
        return executeWithRetry(() -> delegate.getOrganization(organizationName));
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        return executeWithRetry(() -> delegate.getOrganization(organizationName, required));
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return executeWithRetry(delegate::getPrivateDomains, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        return executeWithRetry(() -> delegate.getRecentLogs(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        return executeWithRetry(() -> delegate.getRecentLogs(applicationGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return executeWithRetry(() -> delegate.getRoutes(domainName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return executeWithRetry(() -> delegate.getServiceBroker(name));
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return executeWithRetry(() -> delegate.getServiceBroker(name, required));
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return executeWithRetry(delegate::getServiceBrokers, HttpStatus.NOT_FOUND);
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        return executeWithRetry(() -> delegate.getRequiredServiceInstanceGuid(name));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return executeWithRetry(() -> delegate.getServiceInstance(serviceInstanceName));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        return executeWithRetry(() -> delegate.getServiceInstance(serviceInstanceName, required));
    }

    @Override
    public List<CloudServiceBinding> getServiceBindings(UUID serviceInstanceGuid) {
        return executeWithRetry(() -> delegate.getServiceBindings(serviceInstanceGuid));
    }

    @Override
    public CloudServiceBinding getServiceBindingForApplication(UUID applicationGuid, UUID serviceInstanceGuid) {
        return executeWithRetry(() -> delegate.getServiceBindingForApplication(applicationGuid, serviceInstanceGuid));
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return executeWithRetry(() -> delegate.getServiceInstanceParameters(guid));
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return executeWithRetry(() -> delegate.getServiceBindingParameters(guid));
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return executeWithRetry(delegate::getSharedDomains, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        return executeWithRetry(() -> delegate.getSpace(spaceGuid));
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        return executeWithRetry(() -> delegate.getSpace(organizationName, spaceName));
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        return executeWithRetry(() -> delegate.getSpace(organizationName, spaceName, required));
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return executeWithRetry(() -> delegate.getSpace(spaceName));
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return executeWithRetry(() -> delegate.getSpace(spaceName, required));
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return executeWithRetry(() -> delegate.getSpaces(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        return executeWithRetry(() -> delegate.getSpaces(organizationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public void rename(String oldApplicationName, String newApplicationName) {
        executeWithRetry(() -> delegate.rename(oldApplicationName, newApplicationName));
    }

    @Override
    public void restartApplication(String applicationName) {
        executeWithRetry(() -> delegate.restartApplication(applicationName));
    }

    @Override
    public void startApplication(String applicationName) {
        executeWithRetry(() -> delegate.startApplication(applicationName));
    }

    @Override
    public void stopApplication(String applicationName) {
        executeWithRetry(() -> delegate.stopApplication(applicationName));
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName) {
        executeWithRetry(() -> delegate.unbindServiceInstance(applicationName, serviceInstanceName));
    }

    @Override
    public void unbindServiceInstance(CloudApplication application, CloudServiceInstance serviceInstance) {
        executeWithRetry(() -> delegate.unbindServiceInstance(application, serviceInstance));
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        executeWithRetry(() -> delegate.updateApplicationDiskQuota(applicationName, disk));
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        executeWithRetry(() -> delegate.updateApplicationEnv(applicationName, env));
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        executeWithRetry(() -> delegate.updateApplicationInstances(applicationName, instances));
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        executeWithRetry(() -> delegate.updateApplicationMemory(applicationName, memory));
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        executeWithRetry(() -> delegate.updateApplicationStaging(applicationName, staging));
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRouteSummary> routes) {
        executeWithRetry(() -> delegate.updateApplicationRoutes(applicationName, routes), HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> delegate.updateServiceBroker(serviceBroker));
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        executeWithRetry(() -> delegate.updateServicePlanVisibilityForBroker(name, visibility));
    }

    @Override
    public void updateServicePlan(String serviceName, String servicePlan) {
        executeWithRetry(() -> delegate.updateServicePlan(serviceName, servicePlan));
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        executeWithRetry(() -> delegate.updateServiceParameters(serviceName, parameters));
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        executeWithRetry(() -> delegate.updateServiceTags(serviceName, tags));
    }

    @Override
    public void uploadApplication(String applicationName, Path file, UploadStatusCallback callback) {
        executeWithRetry(() -> delegate.uploadApplication(applicationName, file, callback));
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, inputStream, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback) {
        return executeWithRetry(() -> delegate.asyncUploadApplication(applicationName, file, callback));
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return executeWithRetry(() -> delegate.getUploadStatus(packageGuid));
    }

    @Override
    public List<CloudServiceInstance> getServiceInstances() {
        return executeWithRetry(delegate::getServiceInstances, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceKey createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        return executeWithRetry(() -> delegate.createServiceKey(serviceInstanceName, serviceKeyName, parameters));
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials,
                                                  String syslogDrainUrl) {
        executeWithRetry(() -> delegate.createUserProvidedServiceInstance(serviceInstance, credentials, syslogDrainUrl));
    }

    @Override
    public void deleteAllApplications() {
        executeWithRetry(delegate::deleteAllApplications);
    }

    @Override
    public void deleteAllServiceInstances() {
        executeWithRetry(delegate::deleteAllServiceInstances);
    }

    @Override
    public void deleteServiceKey(String serviceInstanceName, String serviceKeyName) {
        executeWithRetry(() -> delegate.deleteServiceKey(serviceInstanceName, serviceKeyName));
    }

    @Override
    public void deleteServiceKey(CloudServiceKey serviceKey) {
        executeWithRetry(() -> delegate.deleteServiceKey(serviceKey));
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return executeWithRetry(() -> delegate.getApplicationEnvironment(applicationName));
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID appGuid) {
        return executeWithRetry(() -> delegate.getApplicationEnvironment(appGuid));
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return executeWithRetry(() -> delegate.getApplicationEvents(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudEvent> getEventsByActee(UUID uuid) {
        return executeWithRetry(() -> delegate.getEventsByActee(uuid));
    }

    @Override
    public URL getCloudControllerUrl() {
        return executeWithRetry(delegate::getCloudControllerUrl);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return executeWithRetry(delegate::getEvents, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return executeWithRetry(delegate::getOrganizations, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        return executeWithRetry(() -> delegate.getServiceKeys(serviceInstanceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return executeWithRetry(() -> delegate.getServiceKeys(serviceInstance), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return executeWithRetry(delegate::getServiceOfferings, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudStack getStack(String name) {
        return executeWithRetry(() -> delegate.getStack(name));
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return executeWithRetry(() -> delegate.getStack(name, required));
    }

    @Override
    public List<CloudStack> getStacks() {
        return executeWithRetry(delegate::getStacks, HttpStatus.NOT_FOUND);
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo login() {
        return executeWithRetry(delegate::login);
    }

    @Override
    public void logout() {
        executeWithRetry(delegate::logout);
    }

    @Override
    public void uploadApplication(String applicationName, String file) {
        executeWithRetry(() -> delegate.uploadApplication(applicationName, file));
    }

    @Override
    public void uploadApplication(String applicationName, Path file) {
        executeWithRetry(() -> delegate.uploadApplication(applicationName, file));
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, inputStream);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file) {
        return executeWithRetry(() -> delegate.asyncUploadApplication(applicationName, file));
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return executeWithRetry(() -> delegate.getTask(taskGuid));
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return executeWithRetry(() -> delegate.getTasks(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return executeWithRetry(() -> delegate.runTask(applicationName, task));
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return executeWithRetry(() -> delegate.cancelTask(taskGuid));
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return executeWithRetry(() -> delegate.createBuild(packageGuid));
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return executeWithRetry(() -> delegate.getBuildsForPackage(packageGuid));
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return executeWithRetry(() -> delegate.getBuild(buildGuid));
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID appGuid) {
        executeWithRetry(() -> delegate.bindDropletToApp(dropletGuid, appGuid));
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return executeWithRetry(() -> delegate.getBuildsForApplication(applicationGuid));
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName,
                                      ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        executeWithRetry(() -> delegate.unbindServiceInstance(applicationName, serviceInstanceName, applicationServicesUpdateCallback));
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        return executeWithRetry(() -> delegate.getApplicationsByMetadataLabelSelector(labelSelector));
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        return executeWithRetry(() -> delegate.getServiceInstancesByMetadataLabelSelector(labelSelector));
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        return executeWithRetry(() -> delegate.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(labelSelector));
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        executeWithRetry(() -> delegate.updateApplicationMetadata(guid, metadata));
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        executeWithRetry(() -> delegate.updateServiceInstanceMetadata(guid, metadata));
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        return executeWithRetry(() -> delegate.getCurrentDropletForApplication(applicationGuid));
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return executeWithRetry(() -> delegate.getPackage(packageGuid));
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return executeWithRetry(() -> delegate.getPackagesForApplication(applicationGuid));
    }

    @Override
    public List<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        return executeWithRetry(() -> delegate.getUserRolesBySpaceAndUser(spaceGuid, userGuid));
    }

    @Override
    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        return executeWithRetry(() -> delegate.createDockerPackage(applicationGuid, dockerInfo));
    }

    private void executeWithRetry(Runnable operation, HttpStatus... statusesToIgnore) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, statusesToIgnore);
    }

    private <T> T executeWithRetry(Supplier<T> operation, HttpStatus... statusesToIgnore) {
        ResilientCloudOperationExecutor executor = new ResilientCloudOperationExecutor().withStatusesToIgnore(statusesToIgnore);
        return executor.execute(operation);
    }

}
