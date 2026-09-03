package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.UploadStatusCallback;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

public class CloudControllerRestClientV3Impl implements CloudControllerRestClient {

    private final CloudSpace target;

    private final ApplicationsV3Operations applicationsOperations;
    private final BuildsV3Operations buildsOperations;
    private final DomainsV3Operations domainsOperations;
    private final EventsV3Operations eventsOperations;
    private final JobsV3Operations jobsOperations;
    private final PackagesV3Operations packagesOperations;
    private final ProcessesV3Operations processesOperations;
    private final RolesV3Operations rolesOperations;
    private final RoutesV3Operations routesOperations;
    private final ServiceBindingsV3Operations serviceBindingsOperations;
    private final ServiceBrokersV3Operations serviceBrokersOperations;
    private final ServiceInstancesV3Operations serviceInstancesOperations;
    private final ServiceKeysV3Operations serviceKeysOperations;
    private final ServiceOfferingsV3Operations serviceOfferingsOperations;
    private final StacksV3Operations stacksOperations;
    private final TasksV3Operations tasksOperations;

    public CloudControllerRestClientV3Impl(CloudSpace target, CloudControllerV3Client cloudControllerClient,
                                           Function<Duration, RestClient> uploadRestClientFactory) {
        this.target = target;
        this.applicationsOperations = new ApplicationsV3Operations(cloudControllerClient, target);
        this.buildsOperations = new BuildsV3Operations(cloudControllerClient);
        this.domainsOperations = new DomainsV3Operations(cloudControllerClient, target);
        this.eventsOperations = new EventsV3Operations(cloudControllerClient, target);
        this.jobsOperations = new JobsV3Operations(cloudControllerClient);
        this.packagesOperations = new PackagesV3Operations(cloudControllerClient, target, uploadRestClientFactory);
        this.processesOperations = new ProcessesV3Operations(cloudControllerClient, target);
        this.rolesOperations = new RolesV3Operations(cloudControllerClient);
        this.routesOperations = new RoutesV3Operations(cloudControllerClient, target);
        this.serviceBindingsOperations = new ServiceBindingsV3Operations(cloudControllerClient, target);
        this.serviceBrokersOperations = new ServiceBrokersV3Operations(cloudControllerClient, target);
        this.serviceInstancesOperations = new ServiceInstancesV3Operations(cloudControllerClient, target);
        this.serviceKeysOperations = new ServiceKeysV3Operations(cloudControllerClient);
        this.serviceOfferingsOperations = new ServiceOfferingsV3Operations(cloudControllerClient, target);
        this.stacksOperations = new StacksV3Operations(cloudControllerClient);
        this.tasksOperations = new TasksV3Operations(cloudControllerClient, target);
    }

    @Override
    public CloudSpace getTarget() {
        return target;
    }

    @Override
    public void addDomain(String domainName) {
        domainsOperations.addDomain(domainName);
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        routesOperations.addRoute(host, domainName, path);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        return serviceBindingsOperations.bindServiceInstance(bindingName, applicationName, serviceInstanceName);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters) {
        return serviceBindingsOperations.bindServiceInstance(bindingName, applicationName, serviceInstanceName, parameters);
    }

    @Override
    public void createApplication(ApplicationToCreateDto dto) {
        applicationsOperations.createApplication(dto);

        if (dto.getStaging() != null) {
            updateApplicationStaging(dto.getName(), dto.getStaging());
        }

        if (dto.getRoutes() != null && !dto.getRoutes()
                                           .isEmpty()) {
            updateApplicationRoutes(dto.getName(), dto.getRoutes());
        }
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        serviceInstancesOperations.createServiceInstance(serviceInstance);
    }

    @Override
    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        return serviceBrokersOperations.createServiceBroker(serviceBroker);
    }

    @Override
    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        return serviceKeysOperations.createAndFetchServiceKey(keyModel, getServiceInstance(serviceInstanceName));
    }

    @Override
    public Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        return serviceKeysOperations.createServiceKey(keyModel, getServiceInstance(serviceInstanceName));
    }

    @Override
    public Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        return serviceKeysOperations.createServiceKey(getServiceInstance(serviceInstanceName), serviceKeyName, parameters);
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        serviceInstancesOperations.createUserProvidedServiceInstance(serviceInstance);
    }

    @Override
    public void deleteApplication(String applicationName) {
        applicationsOperations.deleteApplication(applicationName);
    }

    @Override
    public void deleteDomain(String domainName) {
        domainsOperations.deleteDomain(domainName);
    }

    @Override
    public void deleteOrphanedRoutes() {
        routesOperations.deleteOrphanedRoutes();
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        routesOperations.deleteRoute(host, domainName, path);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        serviceInstancesOperations.deleteServiceInstance(serviceInstanceName);
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        serviceInstancesOperations.deleteServiceInstance(serviceInstance);
    }

    @Override
    public String deleteServiceBroker(String name) {
        return serviceBrokersOperations.deleteServiceBroker(name);
    }

    @Override
    public Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName) {
        CloudServiceKey serviceKey = getServiceKey(serviceInstanceName, serviceKeyName);

        if (serviceKey == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.SERVICE_KEY_0_NOT_FOUND, serviceKeyName));
        }

        return deleteServiceBinding(serviceKey.getGuid());
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid) {
        return serviceBindingsOperations.deleteServiceBinding(bindingGuid);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return applicationsOperations.getApplication(applicationName);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return applicationsOperations.getApplication(applicationName, required);
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        return applicationsOperations.getApplicationGuid(applicationName);
    }

    @Override
    public String getApplicationName(UUID applicationGuid) {
        return applicationsOperations.getApplicationName(applicationGuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        return applicationsOperations.getApplicationEnvironment(applicationGuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return applicationsOperations.getApplicationEnvironment(applicationName);
    }

    @Override
    public List<CloudApplication> getApplications() {
        return applicationsOperations.getApplications();
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        return applicationsOperations.getApplicationsByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return eventsOperations.getApplicationEvents(applicationName);
    }

    @Override
    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        return eventsOperations.getEventsByTarget(uuid);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return processesOperations.getApplicationInstances(app);
    }

    @Override
    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        return processesOperations.getApplicationInstances(applicationGuid);
    }

    @Override
    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        return processesOperations.getApplicationProcess(applicationGuid);
    }

    @Override
    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        return routesOperations.getApplicationRoutes(applicationGuid);
    }

    @Override
    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        return processesOperations.getApplicationSshEnabled(applicationGuid);
    }

    @Override
    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        return processesOperations.getApplicationFeatures(applicationGuid);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return domainsOperations.getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getDomains() {
        return domainsOperations.getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return domainsOperations.getDomainsForOrganization();
    }

    @Override
    public List<CloudEvent> getEvents() {
        return eventsOperations.getEvents();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return domainsOperations.getPrivateDomains();
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return routesOperations.getRoutes(domainName);
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        return serviceInstancesOperations.getRequiredServiceInstanceGuid(name);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return serviceInstancesOperations.getServiceInstance(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        return serviceInstancesOperations.getServiceInstance(serviceInstanceName, required);
    }

    @Override
    public String getServiceInstanceName(UUID serviceInstanceGuid) {
        return serviceInstancesOperations.getServiceInstanceName(serviceInstanceGuid);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName) {
        return serviceInstancesOperations.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required) {
        return serviceInstancesOperations.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, required);
    }

    @Override
    public CloudServiceBinding getServiceBinding(UUID serviceBindingGuid) {
        return serviceBindingsOperations.getServiceBinding(serviceBindingGuid);
    }

    @Override
    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        return serviceBindingsOperations.getServiceAppBindings(serviceInstanceGuid);
    }

    @Override
    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        return serviceBindingsOperations.getAppBindings(applicationGuid);
    }

    @Override
    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        return serviceBindingsOperations.getServiceBindingsForApplication(applicationId, serviceInstanceGuid);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return serviceBrokersOperations.getServiceBroker(name);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return serviceBrokersOperations.getServiceBroker(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return serviceBrokersOperations.getServiceBrokers();
    }

    @Override
    public CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName) {
        return serviceKeysOperations.getServiceKey(getServiceInstance(serviceInstanceName), serviceKeyName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        return serviceKeysOperations.getServiceKeys(getServiceInstance(serviceInstanceName));
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName) {
        return serviceKeysOperations.getServiceKeysWithCredentials(getServiceInstance(serviceInstanceName));
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return serviceKeysOperations.getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        return serviceKeysOperations.getServiceKeysWithCredentials(serviceInstance);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return serviceOfferingsOperations.getServiceOfferings();
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return domainsOperations.getSharedDomains();
    }

    @Override
    public CloudStack getStack(String name) {
        return stacksOperations.getStack(name);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return stacksOperations.getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks() {
        return stacksOperations.getStacks();
    }

    @Override
    public void startApplication(String applicationName) {
        applicationsOperations.startApplication(applicationName);
    }

    @Override
    public void restartApplication(String applicationName) {
        stopApplication(applicationName);
        startApplication(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        applicationsOperations.stopApplication(applicationName);
    }

    @Override
    public void rename(String applicationName, String newName) {
        applicationsOperations.rename(applicationName, newName);
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        applicationsOperations.updateApplicationInstances(applicationName, instances);
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        applicationsOperations.updateApplicationMemory(applicationName, memory);
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        applicationsOperations.updateApplicationDiskQuota(applicationName, disk);
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        applicationsOperations.updateApplicationEnv(applicationName, env);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        applicationsOperations.bindDropletToApp(dropletGuid, applicationGuid);
    }

    @Override
    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        return serviceBindingsOperations.unbindServiceInstance(applicationName, serviceInstanceName);
    }

    @Override
    public List<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        return serviceBindingsOperations.unbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        processesOperations.updateApplicationStaging(applicationName, staging);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> routes) {
        routesOperations.updateApplicationRoutes(applicationName, routes);
    }

    @Override
    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        return serviceBrokersOperations.updateServiceBroker(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        serviceBrokersOperations.updateServicePlanVisibilityForBroker(name, visibility);
    }

    @Override
    public void updateServicePlan(String serviceName, String planName) {
        serviceInstancesOperations.updateServicePlan(serviceName, planName);
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        serviceInstancesOperations.updateServiceParameters(serviceName, parameters);
    }

    @Override
    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        serviceInstancesOperations.updateServiceSyslogDrainUrl(serviceName, syslogDrainUrl);
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        serviceInstancesOperations.updateServiceTags(serviceName, tags);
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback, Duration uploadTimeout) {
        return packagesOperations.asyncUploadApplication(applicationName, file, callback, uploadTimeout);
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return packagesOperations.getUploadStatus(packageGuid);
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return tasksOperations.getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return tasksOperations.getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return tasksOperations.runTask(applicationName, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return tasksOperations.cancelTask(taskGuid);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return buildsOperations.createBuild(packageGuid);
    }

    @Override
    public CloudBuild getBuild(UUID packageGuid) {
        return buildsOperations.getBuild(packageGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return buildsOperations.getBuildsForApplication(applicationGuid);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return serviceInstancesOperations.getServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        return serviceInstancesOperations.getUserProvidedServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return serviceBindingsOperations.getServiceBindingParameters(guid);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return buildsOperations.getBuildsForPackage(packageGuid);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        return serviceInstancesOperations.getServiceInstancesWithoutAuxiliaryContentByNames(names);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        return serviceInstancesOperations.getServiceInstancesByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        return serviceInstancesOperations.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(labelSelector);
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        applicationsOperations.updateApplicationMetadata(guid, metadata);
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        serviceInstancesOperations.updateServiceInstanceMetadata(guid, metadata);
    }

    @Override
    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        serviceBindingsOperations.updateServiceBindingMetadata(guid, metadata);
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        return processesOperations.getCurrentDropletForApplication(applicationGuid);
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return packagesOperations.getPackage(packageGuid);
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return packagesOperations.getPackagesForApplication(applicationGuid);
    }

    @Override
    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        return rolesOperations.getUserRolesBySpaceAndUser(spaceGuid, userGuid);
    }

    @Override
    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        return packagesOperations.createDockerPackage(applicationGuid, dockerInfo);
    }

    @Override
    public CloudAsyncJob getAsyncJob(String jobId) {
        return jobsOperations.getAsyncJob(jobId);
    }

}
