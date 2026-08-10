package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
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
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ApplicationMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/**
 * A {@link CloudControllerRestClient} implementation that talks to the Cloud Controller v3 REST API directly through a blocking Spring
 * {@link RestClient}, with no dependency on the OSS cf-java-client.
 * <p>
 * This is the target implementation of the "migrate off cf-java-client" PoC. It is selected in
 * {@link CloudControllerRestClientFactory} via a feature flag; when the flag is off, the OSS-backed
 * {@link CloudControllerRestClientImpl} is used instead, so the two can be A/B compared against the same live Cloud Foundry.
 * <p>
 * SCAFFOLD STATE: every operation currently throws {@link UnsupportedOperationException}. Operations are being filled in
 * resource-group by resource-group per docs/cf-java-client-migration/02-endpoint-inventory.md. The shared machinery (pagination walker,
 * async job poller, JSON model) lands first and is reused by every operation.
 */
public class CloudControllerRestClientV3Impl implements CloudControllerRestClient {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int DEFAULT_PAGE_SIZE = 5000;
    // Matches the OSS impl's DELETE_JOB_TIMEOUT for async resource deletions.
    private static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);

    private final URL v3ApiUrl;
    private final OAuthClient oAuthClient;
    private final CloudSpace target;
    private final RestClient restClient;
    private final CloudControllerV3Client cc;
    private final BuildsV3Operations buildsOps;
    private final DomainsV3Operations domainsOps;
    private final EventsV3Operations eventsOps;
    private final JobsV3Operations jobsOps;
    private final PackagesV3Operations packagesOps;
    private final ProcessesV3Operations processesOps;
    private final RolesV3Operations rolesOps;
    private final RoutesV3Operations routesOps;
    private final ServiceBindingsV3Operations serviceBindingsOps;
    private final ServiceBrokersV3Operations serviceBrokersOps;
    private final ServiceInstancesV3Operations serviceInstancesOps;
    private final ServiceKeysV3Operations serviceKeysOps;
    private final ServiceOfferingsV3Operations serviceOfferingsOps;
    private final ServicePlansV3Operations servicePlansOps;
    private final StacksV3Operations stacksOps;
    private final TasksV3Operations tasksOps;

    public CloudControllerRestClientV3Impl(URL v3ApiUrl, OAuthClient oAuthClient, CloudSpace target, RestClient restClient) {
        this(v3ApiUrl, oAuthClient, target, restClient, null);
    }

    public CloudControllerRestClientV3Impl(URL v3ApiUrl, OAuthClient oAuthClient, CloudSpace target, RestClient restClient,
                                           java.util.function.Function<java.time.Duration, RestClient> uploadRestClientFactory) {
        this(v3ApiUrl, oAuthClient, target, restClient, new CloudControllerV3Client(restClient), uploadRestClientFactory);
    }

    public CloudControllerRestClientV3Impl(URL v3ApiUrl, OAuthClient oAuthClient, CloudSpace target, RestClient restClient,
                                           CloudControllerV3Client cc,
                                           java.util.function.Function<java.time.Duration, RestClient> uploadRestClientFactory) {
        this.v3ApiUrl = v3ApiUrl;
        this.oAuthClient = oAuthClient;
        this.target = target;
        this.restClient = restClient;
        this.cc = cc;
        this.buildsOps = new BuildsV3Operations(cc, target);
        this.domainsOps = new DomainsV3Operations(cc, target);
        this.eventsOps = new EventsV3Operations(cc, target);
        this.jobsOps = new JobsV3Operations(cc, target);
        this.packagesOps = new PackagesV3Operations(cc, target, uploadRestClientFactory);
        this.processesOps = new ProcessesV3Operations(cc, target);
        this.rolesOps = new RolesV3Operations(cc, target);
        this.routesOps = new RoutesV3Operations(cc, target);
        this.serviceBindingsOps = new ServiceBindingsV3Operations(cc, target);
        this.serviceBrokersOps = new ServiceBrokersV3Operations(cc, target);
        this.serviceInstancesOps = new ServiceInstancesV3Operations(cc, target);
        this.serviceKeysOps = new ServiceKeysV3Operations(cc, target);
        this.serviceOfferingsOps = new ServiceOfferingsV3Operations(cc, target);
        this.servicePlansOps = new ServicePlansV3Operations(cc, target);
        this.stacksOps = new StacksV3Operations(cc, target);
        this.tasksOps = new TasksV3Operations(cc, target);
    }

    private static UnsupportedOperationException notImplemented() {
        StackWalker.StackFrame caller = StackWalker.getInstance()
                                                   .walk(frames -> frames.skip(1)
                                                                         .findFirst()
                                                                         .orElse(null));
        String operation = caller == null ? "operation" : caller.getMethodName();
        return new UnsupportedOperationException(
            "CloudControllerRestClientV3Impl." + operation + " is not implemented yet (cf-java-client migration PoC scaffold).");
    }

    @Override
    public CloudSpace getTarget() {
        return target;
    }

    @Override
    public void addDomain(String domainName) {
        domainsOps.addDomain(domainName);
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        routesOps.addRoute(host, domainName, path);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        return serviceBindingsOps.bindServiceInstance(bindingName, applicationName, serviceInstanceName);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters) {
        return serviceBindingsOps.bindServiceInstance(bindingName, applicationName, serviceInstanceName, parameters);
    }

    @Override
    public void createApplication(ApplicationToCreateDto dto) {
        if (target == null || target.getGuid() == null) {
            throw new CloudOperationException(HttpStatus.BAD_REQUEST, "Bad Request", "A target space is required to create an application.");
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", dto.getName());
        body.put("lifecycle", buildLifecycle(dto.getStaging()));
        body.put("relationships", Map.of("space", Map.of("data", Map.of("guid", target.getGuid()
                                                                                       .toString()))));
        if (dto.getEnv() != null) {
            body.put("environment_variables", dto.getEnv());
        }
        if (dto.getMetadata() != null) {
            body.put("metadata", Map.of("labels", dto.getMetadata()
                                                     .getLabels(),
                                        "annotations", dto.getMetadata()
                                                          .getAnnotations()));
        }
        V3Application created = cc.getRestClient()
                                 .post()
                                 .uri("/v3/apps")
                                 .body(body)
                                 .retrieve()
                                 .body(V3Application.class);
        UUID appGuid = UUID.fromString(created.guid());
        // Follow-up attributes, mirroring the OSS updateApplicationAttributes: staging/process, memory+disk scale, then routes.
        if (dto.getStaging() != null) {
            processesOps.updateApplicationStaging(dto.getName(), dto.getStaging());
        }
        Map<String, Object> scale = new java.util.HashMap<>();
        if (dto.getMemoryInMb() != null) {
            scale.put("memory_in_mb", dto.getMemoryInMb());
        }
        if (dto.getDiskQuotaInMb() != null) {
            scale.put("disk_in_mb", dto.getDiskQuotaInMb());
        }
        if (!scale.isEmpty()) {
            scaleWebProcess(appGuid, scale);
        }
        if (dto.getRoutes() != null && !dto.getRoutes()
                                           .isEmpty()) {
            updateApplicationRoutes(dto.getName(), dto.getRoutes());
        }
    }

    // Builds the CF v3 lifecycle object: docker when DockerInfo is present, otherwise buildpack/cnb with buildpacks + stack.
    private Map<String, Object> buildLifecycle(Staging staging) {
        if (staging == null) {
            return Map.of("type", "buildpack", "data", Map.of());
        }
        if (staging.getDockerInfo() != null) {
            return Map.of("type", "docker", "data", Map.of());
        }
        String type = staging.getLifecycleType() != null ? staging.getLifecycleType()
                                                                  .name()
                                                                  .toLowerCase()
            : "buildpack";
        Map<String, Object> data = new java.util.HashMap<>();
        if (staging.getBuildpacks() != null) {
            data.put("buildpacks", staging.getBuildpacks());
        }
        if (staging.getStackName() != null) {
            data.put("stack", staging.getStackName());
        }
        return Map.of("type", type, "data", data);
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        serviceInstancesOps.createServiceInstance(serviceInstance);
    }

    @Override
    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        return serviceBrokersOps.createServiceBroker(serviceBroker);
    }

    @Override
    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        return serviceKeysOps.createAndFetchServiceKey(keyModel, getServiceInstance(serviceInstanceName));
    }

    @Override
    public Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        return serviceKeysOps.createServiceKey(keyModel, getServiceInstance(serviceInstanceName));
    }

    @Override
    public Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        return serviceKeysOps.createServiceKey(getServiceInstance(serviceInstanceName), serviceKeyName, parameters);
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        serviceInstancesOps.createUserProvidedServiceInstance(serviceInstance);
    }

    @Override
    public void deleteApplication(String applicationName) {
        UUID applicationGuid = getApplicationGuid(applicationName);
        var response = cc.getRestClient()
                         .delete()
                         .uri("/v3/apps/{guid}", applicationGuid)
                         .retrieve()
                         .toBodilessEntity();
        cc.followAsyncJob(response, DELETE_JOB_TIMEOUT);
    }

    @Override
    public void deleteDomain(String domainName) {
        domainsOps.deleteDomain(domainName);
    }

    @Override
    public void deleteOrphanedRoutes() {
        routesOps.deleteOrphanedRoutes();
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        routesOps.deleteRoute(host, domainName, path);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        serviceInstancesOps.deleteServiceInstance(serviceInstanceName);
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        serviceInstancesOps.deleteServiceInstance(serviceInstance);
    }

    @Override
    public String deleteServiceBroker(String name) {
        return serviceBrokersOps.deleteServiceBroker(name);
    }

    @Override
    public Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName) {
        // Mirrors the OSS impl: resolve the named service key, then delete its binding by GUID.
        CloudServiceKey serviceKey = getServiceKey(serviceInstanceName, serviceKeyName);
        if (serviceKey == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service key " + serviceKeyName + " not found.");
        }
        return deleteServiceBinding(serviceKey.getGuid());
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID bindingGuid) {
        return serviceBindingsOps.deleteServiceBinding(bindingGuid);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        V3Application app = findApplicationByName(applicationName);
        if (app == null) {
            if (required) {
                throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
            }
            return null;
        }
        return V3ApplicationMapper.toCloudApplication(app, target);
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        return getApplication(applicationName).getGuid();
    }

    @Override
    public String getApplicationName(UUID applicationGuid) {
        V3Application app = cc.get("/v3/apps/" + applicationGuid, V3Application.class);
        return app == null ? null : app.name();
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        V3Application.V3EnvironmentVariables env = cc.get("/v3/apps/" + applicationGuid + "/environment_variables",
                                                          V3Application.V3EnvironmentVariables.class);
        return env == null || env.var() == null ? Map.of() : env.var();
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return getApplicationEnvironment(getApplicationGuid(applicationName));
    }

    @Override
    public List<CloudApplication> getApplications() {
        return listApplications(applicationsQuery(null)).stream()
                                                        .map(app -> V3ApplicationMapper.toCloudApplication(app, target))
                                                        .toList();
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        String query = applicationsQuery(null);
        if (labelSelector != null) {
            query = query + "&label_selector=" + labelSelector;
        }
        return listApplications(query).stream()
                                      .map(app -> V3ApplicationMapper.toCloudApplication(app, target))
                                      .toList();
    }

    private V3Application findApplicationByName(String applicationName) {
        List<V3Application> apps = listApplications(applicationsQuery(applicationName));
        return apps.isEmpty() ? null : apps.get(0);
    }

    private List<V3Application> listApplications(String query) {
        return cc.list(query, new ParameterizedTypeReference<V3ListResponse<V3Application>>() {
        });
    }

    // Applications in the target space, optionally filtered by exact name.
    private String applicationsQuery(String name) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + DEFAULT_PAGE_SIZE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        if (name != null) {
            query.append("&names=")
                 .append(name);
        }
        return query.toString();
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return eventsOps.getApplicationEvents(applicationName);
    }

    @Override
    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        return eventsOps.getEventsByTarget(uuid);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return processesOps.getApplicationInstances(app);
    }

    @Override
    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        return processesOps.getApplicationInstances(applicationGuid);
    }

    @Override
    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        return processesOps.getApplicationProcess(applicationGuid);
    }

    @Override
    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        return routesOps.getApplicationRoutes(applicationGuid);
    }

    @Override
    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        return processesOps.getApplicationSshEnabled(applicationGuid);
    }

    @Override
    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        return processesOps.getApplicationFeatures(applicationGuid);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return domainsOps.getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getDomains() {
        return domainsOps.getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return domainsOps.getDomainsForOrganization();
    }

    @Override
    public List<CloudEvent> getEvents() {
        return eventsOps.getEvents();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return domainsOps.getPrivateDomains();
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return routesOps.getRoutes(domainName);
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        return serviceInstancesOps.getRequiredServiceInstanceGuid(name);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return serviceInstancesOps.getServiceInstance(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        return serviceInstancesOps.getServiceInstance(serviceInstanceName, required);
    }

    @Override
    public String getServiceInstanceName(UUID serviceInstanceGuid) {
        return serviceInstancesOps.getServiceInstanceName(serviceInstanceGuid);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName) {
        return serviceInstancesOps.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required) {
        return serviceInstancesOps.getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, required);
    }

    @Override
    public CloudServiceBinding getServiceBinding(UUID serviceBindingGuid) {
        return serviceBindingsOps.getServiceBinding(serviceBindingGuid);
    }

    @Override
    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        return serviceBindingsOps.getServiceAppBindings(serviceInstanceGuid);
    }

    @Override
    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        return serviceBindingsOps.getAppBindings(applicationGuid);
    }

    @Override
    public List<CloudServiceBinding> getServiceBindingsForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        return serviceBindingsOps.getServiceBindingsForApplication(applicationId, serviceInstanceGuid);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return serviceBrokersOps.getServiceBroker(name);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return serviceBrokersOps.getServiceBroker(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return serviceBrokersOps.getServiceBrokers();
    }

    @Override
    public CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName) {
        return serviceKeysOps.getServiceKey(getServiceInstance(serviceInstanceName), serviceKeyName);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        return serviceKeysOps.getServiceKeys(getServiceInstance(serviceInstanceName));
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName) {
        return serviceKeysOps.getServiceKeysWithCredentials(getServiceInstance(serviceInstanceName));
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return serviceKeysOps.getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        return serviceKeysOps.getServiceKeysWithCredentials(serviceInstance);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return serviceOfferingsOps.getServiceOfferings();
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return domainsOps.getSharedDomains();
    }

    @Override
    public CloudStack getStack(String name) {
        return stacksOps.getStack(name);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return stacksOps.getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks() {
        return stacksOps.getStacks();
    }

    @Override
    public void startApplication(String applicationName) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .post()
          .uri("/v3/apps/{guid}/actions/start", guid)
          .retrieve()
          .toBodilessEntity();
    }

    @Override
    public void restartApplication(String applicationName) {
        stopApplication(applicationName);
        startApplication(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .post()
          .uri("/v3/apps/{guid}/actions/stop", guid)
          .retrieve()
          .toBodilessEntity();
    }

    @Override
    public void rename(String applicationName, String newName) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .patch()
          .uri("/v3/apps/{guid}", guid)
          .body(Map.of("name", newName))
          .retrieve()
          .toBodilessEntity();
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of("instances", instances));
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of("memory_in_mb", memory));
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of("disk_in_mb", disk));
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .patch()
          .uri("/v3/apps/{guid}/environment_variables", guid)
          .body(Map.of("var", env))
          .retrieve()
          .toBodilessEntity();
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        cc.getRestClient()
          .patch()
          .uri("/v3/apps/{guid}/relationships/current_droplet", applicationGuid)
          .body(Map.of("data", Map.of("guid", dropletGuid.toString())))
          .retrieve()
          .toBodilessEntity();
    }

    private void scaleWebProcess(UUID applicationGuid, Map<String, Object> scaleBody) {
        cc.getRestClient()
          .post()
          .uri("/v3/apps/{guid}/processes/web/actions/scale", applicationGuid)
          .body(scaleBody)
          .retrieve()
          .toBodilessEntity();
    }

    @Override
    public List<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        return serviceBindingsOps.unbindServiceInstance(applicationName, serviceInstanceName);
    }

    @Override
    public List<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        return serviceBindingsOps.unbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        processesOps.updateApplicationStaging(applicationName, staging);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> routes) {
        routesOps.updateApplicationRoutes(applicationName, routes);
    }

    @Override
    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        return serviceBrokersOps.updateServiceBroker(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        serviceBrokersOps.updateServicePlanVisibilityForBroker(name, visibility);
    }

    @Override
    public void updateServicePlan(String serviceName, String planName) {
        serviceInstancesOps.updateServicePlan(serviceName, planName);
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        serviceInstancesOps.updateServiceParameters(serviceName, parameters);
    }

    @Override
    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        serviceInstancesOps.updateServiceSyslogDrainUrl(serviceName, syslogDrainUrl);
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        serviceInstancesOps.updateServiceTags(serviceName, tags);
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback, Duration uploadTimeout) {
        return packagesOps.asyncUploadApplication(applicationName, file, callback, uploadTimeout);
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return packagesOps.getUploadStatus(packageGuid);
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return tasksOps.getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return tasksOps.getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return tasksOps.runTask(applicationName, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return tasksOps.cancelTask(taskGuid);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return buildsOps.createBuild(packageGuid);
    }

    @Override
    public CloudBuild getBuild(UUID packageGuid) {
        return buildsOps.getBuild(packageGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return buildsOps.getBuildsForApplication(applicationGuid);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return serviceInstancesOps.getServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        return serviceInstancesOps.getUserProvidedServiceInstanceParameters(guid);
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return serviceBindingsOps.getServiceBindingParameters(guid);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return buildsOps.getBuildsForPackage(packageGuid);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        return serviceInstancesOps.getServiceInstancesWithoutAuxiliaryContentByNames(names);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        return serviceInstancesOps.getServiceInstancesByMetadataLabelSelector(labelSelector);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        return serviceInstancesOps.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(labelSelector);
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        cc.getRestClient()
          .patch()
          .uri("/v3/apps/{guid}", guid)
          .body(Map.of("metadata", Map.of("labels", metadata.getLabels(), "annotations", metadata.getAnnotations())))
          .retrieve()
          .toBodilessEntity();
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        serviceInstancesOps.updateServiceInstanceMetadata(guid, metadata);
    }

    @Override
    public void updateServiceBindingMetadata(UUID guid, Metadata metadata) {
        serviceBindingsOps.updateServiceBindingMetadata(guid, metadata);
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        return processesOps.getCurrentDropletForApplication(applicationGuid);
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return packagesOps.getPackage(packageGuid);
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return packagesOps.getPackagesForApplication(applicationGuid);
    }

    @Override
    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        return rolesOps.getUserRolesBySpaceAndUser(spaceGuid, userGuid);
    }

    @Override
    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        return packagesOps.createDockerPackage(applicationGuid, dockerInfo);
    }

    @Override
    public CloudAsyncJob getAsyncJob(String jobId) {
        return jobsOps.getAsyncJob(jobId);
    }

}
