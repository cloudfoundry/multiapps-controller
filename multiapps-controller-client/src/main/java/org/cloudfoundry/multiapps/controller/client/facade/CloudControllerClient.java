package org.cloudfoundry.multiapps.controller.client.facade;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
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
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;

/**
 * The interface defining operations making up the Cloud Foundry Java client's API.
 */
public interface CloudControllerClient {

    CloudSpace getTarget();

    /**
     * Add a private domain in the current organization.
     *
     * @param domainName the domain to add
     */
    void addDomain(String domainName);

    /**
     * Register a new route to the a domain.
     *
     * @param host the host of the route to register
     * @param domainName the domain of the route to register
     */
    void addRoute(String host, String domainName, String path);

    /**
     * Associate (provision) a service with an application.
     *
     * @param bindingName the binding name
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     * @return job id for async polling if present
     */
    Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName);

    /**
     * Associate (provision) a service with an application.
     *
     * @param bindingName the binding name
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     * @param parameters the binding parameters
     * @param updateServicesCallback callback used for error handling
     * @return job id for async polling if present
     */
    Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                         Map<String, Object> parameters, ApplicationServicesUpdateCallback updateServicesCallback);

    /**
     * Create application
     *
     * @param applicationToCreateDto the application's parameters used for its creation
     */
    void createApplication(ApplicationToCreateDto applicationToCreateDto);

    /**
     * Create a service instance.
     *
     * @param serviceInstance cloud service instance info
     */
    void createServiceInstance(CloudServiceInstance serviceInstance);

    /**
     * Create a service broker.
     *
     * @param serviceBroker cloud service broker info
     * @return job id for async poll
     */
    String createServiceBroker(CloudServiceBroker serviceBroker);

    /**
     * @param keyModel service-key cloud object
     * @param serviceInstanceName name of related service instance
     * @return the service-key object populated with new guid
     */
    CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName);

    /**
     * @param keyModel service-key cloud object
     * @param serviceInstanceName name of related service instance
     * @return job id for async polling if present
     */
    Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName);

    /**
     * Create a service key.
     *
     * @param serviceInstanceName name of service instance
     * @param serviceKeyName name of service-key
     * @param parameters parameters of service-key
     * @return job id for async polling if present
     */
    Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters);

    /**
     * Create a user-provided service instance.
     *
     * @param serviceInstance cloud service instance info
     */
    void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance);

    /**
     * Delete application.
     *
     * @param applicationName name of application
     */
    void deleteApplication(String applicationName);

    /**
     * Delete a private domain in the current organization.
     *
     * @param domainName the domain to delete
     */
    void deleteDomain(String domainName);

    /**
     * Delete routes that do not have any application which is assigned to them.
     */
    void deleteOrphanedRoutes();

    /**
     * Delete a registered route from the space of the current session.
     *
     * @param host the host of the route to delete
     * @param domainName the domain of the route to delete
     */
    void deleteRoute(String host, String domainName, String path);

    /**
     * Delete cloud service instance.
     *
     * @param serviceInstance name of service instance
     */
    void deleteServiceInstance(String serviceInstance);

    /**
     * @param serviceInstance {@link CloudServiceInstance}
     */
    void deleteServiceInstance(CloudServiceInstance serviceInstance);

    /**
     * Delete a service broker.
     *
     * @param name the service broker name
     * @return async job id
     */
    String deleteServiceBroker(String name);

    /**
     * Get a service binding
     *
     * @param serviceBindingId the id of the service binding
     * @return the service binding
     */
    CloudServiceBinding getServiceBinding(UUID serviceBindingId);

    /**
     * Delete a service binding.
     *
     * @param serviceInstanceName name of service instance
     * @param serviceKeyName name of service key
     * @return job id for async polling if present
     */
    Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName);

    /**
     * Delete a service binding.
     *
     * @param bindingGuid The GUID of the binding
     * @param serviceBindingOperationCallback callback used for error handling
     * @return job id for async polling if present
     */
    Optional<String> deleteServiceBinding(UUID bindingGuid, ServiceBindingOperationCallback serviceBindingOperationCallback);

    /**
     * Delete a service binding.
     *
     * @param bindingGuid The GUID of the binding
     * @return job id for async polling if present
     */
    Optional<String> deleteServiceBinding(UUID bindingGuid);

    /**
     * Get cloud application with the specified name.
     *
     * @param applicationName name of the app
     * @return the cloud application
     */
    CloudApplication getApplication(String applicationName);

    /**
     * Get cloud application with the specified name.
     *
     * @param applicationName name of the app
     * @param required if true, and organization is not found, throw an exception
     * @return the cloud application
     */
    CloudApplication getApplication(String applicationName, boolean required);

    /**
     * Get the GUID of the cloud application with the specified name.
     *
     * @param applicationName name of the app
     * @return the cloud application's guid
     */
    UUID getApplicationGuid(String applicationName);

    String getApplicationName(UUID applicationGuid);

    /**
     * Get application environment variables for the app with the specified name.
     *
     * @param applicationName name of the app
     * @return the cloud application environment variables
     */
    Map<String, String> getApplicationEnvironment(String applicationName);

    /**
     * Get application environment variables for the app with the specified GUID.
     *
     * @param applicationGuid GUID of the app
     * @return the cloud application environment variables
     */
    Map<String, String> getApplicationEnvironment(UUID applicationGuid);

    /**
     * Get application events.
     *
     * @param applicationName name of application
     * @return application events
     */
    List<CloudEvent> getApplicationEvents(String applicationName);

    List<CloudEvent> getEventsByActee(UUID uuid);

    /**
     * Get application instances info for application.
     *
     * @param app the application.
     * @return instances info
     */
    InstancesInfo getApplicationInstances(CloudApplication app);

    InstancesInfo getApplicationInstances(UUID applicationGuid);

    CloudProcess getApplicationProcess(UUID applicationGuid);

    List<CloudRoute> getApplicationRoutes(UUID applicationGuid);

    boolean getApplicationSshEnabled(UUID applicationGuid);

    Map<String, Boolean> getApplicationFeatures(UUID applicationGuid);

    /**
     * Get all applications in the currently targeted space. This method has EXTREMELY poor performance for spaces with a lot of
     * applications.
     *
     * @return list of applications
     */
    List<CloudApplication> getApplications();

    /**
     * Gets the default domain for the current org, which is the first shared domain.
     *
     * @return the default domain
     */
    CloudDomain getDefaultDomain();

    /**
     * Get list of all domain shared and private domains.
     *
     * @return list of domains
     */
    List<CloudDomain> getDomains();

    /**
     * Get list of all domain registered for the current organization.
     *
     * @return list of domains
     */
    List<CloudDomain> getDomainsForOrganization();

    /**
     * Get system events.
     *
     * @return all system events
     */
    List<CloudEvent> getEvents();

    /**
     * Get list of all private domains.
     *
     * @return list of private domains
     */
    List<CloudDomain> getPrivateDomains();

    /**
     * Get the info for all routes for a domain.
     *
     * @param domainName the domain the routes belong to
     * @return list of routes
     */
    List<CloudRoute> getRoutes(String domainName);

    /**
     * Get a service broker.
     *
     * @param name the service broker name
     * @return the service broker
     */
    CloudServiceBroker getServiceBroker(String name);

    /**
     * Get a service broker.
     *
     * @param name the service broker name
     * @param required if true, and organization is not found, throw an exception
     * @return the service broker
     */
    CloudServiceBroker getServiceBroker(String name, boolean required);

    /**
     * Get all service brokers.
     *
     * @return the service brokers
     */
    List<CloudServiceBroker> getServiceBrokers();

    /**
     * Get the GUID of a service instance.
     *
     * @param serviceInstanceName the name of the service instance
     * @return the service instance GUID
     */
    UUID getRequiredServiceInstanceGuid(String serviceInstanceName);

    /**
     * Get a service instance.
     *
     * @param serviceInstanceName name of the service instance
     * @return the service instance info
     */
    CloudServiceInstance getServiceInstance(String serviceInstanceName);

    /**
     * Get a service instance.
     *
     * @param serviceInstanceName name of the service instance
     * @param required if true, and service instance is not found, throw an exception
     * @return the service instance info
     */
    CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required);

    /**
     * Get a service instance name.
     *
     * @param serviceInstanceGuid GUID of the service instance
     * @return the service instance name
     */
    String getServiceInstanceName(UUID serviceInstanceGuid);

    /**
     * Get a service instance.
     *
     * @param serviceInstanceName name of the service instance
     * @return the service instance info
     */
    CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName);

    /**
     * Get a service instance.
     *
     * @param serviceInstanceName name of the service instance
     * @param required if true, and service instance is not found, throw an exception
     * @return the service instance info
     */
    CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required);

    /**
     * Get the bindings for a particular service instance.
     *
     * @param serviceInstanceGuid the GUID of the service instance
     * @return the bindings
     */
    List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid);

    /**
     * Get the bindings for a particular application.
     *
     * @param applicationGuid the GUID of the application
     * @return the bindings
     */
    List<CloudServiceBinding> getAppBindings(UUID applicationGuid);

    /**
     * Get the binding between an application and a service instance.
     *
     * @param applicationId the GUID of the application
     * @param serviceInstanceGuid the GUID of the service instance
     * @return the binding
     */
    CloudServiceBinding getServiceBindingForApplication(UUID applicationId, UUID serviceInstanceGuid);

    /**
     * Get all service instance parameters.
     *
     * @param guid The service instance guid
     * @return service instance parameters in key-value pairs
     */
    Map<String, Object> getServiceInstanceParameters(UUID guid);

    /**
     * Get all user-provided service instance parameters
     *
     * @param guid The service instance guid
     * @return user-provided service instance parameters in key-value pairs
     */
    Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid);

    /**
     * Get all service binding parameters.
     *
     * @param guid The service binding guid
     * @return service binding parameters in key-value pairs
     */
    Map<String, Object> getServiceBindingParameters(UUID guid);

    /**
     * Get a service key.
     *
     * @param serviceInstanceName The service instance name
     * @param serviceKeyName The service key name
     * @return the service key info
     */
    CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName);

    /**
     * Get service keys for a service instance.
     *
     * @param serviceInstanceName name containing service keys
     * @return the service keys info
     */
    List<CloudServiceKey> getServiceKeys(String serviceInstanceName);

    List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName);

    /**
     * Get service keys for a service instance.
     *
     * @param serviceInstance instance containing service keys
     * @return the service keys info
     */
    List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance);

    List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance);

    /**
     * Get all service offerings.
     *
     * @return list of service offerings
     */
    List<CloudServiceOffering> getServiceOfferings();

    /**
     * Get list of all shared domains.
     *
     * @return list of shared domains
     */
    List<CloudDomain> getSharedDomains();

    /**
     * Get a stack by name.
     *
     * @param name the name of the stack to get
     * @return the stack
     */
    CloudStack getStack(String name);

    /**
     * Get a stack by name.
     *
     * @param name the name of the stack to get
     * @param required if true, and organization is not found, throw an exception
     * @return the stack, or null if not found
     */
    CloudStack getStack(String name, boolean required);

    /**
     * Get the list of stacks available for staging applications.
     *
     * @return the list of available stacks
     */
    List<CloudStack> getStacks();

    /**
     * Rename an application.
     *
     * @param applicationName the current name
     * @param newName the new name
     */
    void rename(String applicationName, String newName);

    /**
     * Restart application.
     *
     * @param applicationName name of application
     */
    void restartApplication(String applicationName);

    /**
     * Start application. May return starting info if the response obtained after the start request contains headers . If the response does
     * not contain headers, null is returned instead.
     *
     * @param applicationName name of application
     */
    void startApplication(String applicationName);

    /**
     * Stop application.
     *
     * @param applicationName name of application
     */
    void stopApplication(String applicationName);

    /**
     * Un-associate (unprovision) a service from an application.
     *
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     * @param applicationServicesUpdateCallback callback used for error handling
     * @return job id for async polling if present
     */
    Optional<String> unbindServiceInstance(String applicationName, String serviceInstanceName,
                                           ApplicationServicesUpdateCallback applicationServicesUpdateCallback);

    /**
     * Un-associate (unprovision) a service from an application.
     *
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     * @return job id for async polling if present
     */
    Optional<String> unbindServiceInstance(String applicationName, String serviceInstanceName);

    /**
     * Un-associate (unprovision) a service from an application.
     *
     * @param applicationGuid the application guid
     * @param serviceInstanceGuid the service instance guid
     * @return job id for async polling if present
     */
    Optional<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid);

    /**
     * Update application disk quota.
     *
     * @param applicationName name of application
     * @param disk new disk setting in MB
     */
    void updateApplicationDiskQuota(String applicationName, int disk);

    /**
     * Update application env using a map where the key specifies the name of the environment variable and the value the value of the
     * environment variable..
     *
     * @param applicationName name of application
     * @param env map of environment settings
     */
    void updateApplicationEnv(String applicationName, Map<String, String> env);

    /**
     * Update application instances.
     *
     * @param applicationName name of application
     * @param instances number of instances to use
     */
    void updateApplicationInstances(String applicationName, int instances);

    /**
     * Update application memory.
     *
     * @param applicationName name of application
     * @param memory new memory setting in MB
     */
    void updateApplicationMemory(String applicationName, int memory);

    /**
     * Update application staging information.
     *
     * @param applicationName name of appplication
     * @param staging staging information for the app
     */
    void updateApplicationStaging(String applicationName, Staging staging);

    /**
     * Update application Routes.
     *
     * @param applicationName name of application
     * @param routes list of route summary info for the routes the app should use
     */
    void updateApplicationRoutes(String applicationName, Set<CloudRoute> routes);

    /**
     * Update a service broker (unchanged forces catalog refresh).
     *
     * @param serviceBroker cloud service broker info
     * @return async job id
     */
    String updateServiceBroker(CloudServiceBroker serviceBroker);

    /**
     * Service plans are private by default when a service broker's catalog is fetched/updated. This method will update the visibility of
     * all plans for a broker to either public or private.
     *
     * @param name the service broker name
     * @param visibility true for public, false for private
     */
    void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility);

    void updateServicePlan(String serviceName, String planName);

    void updateServiceParameters(String serviceName, Map<String, Object> parameters);

    void updateServiceTags(String serviceName, List<String> tags);

    void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl);

    CloudPackage asyncUploadApplicationWithExponentialBackoff(String applicationName, Path file, UploadStatusCallback callback,
                                                              Duration overrideTimeout);

    Upload getUploadStatus(UUID packageGuid);

    CloudTask getTask(UUID taskGuid);

    /**
     * Get the list of one-off tasks currently known for the given application.
     *
     * @param applicationName the application to look for tasks
     * @return the list of known tasks
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    List<CloudTask> getTasks(String applicationName);

    /**
     * Run a one-off task on an application.
     *
     * @param applicationName the application to run the task on
     * @param task the task to run
     * @return the ran task
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    CloudTask runTask(String applicationName, CloudTask task);

    /**
     * Cancel the given task.
     *
     * @param taskGuid the GUID of the task to cancel
     * @return the cancelled task
     */
    CloudTask cancelTask(UUID taskGuid);

    CloudBuild createBuild(UUID packageGuid);

    CloudBuild getBuild(UUID buildGuid);

    void bindDropletToApp(UUID dropletGuid, UUID applicationGuid);

    List<CloudBuild> getBuildsForApplication(UUID applicationGuid);

    List<CloudBuild> getBuildsForPackage(UUID packageGuid);

    List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector);

    void updateApplicationMetadata(UUID guid, Metadata metadata);

    void updateServiceInstanceMetadata(UUID guid, Metadata metadata);

    void updateServiceBindingMetadata(UUID guid, Metadata metadata);

    List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names);

    List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector);

    List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector);

    DropletInfo getCurrentDropletForApplication(UUID applicationGuid);

    CloudPackage getPackage(UUID packageGuid);

    List<CloudPackage> getPackagesForApplication(UUID applicationGuid);

    Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid);

    CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo);

    CloudAsyncJob getAsyncJob(String jobId);

}
