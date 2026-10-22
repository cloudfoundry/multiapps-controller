package org.cloudfoundry.multiapps.controller.process.client;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.ApplicationServicesUpdateCallback;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.ServiceBindingOperationCallback;
import org.cloudfoundry.multiapps.controller.client.facade.UploadStatusCallback;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
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
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

/**
 * THROWAWAY SPIKE. Dry-run decorator over {@link CloudControllerClient}: every read forwards to the
 * delegate; every mutation is recorded into the {@link DryRunChangeSet} process variable and NOT
 * executed against CF. Some stub returns are null and get patched reactively (plan corners #2/#3).
 */
public class DryRunCloudControllerClient implements CloudControllerClient {

    private final CloudControllerClient delegate;
    private final ProcessContext context;
    // Tracks app names "created" during this dry run so reads can return a stub instead of hitting CF
    private final Set<String> createdAppNames = new HashSet<>();

    public DryRunCloudControllerClient(CloudControllerClient delegate, ProcessContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    private void record(Function<DryRunChangeSet, List<String>> bucket, String entry) {
        DryRunChangeSet cs = context.getVariable(Variables.DRY_RUN_CHANGE_SET);
        if (cs == null) {
            cs = new DryRunChangeSet();
        }
        List<String> list = bucket.apply(cs);
        if (!list.contains(entry)) {
            list.add(entry);
        }
        context.setVariable(Variables.DRY_RUN_CHANGE_SET, cs);
    }

    private CloudApplication stubApp(String name) {
        return ImmutableCloudApplication.builder()
                                        .name(name)
                                        .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                        .build();
    }

    @Override
    public CloudSpace getTarget()
    {
        return delegate.getTarget();
    }

    @Override
    public void addDomain(String domainName)
    {
        record(cs -> cs.routesCreate, domainName);
    }

    @Override
    public void addRoute(String host, String domainName, String path)
    {
        record(cs -> cs.routesCreate, host + "." + domainName);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName)
    {
        record(cs -> cs.servicesCreate, serviceInstanceName);
        return Optional.empty();
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters, ApplicationServicesUpdateCallback updateServicesCallback)
    {
        record(cs -> cs.servicesCreate, serviceInstanceName);
        return Optional.empty();
    }

    @Override
    public void createApplication(ApplicationToCreateDto applicationToCreateDto)
    {
        record(cs -> cs.appsCreate, applicationToCreateDto.getName());
        createdAppNames.add(applicationToCreateDto.getName());
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance)
    {
        record(cs -> cs.servicesCreate, serviceInstance.getName());
    }

    @Override
    public String createServiceBroker(CloudServiceBroker serviceBroker)
    {
        record(cs -> cs.servicesCreate, serviceBroker.getName());
        return "dry-run-job-id";
    }

    @Override
    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName)
    {
        record(cs -> cs.servicesCreate, serviceInstanceName);
        return null;
    }

    @Override
    public Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName)
    {
        record(cs -> cs.servicesCreate, serviceInstanceName);
        return Optional.empty();
    }

    @Override
    public Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters)
    {
        record(cs -> cs.servicesCreate, serviceInstanceName);
        return Optional.empty();
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance)
    {
        record(cs -> cs.servicesCreate, serviceInstance.getName());
    }

    @Override
    public void deleteApplication(String applicationName)
    {
        record(cs -> cs.appsDelete, applicationName);
    }

    @Override
    public void deleteDomain(String domainName)
    {
        record(cs -> cs.routesDelete, domainName);
    }

    @Override
    public void deleteOrphanedRoutes()
    {
        record(cs -> cs.routesDelete, "deleteOrphanedRoutes");
    }

    @Override
    public void deleteRoute(String host, String domainName, String path)
    {
        record(cs -> cs.routesDelete, host + "." + domainName);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName)
    {
        record(cs -> cs.servicesDelete, serviceInstanceName);
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance)
    {
        record(cs -> cs.servicesDelete, serviceInstance.getName());
    }

    @Override
    public String deleteServiceBroker(String name)
    {
        record(cs -> cs.servicesDelete, name);
        return "dry-run-job-id";
    }

    @Override
    public CloudServiceBinding getServiceBinding(UUID serviceBindingId)
    {
        return delegate.getServiceBinding(serviceBindingId);
    }

    @Override
    public Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName) {
        return Optional.empty();
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid, ServiceBindingOperationCallback serviceBindingOperationCallback) {
        return Optional.empty();
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid) {
        return Optional.empty();
    }

    @Override
    public CloudApplication getApplication(String applicationName)
    {
        if (createdAppNames.contains(applicationName)) {
            return stubApp(applicationName);
        }
        return delegate.getApplication(applicationName);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required)
    {
        if (createdAppNames.contains(applicationName)) {
            return stubApp(applicationName);
        }
        return delegate.getApplication(applicationName, required);
    }

    @Override
    public UUID getApplicationGuid(String applicationName)
    {
        if (createdAppNames.contains(applicationName)) {
            return stubApp(applicationName).getGuid();
        }
        return delegate.getApplicationGuid(applicationName);
    }

    @Override
    public String getApplicationName(UUID uuid)
    {
        return delegate.getApplicationName(uuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName)
    {
        if (createdAppNames.contains(applicationName)) {
            return Map.of();
        }
        return delegate.getApplicationEnvironment(applicationName);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid)
    {
        return delegate.getApplicationEnvironment(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName)
    {
        return delegate.getApplicationEvents(applicationName);
    }

    @Override
    public List<CloudEvent> getEventsByActee(UUID uuid)
    {
        return delegate.getEventsByActee(uuid);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app)
    {
        return delegate.getApplicationInstances(app);
    }

    @Override
    public InstancesInfo getApplicationInstances(UUID applicationGuid)
    {
        return delegate.getApplicationInstances(applicationGuid);
    }

    @Override
    public CloudProcess getApplicationProcess(UUID applicationGuid)
    {
        return delegate.getApplicationProcess(applicationGuid);
    }

    @Override
    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid)
    {
        return delegate.getApplicationRoutes(applicationGuid);
    }

    @Override
    public boolean getApplicationSshEnabled(UUID applicationGuid)
    {
        return delegate.getApplicationSshEnabled(applicationGuid);
    }

    @Override
    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid)
    {
        return delegate.getApplicationFeatures(applicationGuid);
    }

    @Override
    public List<CloudApplication> getApplications()
    {
        return delegate.getApplications();
    }

    @Override
    public CloudDomain getDefaultDomain()
    {
        return delegate.getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getDomains()
    {
        return delegate.getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization()
    {
        return delegate.getDomainsForOrganization();
    }

    @Override
    public List<CloudEvent> getEvents()
    {
        return delegate.getEvents();
    }

    @Override
    public List<CloudDomain> getPrivateDomains()
    {
        return delegate.getPrivateDomains();
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName)
    {
        return delegate.getRoutes(domainName);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name)
    {
        return delegate.getServiceBroker(name);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required)
    {
        return delegate.getServiceBroker(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers()
    {
        return delegate.getServiceBrokers();
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name)
    {
        return delegate.getRequiredServiceInstanceGuid(name);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName)
    {
        return delegate.getServiceInstance(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required)
    {
        return delegate.getServiceInstance(serviceInstanceName, required);
    }

    @Override
    public String getServiceInstanceName(UUID serviceInstanceGuid)
    {
        return delegate.getServiceInstanceName(serviceInstanceGuid);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName)
    {
        return delegate.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required)
    {
        return delegate.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, required);
    }

    @Override
    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid)
    {
        return delegate.getServiceAppBindings(serviceInstanceGuid);
    }

    @Override
    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid)
    {
        return delegate.getAppBindings(applicationGuid);
    }

    @Override
    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationGuid, UUID serviceInstanceGuid)
    {
        return delegate.getServiceBindingsForApplication(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid)
    {
        return delegate.getServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid)
    {
        return delegate.getUserProvidedServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid)
    {
        return delegate.getServiceBindingParameters(guid);
    }

    @Override
    public CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName)
    {
        return delegate.getServiceKey(serviceInstanceName, serviceKeyName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName)
    {
        return delegate.getServiceKeys(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName)
    {
        return delegate.getServiceKeysWithCredentials(serviceInstanceName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance)
    {
        return delegate.getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance)
    {
        return delegate.getServiceKeysWithCredentials(serviceInstance);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings()
    {
        return delegate.getServiceOfferings();
    }

    @Override
    public List<CloudDomain> getSharedDomains()
    {
        return delegate.getSharedDomains();
    }

    @Override
    public CloudStack getStack(String name)
    {
        return delegate.getStack(name);
    }

    @Override
    public CloudStack getStack(String name, boolean required)
    {
        return delegate.getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks()
    {
        return delegate.getStacks();
    }

    @Override
    public void rename(String applicationName, String newName)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void restartApplication(String applicationName)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void startApplication(String applicationName)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void stopApplication(String applicationName)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName,
                                              ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        return List.of();
    }

    @Override
    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        return List.of();
    }

    @Override
    public List<String> unbindServiceInstance(UUID appGuid, UUID serviceGuid) {
        return List.of();
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> routes)
    {
        record(cs -> cs.appsUpdate, applicationName);
    }

    @Override
    public String updateServiceBroker(CloudServiceBroker serviceBroker)
    {
        record(cs -> cs.servicesUpdate, serviceBroker.getName());
        return "dry-run-job-id";
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility)
    {
        record(cs -> cs.servicesUpdate, name);
    }

    @Override
    public void updateServicePlan(String serviceName, String servicePlan)
    {
        record(cs -> cs.servicesUpdate, serviceName);
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters)
    {
        record(cs -> cs.servicesUpdate, serviceName);
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags)
    {
        record(cs -> cs.servicesUpdate, serviceName);
    }

    @Override
    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl)
    {
        record(cs -> cs.servicesUpdate, serviceName);
    }

    @Override
    public CloudPackage asyncUploadApplicationWithExponentialBackoff(String applicationName, Path file, UploadStatusCallback callback,
                                                                     Duration overrideTimeout)
    {
        record(cs -> cs.appsUpdate, applicationName);
        return null;
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid)
    {
        return delegate.getUploadStatus(packageGuid);
    }

    @Override
    public CloudTask getTask(UUID taskGuid)
    {
        return delegate.getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName)
    {
        return delegate.getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task)
    {
        record(cs -> cs.appsCreate, applicationName);
        return null;
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid)
    {
        return null;
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid)
    {
        return null;
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid)
    {
        return delegate.getBuild(buildGuid);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid)
    {
        record(cs -> cs.appsCreate, "bindDropletToApp" + " " + dropletGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid)
    {
        return delegate.getBuildsForApplication(applicationGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid)
    {
        return delegate.getBuildsForPackage(packageGuid);
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector)
    {
        return delegate.getApplicationsByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names)
    {
        return delegate.getServiceInstancesWithoutAuxiliaryContentByNames(names);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector)
    {
        return delegate.getServiceInstancesByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector)
    {
        return delegate.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(labelSelector);
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata)
    {
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
    }

    @Override
    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid)
    {
        return delegate.getCurrentDropletForApplication(applicationGuid);
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid)
    {
        return delegate.getPackage(packageGuid);
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid)
    {
        return delegate.getPackagesForApplication(applicationGuid);
    }

    @Override
    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid)
    {
        return delegate.getUserRolesBySpaceAndUser(spaceGuid, userGuid);
    }

    @Override
    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo)
    {
        record(cs -> cs.appsCreate, "createDockerPackage" + " " + applicationGuid);
        return null;
    }

    @Override
    public CloudAsyncJob getAsyncJob(String jobId)
    {
        return delegate.getAsyncJob(jobId);
    }

}
