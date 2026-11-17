package org.cloudfoundry.multiapps.controller.client.facade;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudControllerRestClient;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudControllerRestClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.rest.ImmutableCloudControllerRestClientFactory;
import org.springframework.util.Assert;

/**
 * A Java client to exercise the Cloud Foundry API.
 */
public class CloudControllerClientImpl implements CloudControllerClient {

    private final CloudControllerRestClient delegate;

    /**
     * Construct client without a default organization and space.
     */
    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials) {
        this(controllerUrl, credentials, null, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, CloudSpace target, boolean trustSelfSignedCerts) {
        Assert.notNull(controllerUrl, "URL for cloud controller cannot be null");
        CloudControllerRestClientFactory restClientFactory = ImmutableCloudControllerRestClientFactory.builder()
                                                                                                      .shouldTrustSelfSignedCertificates(
                                                                                                          trustSelfSignedCerts)
                                                                                                      .build();
        this.delegate = restClientFactory.createClient(controllerUrl, credentials, target);
    }

    /**
     * Construct a client with a pre-configured CloudControllerClient
     */
    public CloudControllerClientImpl(CloudControllerRestClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public CloudSpace getTarget() {
        return delegate.getTarget();
    }

    @Override
    public void addDomain(String domainName) {
        delegate.addDomain(domainName);
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        delegate.addRoute(host, domainName, path);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        return delegate.bindServiceInstance(bindingName, applicationName, serviceInstanceName);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters, ApplicationServicesUpdateCallback updateServicesCallback) {
        try {
            return delegate.bindServiceInstance(bindingName, applicationName, serviceInstanceName, parameters);
        } catch (CloudOperationException e) {
            updateServicesCallback.onError(e, applicationName, serviceInstanceName);
        }
        return Optional.empty();
    }

    @Override
    public void createApplication(ApplicationToCreateDto applicationToCreateDto) {
        delegate.createApplication(applicationToCreateDto);
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        delegate.createServiceInstance(serviceInstance);
    }

    @Override
    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        return delegate.createServiceBroker(serviceBroker);
    }

    @Override
    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        return delegate.createAndFetchServiceKey(keyModel, serviceInstanceName);
    }

    @Override
    public Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        return delegate.createServiceKey(keyModel, serviceInstanceName);
    }

    @Override
    public Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        return delegate.createServiceKey(serviceInstanceName, serviceKeyName, parameters);
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        delegate.createUserProvidedServiceInstance(serviceInstance);
    }

    @Override
    public void deleteApplication(String applicationName) {
        delegate.deleteApplication(applicationName);
    }

    @Override
    public void deleteDomain(String domainName) {
        delegate.deleteDomain(domainName);
    }

    @Override
    public void deleteOrphanedRoutes() {
        delegate.deleteOrphanedRoutes();
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        delegate.deleteRoute(host, domainName, path);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        delegate.deleteServiceInstance(serviceInstanceName);
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        delegate.deleteServiceInstance(serviceInstance);
    }

    @Override
    public String deleteServiceBroker(String name) {
        return delegate.deleteServiceBroker(name);
    }

    @Override
    public CloudServiceBinding getServiceBinding(UUID serviceBindingId) {
        return delegate.getServiceBinding(serviceBindingId);
    }

    @Override
    public Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName) {
        return delegate.deleteServiceBinding(serviceInstanceName, serviceKeyName);
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid, ServiceBindingOperationCallback serviceBindingOperationCallback) {
        try {
            return delegate.deleteServiceBinding(bindingGuid);
        } catch (CloudOperationException e) {
            serviceBindingOperationCallback.onError(e, bindingGuid);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid) {
        return delegate.deleteServiceBinding(bindingGuid);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return delegate.getApplication(applicationName);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return delegate.getApplication(applicationName, required);
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        return delegate.getApplicationGuid(applicationName);
    }

    @Override
    public String getApplicationName(UUID applicationGuid) {
        return delegate.getApplicationName(applicationGuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        return delegate.getApplicationEnvironment(applicationGuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return delegate.getApplicationEnvironment(applicationName);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return delegate.getApplicationEvents(applicationName);
    }

    @Override
    public List<CloudEvent> getEventsByActee(UUID uuid) {
        return delegate.getEventsByTarget(uuid);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return delegate.getApplicationInstances(app);
    }

    @Override
    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        return delegate.getApplicationInstances(applicationGuid);
    }

    @Override
    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        return delegate.getApplicationProcess(applicationGuid);
    }

    @Override
    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        return delegate.getApplicationRoutes(applicationGuid);
    }

    @Override
    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        return delegate.getApplicationFeatures(applicationGuid);
    }

    @Override
    public List<CloudApplication> getApplications() {
        return delegate.getApplications();
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        return delegate.getApplicationsByMetadataLabelSelector(labelSelector);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return delegate.getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getDomains() {
        return delegate.getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return delegate.getDomainsForOrganization();
    }

    @Override
    public List<CloudEvent> getEvents() {
        return delegate.getEvents();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return delegate.getPrivateDomains();
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return delegate.getRoutes(domainName);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return delegate.getServiceBroker(name);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return delegate.getServiceBroker(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return delegate.getServiceBrokers();
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        return delegate.getRequiredServiceInstanceGuid(name);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return delegate.getServiceInstance(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        return delegate.getServiceInstance(serviceInstanceName, required);
    }

    @Override
    public String getServiceInstanceName(UUID serviceInstanceGuid) {
        return delegate.getServiceInstanceName(serviceInstanceGuid);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName) {
        return delegate.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required) {
        return delegate.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, required);
    }

    @Override
    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        return delegate.getServiceAppBindings(serviceInstanceGuid);
    }

    @Override
    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        return delegate.getAppBindings(applicationGuid);
    }

    @Override
    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        return delegate.getServiceBindingsForApplication(applicationId, serviceInstanceGuid);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return delegate.getServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        return delegate.getUserProvidedServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return delegate.getServiceBindingParameters(guid);
    }

    @Override
    public CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName) {
        return delegate.getServiceKey(serviceInstanceName, serviceKeyName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        return delegate.getServiceKeys(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName) {
        return delegate.getServiceKeysWithCredentials(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return delegate.getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        return delegate.getServiceKeysWithCredentials(serviceInstance);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return delegate.getServiceOfferings();
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        return delegate.getServiceInstancesByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        return delegate.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return delegate.getSharedDomains();
    }

    @Override
    public CloudStack getStack(String name) {
        return delegate.getStack(name);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return delegate.getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks() {
        return delegate.getStacks();
    }

    @Override
    public void rename(String applicationName, String newName) {
        delegate.rename(applicationName, newName);
    }

    @Override
    public void restartApplication(String applicationName) {
        delegate.restartApplication(applicationName);
    }

    @Override
    public void startApplication(String applicationName) {
        delegate.startApplication(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        delegate.stopApplication(applicationName);
    }

    @Override
    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName,
                                              ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        try {
            return delegate.unbindServiceInstance(applicationName, serviceInstanceName);
        } catch (CloudOperationException e) {
            applicationServicesUpdateCallback.onError(e, applicationName, serviceInstanceName);
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        return delegate.unbindServiceInstance(applicationName, serviceInstanceName);
    }

    @Override
    public List<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        return delegate.unbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        delegate.updateApplicationDiskQuota(applicationName, disk);
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        delegate.updateApplicationEnv(applicationName, env);
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        delegate.updateApplicationInstances(applicationName, instances);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        return delegate.getServiceInstancesWithoutAuxiliaryContentByNames(names);
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        delegate.updateApplicationMemory(applicationName, memory);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        delegate.updateApplicationStaging(applicationName, staging);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> routes) {
        delegate.updateApplicationRoutes(applicationName, routes);
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        delegate.updateApplicationMetadata(guid, metadata);
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        delegate.updateServiceInstanceMetadata(guid, metadata);
    }

    @Override
    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        delegate.updateServiceBindingMetadata(guid, metadata);
    }

    @Override
    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        return delegate.updateServiceBroker(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        delegate.updateServicePlanVisibilityForBroker(name, visibility);
    }

    @Override
    public void updateServicePlan(String serviceName, String planName) {
        delegate.updateServicePlan(serviceName, planName);
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        delegate.updateServiceParameters(serviceName, parameters);
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        delegate.updateServiceTags(serviceName, tags);
    }

    @Override
    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        delegate.updateServiceSyslogDrainUrl(serviceName, syslogDrainUrl);
    }

    @Override
    public CloudPackage asyncUploadApplicationWithExponentialBackoff(String applicationName, Path file, UploadStatusCallback callback,
                                                                     Duration overrideTimeout) {
        return delegate.asyncUploadApplication(applicationName, file, callback, overrideTimeout);
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return delegate.getUploadStatus(packageGuid);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return delegate.createBuild(packageGuid);
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return delegate.getBuild(buildGuid);
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return delegate.getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return delegate.getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return delegate.runTask(applicationName, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return delegate.cancelTask(taskGuid);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        delegate.bindDropletToApp(dropletGuid, applicationGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return delegate.getBuildsForApplication(applicationGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return delegate.getBuildsForPackage(packageGuid);
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        return delegate.getCurrentDropletForApplication(applicationGuid);
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return delegate.getPackage(packageGuid);
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return delegate.getPackagesForApplication(applicationGuid);
    }

    @Override
    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        return delegate.getUserRolesBySpaceAndUser(spaceGuid, userGuid);
    }

    @Override
    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        return delegate.createDockerPackage(applicationGuid, dockerInfo);
    }

    @Override
    public CloudAsyncJob getAsyncJob(String jobId) {
        return delegate.getAsyncJob(jobId);
    }

}
