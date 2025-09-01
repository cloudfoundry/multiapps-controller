package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.DockerData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.Resource;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.client.v3.applications.ApplicationFeature;
import org.cloudfoundry.client.v3.applications.ApplicationRelationships;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.cloudfoundry.client.v3.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v3.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v3.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationCurrentDropletResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationEnvironmentVariablesRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationEnvironmentVariablesResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationSshEnabledRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationSshEnabledResponse;
import org.cloudfoundry.client.v3.applications.ListApplicationBuildsRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationFeaturesRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationPackagesRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v3.applications.ScaleApplicationRequest;
import org.cloudfoundry.client.v3.applications.SetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.applications.StartApplicationRequest;
import org.cloudfoundry.client.v3.applications.StopApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationEnvironmentVariablesRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationFeatureRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationResponse;
import org.cloudfoundry.client.v3.auditevents.AuditEventResource;
import org.cloudfoundry.client.v3.auditevents.ListAuditEventsRequest;
import org.cloudfoundry.client.v3.builds.Build;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.builds.ListBuildsRequest;
import org.cloudfoundry.client.v3.domains.CreateDomainRequest;
import org.cloudfoundry.client.v3.domains.DeleteDomainRequest;
import org.cloudfoundry.client.v3.domains.Domain;
import org.cloudfoundry.client.v3.domains.DomainRelationships;
import org.cloudfoundry.client.v3.domains.DomainResource;
import org.cloudfoundry.client.v3.domains.ListDomainsRequest;
import org.cloudfoundry.client.v3.jobs.GetJobRequest;
import org.cloudfoundry.client.v3.organizations.GetOrganizationDefaultDomainRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationDomainsRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.Package;
import org.cloudfoundry.client.v3.packages.PackageRelationships;
import org.cloudfoundry.client.v3.packages.PackageResource;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheck;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheckType;
import org.cloudfoundry.client.v3.processes.UpdateProcessRequest;
import org.cloudfoundry.client.v3.roles.ListRolesRequest;
import org.cloudfoundry.client.v3.roles.RoleResource;
import org.cloudfoundry.client.v3.roles.RoleType;
import org.cloudfoundry.client.v3.routes.CreateRouteRequest;
import org.cloudfoundry.client.v3.routes.CreateRouteResponse;
import org.cloudfoundry.client.v3.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v3.routes.Destination;
import org.cloudfoundry.client.v3.routes.InsertRouteDestinationsRequest;
import org.cloudfoundry.client.v3.routes.ListRoutesRequest;
import org.cloudfoundry.client.v3.routes.RemoveRouteDestinationsRequest;
import org.cloudfoundry.client.v3.routes.RouteRelationships;
import org.cloudfoundry.client.v3.routes.RouteResource;
import org.cloudfoundry.client.v3.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v3.servicebindings.CreateServiceBindingResponse;
import org.cloudfoundry.client.v3.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v3.servicebindings.GetServiceBindingDetailsRequest;
import org.cloudfoundry.client.v3.servicebindings.GetServiceBindingDetailsResponse;
import org.cloudfoundry.client.v3.servicebindings.GetServiceBindingParametersRequest;
import org.cloudfoundry.client.v3.servicebindings.GetServiceBindingParametersResponse;
import org.cloudfoundry.client.v3.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingRelationships;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingType;
import org.cloudfoundry.client.v3.servicebindings.UpdateServiceBindingRequest;
import org.cloudfoundry.client.v3.servicebrokers.BasicAuthentication;
import org.cloudfoundry.client.v3.servicebrokers.CreateServiceBrokerRequest;
import org.cloudfoundry.client.v3.servicebrokers.DeleteServiceBrokerRequest;
import org.cloudfoundry.client.v3.servicebrokers.ListServiceBrokersRequest;
import org.cloudfoundry.client.v3.servicebrokers.ServiceBrokerRelationships;
import org.cloudfoundry.client.v3.servicebrokers.ServiceBrokerResource;
import org.cloudfoundry.client.v3.servicebrokers.UpdateServiceBrokerRequest;
import org.cloudfoundry.client.v3.serviceinstances.CreateServiceInstanceRequest;
import org.cloudfoundry.client.v3.serviceinstances.DeleteServiceInstanceRequest;
import org.cloudfoundry.client.v3.serviceinstances.GetManagedServiceParametersRequest;
import org.cloudfoundry.client.v3.serviceinstances.GetManagedServiceParametersResponse;
import org.cloudfoundry.client.v3.serviceinstances.GetServiceInstanceRequest;
import org.cloudfoundry.client.v3.serviceinstances.GetUserProvidedCredentialsRequest;
import org.cloudfoundry.client.v3.serviceinstances.GetUserProvidedCredentialsResponse;
import org.cloudfoundry.client.v3.serviceinstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstance;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceRelationships;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.client.v3.serviceinstances.UpdateServiceInstanceRequest;
import org.cloudfoundry.client.v3.serviceofferings.GetServiceOfferingRequest;
import org.cloudfoundry.client.v3.serviceofferings.ListServiceOfferingsRequest;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOffering;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingResource;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v3.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v3.serviceplans.ServicePlan;
import org.cloudfoundry.client.v3.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v3.serviceplans.UpdateServicePlanVisibilityRequest;
import org.cloudfoundry.client.v3.serviceplans.Visibility;
import org.cloudfoundry.client.v3.spaces.DeleteUnmappedRoutesRequest;
import org.cloudfoundry.client.v3.stacks.ListStacksRequest;
import org.cloudfoundry.client.v3.stacks.Stack;
import org.cloudfoundry.client.v3.tasks.CancelTaskRequest;
import org.cloudfoundry.client.v3.tasks.CreateTaskRequest;
import org.cloudfoundry.client.v3.tasks.GetTaskRequest;
import org.cloudfoundry.client.v3.tasks.ListTasksRequest;
import org.cloudfoundry.client.v3.tasks.Task;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.Constants;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.UploadStatusCallback;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawCloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawUserRole;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableRawV3CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.BitsData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
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
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Derivable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ErrorDetails;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableErrorDetails;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableUpload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.RouteDestination;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServicePlanVisibility;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Upload;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.util.JobV3Util;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Abstract implementation of the CloudControllerClient intended to serve as the base.
 */
public class CloudControllerRestClientImpl implements CloudControllerRestClient {

    private static final long PACKAGE_UPLOAD_JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);
    private static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration BINDING_OPERATIONS_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST = 4000;

    private CloudSpace target; // optional, as some operations do not require a targeted space
    private CloudFoundryClient delegate;

    /**
     * Only for unit tests. This works around the fact that the initialize method is called within the constructor and hence can not be
     * overloaded, making it impossible to write unit tests that don't trigger network calls.
     */
    protected CloudControllerRestClientImpl() {
    }

    public CloudControllerRestClientImpl(CloudFoundryClient delegate) {
        this(delegate, null);
    }

    public CloudControllerRestClientImpl(CloudFoundryClient delegate, CloudSpace target) {
        this.target = target;
        this.delegate = delegate;
    }

    @Override
    public CloudSpace getTarget() {
        return target;
    }

    @Override
    public void addDomain(String domainName) {
        assertSpaceProvided("add domain");
        CloudDomain domain = findDomainByName(domainName);
        if (domain == null) {
            doCreateDomain(domainName);
        }
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);
        doAddRoute(domainGuid, host, path);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName) {
        return bindServiceInstance(bindingName, applicationName, serviceInstanceName, null);
    }

    @Override
    public Optional<String> bindServiceInstance(String bindingName, String applicationName, String serviceInstanceName,
                                                Map<String, Object> parameters) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);

        var createBindingRequest = CreateServiceBindingRequest.builder()
                                                              .name(bindingName)
                                                              .type(ServiceBindingType.APPLICATION)
                                                              .relationships(ServiceBindingRelationships.builder()
                                                                                                        .application(buildToOneRelationship(
                                                                                                            applicationGuid))
                                                                                                        .serviceInstance(
                                                                                                            buildToOneRelationship(
                                                                                                                serviceInstanceGuid))
                                                                                                        .build());
        if (!CollectionUtils.isEmpty(parameters)) {
            createBindingRequest.parameters(parameters);
        }
        return delegate.serviceBindingsV3()
                       .create(createBindingRequest.build())
                       .map(CreateServiceBindingResponse::getJobId)
                       .block();
    }

    @Override
    public void createApplication(ApplicationToCreateDto applicationToCreateDto) {
        assertSpaceProvided("create application");
        CreateApplicationRequest createApplicationRequest = CreateApplicationRequest.builder()
                                                                                    .name(applicationToCreateDto.getName())
                                                                                    .metadata(applicationToCreateDto.getMetadata())
                                                                                    .lifecycle(buildApplicationLifecycle(
                                                                                        applicationToCreateDto.getStaging()))
                                                                                    .relationships(buildApplicationRelationships())
                                                                                    .environmentVariables(applicationToCreateDto.getEnv())
                                                                                    .build();
        CreateApplicationResponse createApplicationResponse = delegate.applicationsV3()
                                                                      .create(createApplicationRequest)
                                                                      .block();
        updateApplicationAttributes(applicationToCreateDto, createApplicationResponse.getId());
    }

    private Lifecycle buildApplicationLifecycle(Staging staging) {
        // Prioritize Docker lifecycle if DockerInfo is provided
        if (staging.getDockerInfo() != null) {
            return createDockerLifecycle();
        }

        // Determine lifecycle type, defaulting to BUILDPACK if lifecycleType is null
        LifecycleType lifecycleType = staging.getLifecycleType() != null ? LifecycleType.valueOf(staging.getLifecycleType()
                                                                                                        .name()) : LifecycleType.BUILDPACK;

        return createLifecycleByType(staging, lifecycleType);
    }

    private Lifecycle createLifecycleByType(Staging staging, LifecycleType lifecycleType) {
        validateLifecycleConfiguration(staging, lifecycleType);
        BuildpackData buildpackData = createBuildpackData(staging);
        return Lifecycle.builder()
                        .type(lifecycleType)
                        .data(buildpackData)
                        .build();
    }

    private Lifecycle createDockerLifecycle() {
        return Lifecycle.builder()
                        .type(LifecycleType.DOCKER)
                        .data(DockerData.builder()
                                        .build())
                        .build();
    }

    private void validateLifecycleConfiguration(Staging staging, LifecycleType lifecycleType) {
        if (lifecycleType == LifecycleType.CNB && CollectionUtils.isEmpty(staging.getBuildpacks())) {
            throw new IllegalArgumentException(Messages.BUILDPACKS_ARE_REQUIRED_FOR_CNB_LIFECYCLE_TYPE);
        }
    }

    private BuildpackData createBuildpackData(Staging staging) {
        BuildpackData.Builder buildpackDataBuilder = BuildpackData.builder()
                                                                  .stack(staging.getStackName());
        if (staging.getBuildpacks() != null) {
            buildpackDataBuilder.addAllBuildpacks(staging.getBuildpacks());
        }
        return buildpackDataBuilder.build();
    }

    private ApplicationRelationships buildApplicationRelationships() {
        return ApplicationRelationships.builder()
                                       .space(buildToOneRelationship(getTargetSpaceGuid()))
                                       .build();
    }

    private void updateApplicationAttributes(ApplicationToCreateDto applicationToCreateDto, String applicationGuid) {
        UUID createdApplicationGuid = UUID.fromString(applicationGuid);
        updateApplicationProcess(createdApplicationGuid, applicationToCreateDto.getStaging());
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(createdApplicationGuid.toString())
                                              .type("web")
                                              .memoryInMb(applicationToCreateDto.getMemoryInMb())
                                              .diskInMb(applicationToCreateDto.getDiskQuotaInMb())
                                              .build())
                .block();
        if (!CollectionUtils.isEmpty(applicationToCreateDto.getRoutes())) {
            addRoutes(applicationToCreateDto.getRoutes(), createdApplicationGuid);
        }
    }

    private void updateApplicationProcess(UUID applicationGuid, Staging staging) {
        staging.getAppFeatures()
               .entrySet()
               .forEach(entry -> updateAppFeature(applicationGuid, entry.getKey(), entry.getValue()));
        GetApplicationProcessResponse getApplicationProcessResponse = getApplicationProcessResource(applicationGuid);
        UpdateProcessRequest.Builder updateProcessRequestBuilder = UpdateProcessRequest.builder()
                                                                                       .processId(getApplicationProcessResponse.getId())
                                                                                       .command(staging.getCommand());
        if (staging.getHealthCheckType() != null) {
            updateProcessRequestBuilder.healthCheck(buildHealthCheck(staging));
        }
        if (staging.isReadinessHealthCheckEnabled() != null && staging.isReadinessHealthCheckEnabled()
            && staging.getReadinessHealthCheckType() != null) {
            updateProcessRequestBuilder.readinessHealthCheck(buildReadinessHealthCheck(staging));
        }
        delegate.processes()
                .update(updateProcessRequestBuilder.build())
                .block();
    }

    private ReadinessHealthCheck buildReadinessHealthCheck(Staging staging) {
        return ReadinessHealthCheck.builder()
                                   .type(ReadinessHealthCheckType.from(staging.getReadinessHealthCheckType()))
                                   .data(buildReadinessHealthCheckData(staging))
                                   .build();
    }

    private Data buildReadinessHealthCheckData(Staging staging) {
        return Data.builder()
                   .invocationTimeout(staging.getReadinessHealthCheckInvocationTimeout())
                   .endpoint(staging.getReadinessHealthCheckHttpEndpoint())
                   .interval(staging.getReadinessHealthCheckInterval())
                   .build();
    }

    private void updateAppFeature(UUID applicationGuid, String featureName, boolean enabled) {
        delegate.applicationsV3()
                .updateFeature(UpdateApplicationFeatureRequest.builder()
                                                              .featureName(featureName)
                                                              .enabled(enabled)
                                                              .applicationId(applicationGuid.toString())
                                                              .build())
                .block();
    }

    private GetApplicationProcessResponse getApplicationProcessResource(UUID applicationGuid) {
        return delegate.applicationsV3()
                       .getProcess(GetApplicationProcessRequest.builder()
                                                               .type("web")
                                                               .applicationId(applicationGuid.toString())
                                                               .build())
                       .block();
    }

    private HealthCheck buildHealthCheck(Staging staging) {
        HealthCheckType healthCheckType = HealthCheckType.from(staging.getHealthCheckType());
        return HealthCheck.builder()
                          .type(healthCheckType)
                          .data(Data.builder()
                                    .endpoint(staging.getHealthCheckHttpEndpoint())
                                    .timeout(staging.getHealthCheckTimeout())
                                    .invocationTimeout(staging.getInvocationTimeout())
                                    .build())
                          .build();
    }

    private void addRoutes(Set<CloudRoute> routes, UUID applicationGuid) {
        Map<String, UUID> domains = getDomainsFromRoutes(routes);
        for (CloudRoute route : routes) {
            validateDomainForRoute(route, domains);
            UUID domainGuid = domains.get(route.getDomain()
                                               .getName());
            UUID routeGuid = getOrAddRoute(domainGuid, route.getHost(), route.getPath());
            bindRoute(routeGuid, applicationGuid, route.getRequestedProtocol());
        }
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");
        CloudServicePlan servicePlan = findPlanForService(serviceInstance, serviceInstance.getPlan());
        UUID servicePlanGuid = servicePlan.getGuid();

        delegate.serviceInstancesV3()
                .create(CreateServiceInstanceRequest.builder()
                                                    .type(ServiceInstanceType.MANAGED)
                                                    .name(serviceInstance.getName())
                                                    .metadata(serviceInstance.getV3Metadata())
                                                    .relationships(ServiceInstanceRelationships.builder()
                                                                                               .servicePlan(buildToOneRelationship(
                                                                                                   servicePlanGuid.toString()))
                                                                                               .space(buildToOneRelationship(
                                                                                                   getTargetSpaceGuid().toString()))
                                                                                               .build())
                                                    .tags(serviceInstance.getTags())
                                                    .parameters(serviceInstance.getCredentials())
                                                    .build())
                .block();
    }

    @Override
    public String createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        ServiceBrokerRelationships serviceBrokerRelationship = Optional.ofNullable(serviceBroker.getSpaceGuid())
                                                                       .map(UUID::fromString)
                                                                       .map(this::buildToOneRelationship)
                                                                       .map(spaceRelationship -> ServiceBrokerRelationships.builder()
                                                                                                                           .space(
                                                                                                                               spaceRelationship)
                                                                                                                           .build())
                                                                       .orElse(null);

        return delegate.serviceBrokersV3()
                       .create(CreateServiceBrokerRequest.builder()
                                                         .name(serviceBroker.getName())
                                                         .url(serviceBroker.getUrl())
                                                         .authentication(BasicAuthentication.builder()
                                                                                            .username(serviceBroker.getUsername())
                                                                                            .password(serviceBroker.getPassword())
                                                                                            .build())
                                                         .relationships(serviceBrokerRelationship)
                                                         .build())
                       .block();
    }

    @Override
    public CloudServiceKey createAndFetchServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        doCreateServiceKeySync(keyModel.getName(), keyModel.getCredentials(), keyModel.getV3Metadata(), serviceInstance);

        return fetchWithAuxiliaryContent(
            () -> getServiceKeyResourceByNameAndServiceInstanceGuid(keyModel.getName(), getGuid(serviceInstance)),
            fetchedKey -> zipWithAuxiliaryServiceKeyContent(fetchedKey, serviceInstance));
    }

    @Override
    public Optional<String> createServiceKey(CloudServiceKey keyModel, String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return doCreateServiceKey(keyModel.getName(), keyModel.getCredentials(), keyModel.getV3Metadata(), serviceInstance);
    }

    @Override
    public Optional<String> createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return doCreateServiceKey(serviceKeyName, parameters, null, serviceInstance);
    }

    private void doCreateServiceKeySync(String name, Map<String, Object> parameters, Metadata metadata,
                                        CloudServiceInstance serviceInstance) {
        CreateServiceBindingRequest createServiceBindingRequest = buildServiceCredentialBindingRequest(name, parameters, metadata,
                                                                                                       serviceInstance);
        delegate.serviceBindingsV3()
                .create(createServiceBindingRequest)
                .map(response -> response.getJobId()
                                         .get())
                .flatMap(jobId -> JobV3Util.waitForCompletion(delegate, BINDING_OPERATIONS_TIMEOUT, jobId))
                .block();
    }

    private Optional<String> doCreateServiceKey(String name, Map<String, Object> parameters, Metadata metadata,
                                                CloudServiceInstance serviceInstance) {
        CreateServiceBindingRequest createServiceBindingRequest = buildServiceCredentialBindingRequest(name, parameters, metadata,
                                                                                                       serviceInstance);
        return delegate.serviceBindingsV3()
                       .create(createServiceBindingRequest)
                       .map(CreateServiceBindingResponse::getJobId)
                       .block();
    }

    private CreateServiceBindingRequest buildServiceCredentialBindingRequest(String name, Map<String, Object> parameters, Metadata metadata,
                                                                             CloudServiceInstance serviceInstance) {
        if (serviceInstance.getType() != ServiceInstanceType.MANAGED) {
            throw new IllegalArgumentException(
                String.format(Messages.CANT_CREATE_SERVICE_KEY_FOR_USER_PROVIDED_SERVICE, serviceInstance.getName()));
        }
        UUID serviceInstanceGuid = getGuid(serviceInstance);

        var createBindingRequest = CreateServiceBindingRequest.builder()
                                                              .type(ServiceBindingType.KEY)
                                                              .name(name)
                                                              .metadata(metadata)
                                                              .relationships(ServiceBindingRelationships.builder()
                                                                                                        .serviceInstance(
                                                                                                            buildToOneRelationship(
                                                                                                                serviceInstanceGuid))
                                                                                                        .build());
        if (!CollectionUtils.isEmpty(parameters)) {
            createBindingRequest.parameters(parameters);
        }
        return createBindingRequest.build();
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");
        String syslogDrainUrl = StringUtils.hasText(serviceInstance.getSyslogDrainUrl()) ? serviceInstance.getSyslogDrainUrl() : "";
        delegate.serviceInstancesV3()
                .create(CreateServiceInstanceRequest.builder()
                                                    .name(serviceInstance.getName())
                                                    .metadata(serviceInstance.getV3Metadata())
                                                    .type(ServiceInstanceType.USER_PROVIDED)
                                                    .credentials(serviceInstance.getCredentials())
                                                    .syslogDrainUrl(syslogDrainUrl)
                                                    .tags(serviceInstance.getTags())
                                                    .relationships(ServiceInstanceRelationships.builder()
                                                                                               .space(buildToOneRelationship(
                                                                                                   getTargetSpaceGuid().toString()))
                                                                                               .build())
                                                    .build())
                .block();
    }

    @Override
    public void deleteApplication(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        deleteApplication(applicationGuid);
    }

    private void deleteApplication(UUID applicationGuid) {
        delegate.applicationsV3()
                .delete(DeleteApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .build())
                .flatMap(jobId -> JobV3Util.waitForCompletion(delegate, DELETE_JOB_TIMEOUT, jobId))
                .block();
    }

    @Override
    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        CloudDomain domain = findDomainByName(domainName, true);
        List<CloudRoute> routes = findRoutes(domain);
        if (!routes.isEmpty()) {
            throw new IllegalStateException("Unable to remove domain that is in use --" + " it has " + routes.size() + " routes.");
        }
        doDeleteDomain(getGuid(domain));
    }

    /**
     * Delete routes that do not have any application which is assigned to them.
     */
    @Override
    public void deleteOrphanedRoutes() {
        delegate.spacesV3()
                .deleteUnmappedRoutes(DeleteUnmappedRoutesRequest.builder()
                                                                 .spaceId(getTargetSpaceGuid().toString())
                                                                 .build())
                .flatMap(jobId -> JobV3Util.waitForCompletion(delegate, DELETE_JOB_TIMEOUT, jobId))
                .block();
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        assertSpaceProvided("delete route for domain");
        UUID routeGuid = getRouteGuid(getRequiredDomainGuid(domainName), host, path);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                              "Host " + host + " not found for domain " + domainName + ".");
        }
        doDeleteRoute(routeGuid);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName);
        doDeleteServiceInstance(serviceInstance.getGuid());
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        doDeleteServiceInstance(serviceInstance.getGuid());
    }

    @Override
    public String deleteServiceBroker(String name) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID guid = broker.getMetadata()
                          .getGuid();
        return delegate.serviceBrokersV3()
                       .delete(DeleteServiceBrokerRequest.builder()
                                                         .serviceBrokerId(guid.toString())
                                                         .build())
                       .block();
    }

    @Override
    public Optional<String> deleteServiceBinding(String serviceInstanceName, String serviceKeyName) {
        CloudServiceKey serviceKey = getServiceKey(serviceInstanceName, serviceKeyName);
        if (serviceKey == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service key " + serviceKeyName + " not found.");
        }
        return doDeleteServiceBinding(serviceKey.getGuid());
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return findApplicationByName(applicationName, required);
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        return getRequiredApplicationGuid(applicationName);
    }

    @Override
    public String getApplicationName(UUID applicationGuid) {
        // This will throw a CloudOperationException with a 404 if no app with given GUID is present
        return getApplicationByGuid(applicationGuid).block()
                                                    .getName();
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        return delegate.applicationsV3()
                       .getEnvironmentVariables(GetApplicationEnvironmentVariablesRequest.builder()
                                                                                         .applicationId(applicationGuid.toString())
                                                                                         .build())
                       .map(GetApplicationEnvironmentVariablesResponse::getVars)
                       .block();
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getApplicationEnvironment(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getEventsByTarget(applicationGuid);
    }

    @Override
    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        return findEventsByTarget(uuid.toString());
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication application) {
        if (application.getState()
                       .equals(CloudApplication.State.STARTED)) {
            return findApplicationInstances(getGuid(application));
        }
        return ImmutableInstancesInfo.builder()
                                     .instances(Collections.emptyList())
                                     .build();
    }

    @Override
    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        return findApplicationInstances(applicationGuid);
    }

    @Override
    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        return fetch(() -> Mono.just(getApplicationProcessResource(applicationGuid)), ImmutableRawCloudProcess::of);
    }

    @Override
    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        return fetchList(() -> getRouteResourcesByAppGuid(applicationGuid), routeResource -> ImmutableRawCloudRoute.builder()
                                                                                                                   .route(routeResource)
                                                                                                                   .applicationGuid(
                                                                                                                       applicationGuid)
                                                                                                                   .build());
    }

    @Override
    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        return delegate.applicationsV3()
                       .getSshEnabled(GetApplicationSshEnabledRequest.builder()
                                                                     .applicationId(applicationGuid.toString())
                                                                     .build())
                       .map(GetApplicationSshEnabledResponse::getEnabled)
                       .defaultIfEmpty(false)
                       .block();
    }

    @Override
    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        IntFunction<ListApplicationFeaturesRequest> pageRequestSupplier = page -> ListApplicationFeaturesRequest.builder()
                                                                                                                .applicationId(
                                                                                                                    applicationGuid.toString())
                                                                                                                .page(page)
                                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .listFeatures(pageRequestSupplier.apply(page)))
                              .collect(Collectors.toMap(ApplicationFeature::getName, ApplicationFeature::getEnabled))
                              .block();
    }

    @Override
    public List<CloudApplication> getApplications() {
        return fetchList(this::getApplicationResources, application -> ImmutableRawCloudApplication.builder()
                                                                                                   .application(application)
                                                                                                   .space(target)
                                                                                                   .build());
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        assertSpaceProvided("get applications");
        return fetchList(() -> getApplicationsByLabelSelector(labelSelector), application -> ImmutableRawCloudApplication.builder()
                                                                                                                         .application(
                                                                                                                             application)
                                                                                                                         .space(target)
                                                                                                                         .build());
    }

    private Flux<? extends Application> getApplicationsByLabelSelector(String labelSelector) {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .labelSelector(labelSelector)
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return fetch(() -> getDefaultDomainResource(getTargetOrganizationGuid().toString()), ImmutableRawCloudDomain::of);
    }

    private Mono<? extends Domain> getDefaultDomainResource(String guid) {
        return delegate.organizationsV3()
                       .getDefaultDomain(GetOrganizationDefaultDomainRequest.builder()
                                                                            .organizationId(guid)
                                                                            .build());
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return fetchList(this::getSharedDomainResources, ImmutableRawCloudDomain::of);
    }

    @Override
    public List<CloudDomain> getDomains() {
        return fetchList(this::getDomainResources, ImmutableRawCloudDomain::of);
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        return findDomainsByOrganizationGuid(getTargetOrganizationGuid());
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return fetchList(this::getPrivateDomainResources, ImmutableRawCloudDomain::of);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return fetchList(this::getEventResources, ImmutableRawCloudEvent::of);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        assertSpaceProvided("get routes for domain");
        CloudDomain domain = findDomainByName(domainName, true);
        return findRoutes(domain);
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        Resource serviceInstanceResource = getServiceInstanceByName(name).block();
        if (serviceInstanceResource == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + name + " not found.");
        }
        return UUID.fromString(serviceInstanceResource.getId());
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return getServiceInstance(serviceInstanceName, true);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        CloudServiceInstance serviceInstance = findServiceInstanceByName(serviceInstanceName);
        return getServiceInstanceIfRequired(serviceInstanceName, serviceInstance, required);
    }

    private CloudServiceInstance getServiceInstanceIfRequired(String serviceInstanceName, CloudServiceInstance serviceInstance,
                                                              boolean required) {
        if (serviceInstance == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + serviceInstanceName + " not found.");
        }
        return serviceInstance;
    }

    @Override
    public CloudServiceBinding getServiceBinding(UUID serviceBindingGuid) {
        // TODO: Refactor and use serviceBindingsV3.get() when https://github.com/cloudfoundry/cf-java-client/pull/1171 is merged and
        // released
        return fetch(() -> getServiceBindingResource(serviceBindingGuid), ImmutableRawCloudServiceBinding::of);
    }

    private Mono<ServiceBindingResource> getServiceBindingResource(UUID serviceBindingGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .page(page)
                                                                                                        .id(serviceBindingGuid.toString())
                                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceBindingsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    @Override
    public List<CloudServiceBinding> getServiceAppBindings(UUID serviceInstanceGuid) {
        return fetchList(() -> getServiceBindingResourcesByServiceInstanceGuid(serviceInstanceGuid), ImmutableRawCloudServiceBinding::of);
    }

    @Override
    public List<CloudServiceBinding> getAppBindings(UUID applicationGuid) {
        return fetchList(() -> getServiceBindingResourcesByApplicationGuid(applicationGuid), ImmutableRawCloudServiceBinding::of);
    }

    @Override
    public CloudServiceBinding getServiceBindingForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        return fetch(() -> getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(applicationId, serviceInstanceGuid),
                     ImmutableRawCloudServiceBinding::of);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return getServiceBroker(name, true);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        CloudServiceBroker serviceBroker = findServiceBrokerByName(name);
        if (serviceBroker == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service broker " + name + " not found.");
        }
        return serviceBroker;
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return fetchList(this::getServiceBrokerResources, ImmutableRawCloudServiceBroker::of);
    }

    @Override
    public CloudServiceKey getServiceKey(String serviceInstanceName, String serviceKeyName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return fetchWithAuxiliaryContent(() -> getServiceKeyResourceByNameAndServiceInstanceGuid(serviceKeyName, serviceInstance.getGuid()),
                                         serviceKey -> zipWithAuxiliaryServiceKeyContent(serviceKey, serviceInstance));
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return getServiceKeysWithCredentials(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return fetchList(() -> getServiceKeyResource(serviceInstance), serviceKey -> ImmutableRawCloudServiceKey.builder()
                                                                                                                .serviceInstance(
                                                                                                                    serviceInstance)
                                                                                                                .serviceBindingResource(
                                                                                                                    serviceKey)
                                                                                                                .build());
    }

    @Override
    public List<CloudServiceKey> getServiceKeysWithCredentials(CloudServiceInstance serviceInstance) {
        return fetchListWithAuxiliaryContent(() -> getServiceKeyResource(serviceInstance),
                                             serviceKey -> zipWithAuxiliaryServiceKeyContent(serviceKey, serviceInstance));
    }

    private Mono<Derivable<CloudServiceKey>> zipWithAuxiliaryServiceKeyContent(ServiceBindingResource key,
                                                                               CloudServiceInstance serviceInstance) {
        return getServiceKeyCredentials(key.getId()).map(credentials -> ImmutableRawCloudServiceKey.builder()
                                                                                                   .serviceBindingResource(key)
                                                                                                   .credentials(credentials)
                                                                                                   .serviceInstance(serviceInstance)
                                                                                                   .build());
    }

    private Mono<Map<String, Object>> getServiceKeyCredentials(String keyGuid) {
        return delegate.serviceBindingsV3()
                       .getDetails(GetServiceBindingDetailsRequest.builder()
                                                                  .serviceBindingId(keyGuid)
                                                                  .build())
                       // CF V3 API returns 404 when fetching credentials of a service key which creation failed
                       .onErrorResume(t -> doesErrorMatchStatusCode(t, HttpStatus.NOT_FOUND), t -> Mono.just(
                           GetServiceBindingDetailsResponse.builder()
                                                           .volumeMounts(Collections.emptyList())
                                                           .credentials(Collections.emptyMap())
                                                           .build()))
                       .map(GetServiceBindingDetailsResponse::getCredentials);
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return delegate.serviceInstancesV3()
                       .getManagedServiceParameters(GetManagedServiceParametersRequest.builder()
                                                                                      .serviceInstanceId(guid.toString())
                                                                                      .build())
                       .map(GetManagedServiceParametersResponse::getParameters)
                       .block();
    }

    @Override
    public Map<String, Object> getUserProvidedServiceInstanceParameters(UUID guid) {
        return delegate.serviceInstancesV3()
                       .getUserProvidedCredentials(GetUserProvidedCredentialsRequest.builder()
                                                                                    .serviceInstanceId(guid.toString())
                                                                                    .build())
                       .map(GetUserProvidedCredentialsResponse::getCredentials)
                       .block();
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return delegate.serviceBindingsV3()
                       .getParameters(GetServiceBindingParametersRequest.builder()
                                                                        .serviceBindingId(guid.toString())
                                                                        .build())
                       .map(GetServiceBindingParametersResponse::getParameters)
                       .block();
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return fetchListWithAuxiliaryContent(this::getServiceResources, this::zipWithAuxiliaryServiceOfferingContent);
    }

    @Override
    public void updateServicePlan(String serviceName, String planName) {
        CloudServiceInstance service = getServiceInstance(serviceName);
        if (service.isUserProvided()) {
            return;
        }
        CloudServicePlan plan = findPlanForService(service, planName);
        delegate.serviceInstancesV3()
                .update(UpdateServiceInstanceRequest.builder()
                                                    .serviceInstanceId(service.getGuid()
                                                                              .toString())
                                                    .relationships(ServiceInstanceRelationships.builder()
                                                                                               .servicePlan(
                                                                                                   buildToOneRelationship(plan.getGuid()))
                                                                                               .build())
                                                    .build())
                .block();
    }

    @Override
    public String getServiceInstanceName(UUID serviceInstanceGuid) {
        return getServiceInstanceByGuid(serviceInstanceGuid).block()
                                                            .getName();
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName) {
        return getServiceInstanceWithoutAuxiliaryContent(serviceInstanceName, true);
    }

    @Override
    public CloudServiceInstance getServiceInstanceWithoutAuxiliaryContent(String serviceInstanceName, boolean required) {
        CloudServiceInstance serviceInstance = fetch(() -> getServiceInstanceResourceByName(serviceInstanceName),
                                                     ImmutableRawCloudServiceInstance::of);
        return getServiceInstanceIfRequired(serviceInstanceName, serviceInstance, required);
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        CloudServiceInstance service = getServiceInstanceWithoutAuxiliaryContent(serviceName);
        var updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
                                                                       .serviceInstanceId(service.getGuid()
                                                                                                 .toString());
        if (service.isUserProvided()) {
            updateServiceInstanceRequest.credentials(parameters);
        } else {
            updateServiceInstanceRequest.parameters(parameters);
        }
        delegate.serviceInstancesV3()
                .update(updateServiceInstanceRequest.build())
                .block();
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceName);
        delegate.serviceInstancesV3()
                .update(UpdateServiceInstanceRequest.builder()
                                                    .serviceInstanceId(serviceInstanceGuid.toString())
                                                    .tags(tags)
                                                    .build())
                .block();
    }

    @Override
    public void updateServiceSyslogDrainUrl(String serviceName, String syslogDrainUrl) {
        CloudServiceInstance service = getServiceInstanceWithoutAuxiliaryContent(serviceName);
        if (!service.isUserProvided()) {
            return;
        }
        String updatedSyslogDrain = StringUtils.hasText(syslogDrainUrl) ? syslogDrainUrl : "";
        delegate.serviceInstancesV3()
                .update(UpdateServiceInstanceRequest.builder()
                                                    .serviceInstanceId(service.getGuid()
                                                                              .toString())
                                                    .syslogDrainUrl(updatedSyslogDrain)
                                                    .build())
                .block();
    }

    private <T> List<List<T>> toBatches(Collection<T> largeList, int maxCharLength) {
        if (largeList.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> batches = new ArrayList<>();
        int currentBatchLength = 0, currentBatchIndex = 0;
        batches.add(new ArrayList<>());

        for (T element : largeList) {
            int elementLength = element.toString()
                                       .length();
            if (elementLength + currentBatchLength >= maxCharLength) {
                batches.add(new ArrayList<>());
                currentBatchIndex++;
                currentBatchLength = 0;
            }
            batches.get(currentBatchIndex)
                   .add(element);
            currentBatchLength += elementLength;
        }
        return batches;
    }

    private Flux<ServiceInstanceResource> getServiceInstancesByNames(List<String> serviceInstanceNames) {
        String spaceGuid = getTargetSpaceGuid().toString();
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .spaceId(spaceGuid)
                                                                                                          .addAllServiceInstanceNames(
                                                                                                              serviceInstanceNames)
                                                                                                          .page(page)
                                                                                                          .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceInstancesV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByNames(List<String> names) {
        List<CloudServiceInstance> allServiceInstances = new ArrayList<>();
        for (List<String> batchOfServiceInstanceNames : toBatches(names, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST)) {
            List<CloudServiceInstance> serviceInstances = fetchList(() -> getServiceInstancesByNames(batchOfServiceInstanceNames),
                                                                    ImmutableRawV3CloudServiceInstance::of);
            allServiceInstances.addAll(serviceInstances);
        }
        return allServiceInstances;
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .labelSelector(labelSelector)
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .page(page)
                                                                                                          .build();

        return fetchListWithAuxiliaryContent(() -> getServiceInstanceResources(pageRequestSupplier),
                                             this::zipWithAuxiliaryServiceInstanceContent);
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .labelSelector(labelSelector)
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .page(page)
                                                                                                          .build();

        return fetchList(() -> getServiceInstanceResources(pageRequestSupplier), ImmutableRawCloudServiceInstance::of);
    }

    @Override
    public CloudStack getStack(String name) {
        return getStack(name, true);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        CloudStack stack = findStackResource(name);
        if (stack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Stack " + name + " not found.");
        }
        return stack;
    }

    @Override
    public List<CloudStack> getStacks() {
        return fetchList(this::getStackResources, ImmutableRawCloudStack::of);
    }

    @Override
    public void rename(String applicationName, String newName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .update(UpdateApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .name(newName)
                                                .build())
                .onErrorResume(t -> doesErrorMatchStatusCode(t, HttpStatus.SERVICE_UNAVAILABLE),
                               exception -> fallbackApplicationRename(newName, applicationGuid, exception))
                .block();
    }

    private Mono<? extends UpdateApplicationResponse> fallbackApplicationRename(String newName, UUID applicationGuid, Throwable exception) {
        String existingApplicationName = getApplicationByGuid(applicationGuid).block()
                                                                              .getName();
        if (newName.equals(existingApplicationName)) {
            return Mono.empty();
        }
        return Mono.error(exception);
    }

    @Override
    public void restartApplication(String applicationName) {
        stopApplication(applicationName);
        startApplication(applicationName);
    }

    @Override
    public void startApplication(String applicationName) {
        Application application = getApplicationByName(applicationName).block();
        if (application.getState() == ApplicationState.STARTED) {
            return;
        }
        UUID applicationGuid = UUID.fromString(application.getId());
        delegate.applicationsV3()
                .start(StartApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .build())
                .block();
    }

    @Override
    public void stopApplication(String applicationName) {
        Application application = getApplicationByName(applicationName).block();
        if (application.getState() == ApplicationState.STOPPED) {
            return;
        }
        UUID applicationGuid = UUID.fromString(application.getId());
        delegate.applicationsV3()
                .stop(StopApplicationRequest.builder()
                                            .applicationId(applicationGuid.toString())
                                            .build())
                .block();
    }

    @Override
    public Optional<String> unbindServiceInstance(String applicationName, String serviceInstanceName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);

        return doUnbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public Optional<String> deleteServiceBinding(UUID serviceBindingGuid) {
        return doDeleteServiceBinding(serviceBindingGuid);
    }

    @Override
    public Optional<String> unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        return doUnbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int diskQuota) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .type("web")
                                              .diskInMb(diskQuota)
                                              .build())
                .block();
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .updateEnvironmentVariables(UpdateApplicationEnvironmentVariablesRequest.builder()
                                                                                        .applicationId(applicationGuid.toString())
                                                                                        .vars(env)
                                                                                        .build())
                .block();
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .type("web")
                                              .instances(instances)
                                              .build())
                .block();
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .type("web")
                                              .memoryInMb(memory)
                                              .build())
                .block();
    }

    @Override
    public void updateApplicationMetadata(UUID guid, org.cloudfoundry.client.v3.Metadata metadata) {
        delegate.applicationsV3()
                .update(org.cloudfoundry.client.v3.applications.UpdateApplicationRequest.builder()
                                                                                        .applicationId(guid.toString())
                                                                                        .metadata(metadata)
                                                                                        .build())
                .block();
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .update(UpdateApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .lifecycle(buildApplicationLifecycle(staging))
                                                .build())
                .block();
        updateApplicationProcess(applicationGuid, staging);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> updatedRoutes) {
        UUID applicationGuid = getApplicationGuid(applicationName);
        List<CloudRoute> appRoutes = fetchList(() -> getRouteResourcesByAppGuid(applicationGuid), ImmutableRawCloudRoute::of);
        Set<CloudRoute> outdatedRoutes = getOutdatedRoutes(applicationGuid, appRoutes, updatedRoutes);
        Set<CloudRoute> newRoutes = getNewRoutes(applicationGuid, appRoutes, updatedRoutes);
        removeRoutes(outdatedRoutes, applicationGuid);
        addRoutes(newRoutes, applicationGuid);
    }

    private Set<CloudRoute> getOutdatedRoutes(UUID applicationGuid, List<CloudRoute> currentRoutes, Set<CloudRoute> updatedRoutes) {
        return currentRoutes.stream()
                            .filter(currentRoute -> isRouteOutdated(applicationGuid, currentRoute, updatedRoutes))
                            .collect(Collectors.toSet());
    }

    private boolean isRouteOutdated(UUID applicationGuid, CloudRoute currentRoute, Set<CloudRoute> updatedRoutes) {
        Optional<CloudRoute> updatedRoute = findRoute(currentRoute.getUrl(), updatedRoutes);
        if (updatedRoute.isEmpty()) {
            return true;
        }
        return isProtocolChanged(applicationGuid, currentRoute, updatedRoute.get());
    }

    private Optional<CloudRoute> findRoute(String url, Collection<CloudRoute> routes) {
        return routes.stream()
                     .filter(route -> Objects.equals(url, route.getUrl()))
                     .findFirst();
    }

    private boolean isProtocolChanged(UUID applicationGuid, CloudRoute currentRoute, CloudRoute updatedRoute) {
        if (updatedRoute.getRequestedProtocol() == null) {
            return false;
        }
        return currentRoute.getDestinations()
                           .stream()
                           .filter(routeDestination -> Objects.equals(routeDestination.getApplicationGuid(), applicationGuid))
                           .noneMatch(
                               routeDestination -> Objects.equals(routeDestination.getProtocol(), updatedRoute.getRequestedProtocol()));
    }

    private Set<CloudRoute> getNewRoutes(UUID applicationGuid, List<CloudRoute> currentRoutes, Set<CloudRoute> updatedRoutes) {
        return updatedRoutes.stream()
                            .filter(updatedRoute -> isRouteUpdated(applicationGuid, updatedRoute, currentRoutes))
                            .collect(Collectors.toSet());
    }

    private boolean isRouteUpdated(UUID applicationGuid, CloudRoute updatedRoute, List<CloudRoute> currentRoutes) {
        Optional<CloudRoute> currentRoute = findRoute(updatedRoute.getUrl(), currentRoutes);
        if (currentRoute.isEmpty()) {
            return true;
        }
        return isProtocolChanged(applicationGuid, currentRoute.get(), updatedRoute);
    }

    @Override
    public String updateServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        CloudServiceBroker existingBroker = getServiceBroker(serviceBroker.getName());
        UUID brokerGuid = existingBroker.getMetadata()
                                        .getGuid();

        return delegate.serviceBrokersV3()
                       .update(UpdateServiceBrokerRequest.builder()
                                                         .serviceBrokerId(brokerGuid.toString())
                                                         .name(serviceBroker.getName())
                                                         .authentication(BasicAuthentication.builder()
                                                                                            .username(serviceBroker.getUsername())
                                                                                            .password(serviceBroker.getPassword())
                                                                                            .build())
                                                         .url(serviceBroker.getUrl())
                                                         .build())
                       .flatMap(response -> Mono.justOrEmpty(response.jobId()))
                       .block();
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, org.cloudfoundry.client.v3.Metadata metadata) {
        delegate.serviceInstancesV3()
                .update(UpdateServiceInstanceRequest.builder()
                                                    .serviceInstanceId(guid.toString())
                                                    .metadata(metadata)
                                                    .build())
                .block();
    }

    @Override
    public void updateServiceBindingMetadata(UUID guid, org.cloudfoundry.client.v3.Metadata metadata) {
        delegate.serviceBindingsV3()
                .update(UpdateServiceBindingRequest.builder()
                                                   .serviceBindingId(guid.toString())
                                                   .metadata(metadata)
                                                   .build())
                .block();
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, ServicePlanVisibility visibility) {
        CloudServiceBroker broker = getServiceBroker(name);
        List<CloudServicePlan> servicePlans = findServicePlansByBrokerGuid(getGuid(broker));
        for (CloudServicePlan servicePlan : servicePlans) {
            updateServicePlanVisibility(servicePlan, visibility);
        }
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return fetch(() -> getTaskResource(taskGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return fetchList(() -> getTaskResourcesByApplicationGuid(applicationGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return createTask(applicationGuid, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return fetch(() -> cancelTaskResource(taskGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback, Duration uploadTimeout) {
        CloudPackage cloudPackage = startUpload(applicationName, file, uploadTimeout);
        processAsyncUploadInBackground(cloudPackage, callback);
        return cloudPackage;
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        CloudPackage cloudPackage = getPackage(packageGuid);
        ErrorDetails errorDetails = null;
        if (cloudPackage.getType() == CloudPackage.Type.BITS) {
            errorDetails = ImmutableErrorDetails.builder()
                                                .description(((BitsData) cloudPackage.getData()).getError())
                                                .build();
        }
        return ImmutableUpload.builder()
                              .status(cloudPackage.getStatus())
                              .errorDetails(errorDetails)
                              .build();
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return fetch(() -> getBuildResource(buildGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return fetchList(() -> getBuildResourcesByApplicationGuid(applicationGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return fetchList(() -> getBuildResourcesByPackageGuid(packageGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return fetch(() -> createBuildResource(packageGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        delegate.applicationsV3()
                .setCurrentDroplet(SetApplicationCurrentDropletRequest.builder()
                                                                      .applicationId(applicationGuid.toString())
                                                                      .data(Relationship.builder()
                                                                                        .id(dropletGuid.toString())
                                                                                        .build())
                                                                      .build())
                .block();
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        GetApplicationCurrentDropletResponse dropletResponse = delegate.applicationsV3()
                                                                       .getCurrentDroplet(GetApplicationCurrentDropletRequest.builder()
                                                                                                                             .applicationId(
                                                                                                                                 applicationGuid.toString())
                                                                                                                             .build())
                                                                       .block();
        if (dropletResponse == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not found",
                                              "Application with guid " + applicationGuid + " does not have a droplet");
        }
        return parseDropletInfo(dropletResponse);
    }

    private DropletInfo parseDropletInfo(GetApplicationCurrentDropletResponse getApplicationCurrentDropletResponse) {
        String packageUrl = getApplicationCurrentDropletResponse.getLinks()
                                                                .get(Constants.PACKAGE)
                                                                .getHref();
        if (packageUrl.endsWith("/")) {
            packageUrl = packageUrl.substring(0, packageUrl.lastIndexOf("/"));
        }
        String packageGuid = packageUrl.substring(packageUrl.lastIndexOf("/") + 1);
        return ImmutableDropletInfo.builder()
                                   .guid(UUID.fromString(getApplicationCurrentDropletResponse.getId()))
                                   .packageGuid(UUID.fromString(packageGuid))
                                   .build();
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return fetchList(() -> getPackages(applicationGuid.toString()), ImmutableRawCloudPackage::of);
    }

    private Flux<? extends PackageResource> getPackages(String applicationGuid) {
        IntFunction<ListApplicationPackagesRequest> pageRequestSupplier = page -> ListApplicationPackagesRequest.builder()
                                                                                                                .page(page)
                                                                                                                .applicationId(
                                                                                                                    applicationGuid)
                                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .listPackages(pageRequestSupplier.apply(page)));
    }

    @Override
    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        Supplier<Set<UserRole>> setFactory = () -> EnumSet.noneOf(UserRole.class);
        return fetchFlux(() -> getRoles(spaceGuid, userGuid), ImmutableRawUserRole::of).collect(Collectors.toCollection(setFactory))
                                                                                       .block();
    }

    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        org.cloudfoundry.client.v3.packages.DockerData.Builder dockerDataBuilder = org.cloudfoundry.client.v3.packages.DockerData.builder()
                                                                                                                                 .image(
                                                                                                                                     dockerInfo.getImage());
        if (dockerInfo.getCredentials() != null) {
            addNonNullDockerCredentials(dockerInfo.getCredentials(), dockerDataBuilder);
        }
        CreatePackageRequest packageRequest = CreatePackageRequest.builder()
                                                                  .type(PackageType.DOCKER)
                                                                  .data(dockerDataBuilder.build())
                                                                  .relationships(buildPackageRelationships(applicationGuid))
                                                                  .build();
        CreatePackageResponse packageResponse = delegate.packages()
                                                        .create(packageRequest)
                                                        .block();
        return getPackage(UUID.fromString(packageResponse.getId()));
    }

    @Override
    public CloudAsyncJob getAsyncJob(String jobId) {
        return fetch(() -> delegate.jobsV3()
                                   .get(GetJobRequest.builder()
                                                     .jobId(jobId)
                                                     .build()), ImmutableRawCloudAsyncJob::of);
    }

    private void addNonNullDockerCredentials(DockerCredentials dockerCredentials,
                                             org.cloudfoundry.client.v3.packages.DockerData.Builder dockerDataBuilder) {
        String username = dockerCredentials.getUsername();
        if (username != null) {
            dockerDataBuilder.username(username);
        }
        String password = dockerCredentials.getPassword();
        if (password != null) {
            dockerDataBuilder.password(password);
        }
    }

    private Flux<RoleResource> getRoles(UUID spaceGuid, UUID userGuid) {
        IntFunction<ListRolesRequest> pageRequestSupplier = page -> ListRolesRequest.builder()
                                                                                    .page(page)
                                                                                    .spaceId(spaceGuid.toString())
                                                                                    .userId(userGuid.toString())
                                                                                    .types(RoleType.values())
                                                                                    .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.rolesV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudApplication findApplicationByName(String name, boolean required) {
        CloudApplication application = findApplicationByName(name);
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + name + " not found.");
        }
        return application;
    }

    private CloudApplication findApplicationByName(String name) {
        return fetch(() -> getApplicationByName(name), application -> ImmutableRawCloudApplication.builder()
                                                                                                  .application(application)
                                                                                                  .space(target)
                                                                                                  .build());
    }

    private Flux<? extends Application> getApplicationResources() {
        assertSpaceProvided("get application");
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends Application> getApplicationByGuid(UUID guid) {
        return delegate.applicationsV3()
                       .get(GetApplicationRequest.builder()
                                                 .applicationId(guid.toString())
                                                 .build());
    }

    private Mono<? extends Application> getApplicationByName(String name) {
        assertSpaceProvided("get application");
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .name(name)
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private CloudServiceInstance findServiceInstanceByName(String name) {
        return fetchWithAuxiliaryContent(() -> getServiceInstanceResourceByName(name), this::zipWithAuxiliaryServiceInstanceContent);
    }

    private Mono<? extends ServiceInstance> getServiceInstanceByGuid(UUID serviceInstanceGuid) {
        return delegate.serviceInstancesV3()
                       .get(GetServiceInstanceRequest.builder()
                                                     .serviceInstanceId(serviceInstanceGuid.toString())
                                                     .build());
    }

    private Mono<ServiceInstanceResource> getServiceInstanceResourceByName(String name) {
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .serviceInstanceName(name)
                                                                                                          .page(page)
                                                                                                          .build();
        return getServiceInstanceResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<ServiceInstanceResource> getServiceInstanceResources(IntFunction<ListServiceInstancesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceInstancesV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceInstance>> zipWithAuxiliaryServiceInstanceContent(ServiceInstanceResource serviceInstanceResource) {
        if (isUserProvided(serviceInstanceResource)) {
            return Mono.just(ImmutableRawCloudServiceInstance.of(serviceInstanceResource));
        }
        String servicePlanGuid = serviceInstanceResource.getRelationships()
                                                        .getServicePlan()
                                                        .getData()
                                                        .getId();

        return getServicePlanResource(servicePlanGuid, serviceInstanceResource.getName()).zipWhen(servicePlan -> getServiceOffering(
                                                                                             servicePlan.getRelationships()
                                                                                                        .getServiceOffering()
                                                                                                        .getData()
                                                                                                        .getId()))
                                                                                         .map(
                                                                                             tuple -> ImmutableRawCloudServiceInstance.builder()
                                                                                                                                      .resource(
                                                                                                                                          serviceInstanceResource)
                                                                                                                                      .servicePlan(
                                                                                                                                          tuple.getT1())
                                                                                                                                      .serviceOffering(
                                                                                                                                          tuple.getT2())
                                                                                                                                      .build());
    }

    private boolean isUserProvided(ServiceInstanceResource serviceInstanceResource) {
        return ServiceInstanceType.USER_PROVIDED.equals(serviceInstanceResource.getType());
    }

    private Flux<? extends ServiceBindingResource> getServiceBindingResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .serviceInstanceId(
                                                                                                            serviceInstanceGuid.toString())
                                                                                                        .type(
                                                                                                            ServiceBindingType.APPLICATION)
                                                                                                        .page(page)
                                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceBindingsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends ServiceBindingResource> getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(UUID applicationGuid,
                                                                                                                    UUID serviceInstanceGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .applicationId(
                                                                                                            applicationGuid.toString())
                                                                                                        .serviceInstanceId(
                                                                                                            serviceInstanceGuid.toString())
                                                                                                        .page(page)
                                                                                                        .build();
        return getApplicationServiceBindingResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends ServiceBindingResource> getServiceBindingResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .applicationId(
                                                                                                            applicationGuid.toString())
                                                                                                        .page(page)
                                                                                                        .build();
        return getApplicationServiceBindingResources(pageRequestSupplier);
    }

    private Flux<? extends ServiceBindingResource> getApplicationServiceBindingResources(
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceBindingsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudServicePlan> findServicePlansByBrokerGuid(UUID brokerGuid) {
        List<CloudServiceOffering> offerings = findServiceOfferingsByBrokerGuid(brokerGuid);
        return getPlans(offerings);
    }

    private List<CloudServicePlan> getPlans(List<CloudServiceOffering> offerings) {
        return offerings.stream()
                        .map(CloudServiceOffering::getServicePlans)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }

    private void updateServicePlanVisibility(CloudServicePlan servicePlan, ServicePlanVisibility visibility) {
        updateServicePlanVisibility(getGuid(servicePlan), visibility);
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, ServicePlanVisibility visibility) {
        delegate.servicePlansV3()
                .updateVisibility(UpdateServicePlanVisibilityRequest.builder()
                                                                    .servicePlanId(servicePlanGuid.toString())
                                                                    .type(Visibility.from(visibility.toString()))
                                                                    .build())
                .block();
    }

    private Mono<? extends Task> getTaskResource(UUID guid) {
        GetTaskRequest request = GetTaskRequest.builder()
                                               .taskId(guid.toString())
                                               .build();
        return delegate.tasks()
                       .get(request);
    }

    private Flux<? extends Task> getTaskResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListTasksRequest> pageRequestSupplier = page -> ListTasksRequest.builder()
                                                                                    .applicationId(applicationGuid.toString())
                                                                                    .page(page)
                                                                                    .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.tasks()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudTask createTask(UUID applicationGuid, CloudTask task) {
        return fetch(() -> createTaskResource(applicationGuid, task), ImmutableRawCloudTask::of);
    }

    private Mono<? extends Task> createTaskResource(UUID applicationGuid, CloudTask task) {
        CreateTaskRequest request = CreateTaskRequest.builder()
                                                     .applicationId(applicationGuid.toString())
                                                     .command(task.getCommand())
                                                     .name(task.getName())
                                                     .memoryInMb(task.getLimits()
                                                                     .getMemory())
                                                     .diskInMb(task.getLimits()
                                                                   .getDisk())
                                                     .build();
        return delegate.tasks()
                       .create(request);
    }

    private Mono<? extends Task> cancelTaskResource(UUID taskGuid) {
        CancelTaskRequest request = CancelTaskRequest.builder()
                                                     .taskId(taskGuid.toString())
                                                     .build();
        return delegate.tasks()
                       .cancel(request);
    }

    private CloudPackage startUpload(String applicationName, Path file, Duration uploadTimeout) {
        Assert.notNull(applicationName, "AppName must not be null");
        Assert.notNull(file, "File must not be null");

        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID packageGuid = getGuid(createPackageForApplication(applicationGuid));

        delegate.packages()
                .upload(UploadPackageRequest.builder()
                                            .bits(file)
                                            .packageId(packageGuid.toString())
                                            .build())
                .timeout(uploadTimeout)
                .block();

        return getPackage(packageGuid);
    }

    private CloudPackage createPackageForApplication(UUID applicationGuid) {
        return fetch(() -> createPackageResource(applicationGuid), ImmutableRawCloudPackage::of);
    }

    private Mono<? extends org.cloudfoundry.client.v3.packages.Package> createPackageResource(UUID applicationGuid) {
        CreatePackageRequest request = CreatePackageRequest.builder()
                                                           .type(PackageType.BITS)
                                                           .relationships(buildPackageRelationships(applicationGuid))
                                                           .build();
        return delegate.packages()
                       .create(request);
    }

    private PackageRelationships buildPackageRelationships(UUID applicationGuid) {
        return PackageRelationships.builder()
                                   .application(buildToOneRelationship(applicationGuid))
                                   .build();
    }

    private ToOneRelationship buildToOneRelationship(UUID guid) {
        return ToOneRelationship.builder()
                                .data(buildRelationship(guid.toString()))
                                .build();
    }

    private ToOneRelationship buildToOneRelationship(String guid) {
        return ToOneRelationship.builder()
                                .data(buildRelationship(guid))
                                .build();
    }

    private Relationship buildRelationship(String guid) {
        return Relationship.builder()
                           .id(guid)
                           .build();
    }

    private Mono<? extends Build> createBuildResource(UUID packageGuid) {
        CreateBuildRequest request = CreateBuildRequest.builder()
                                                       .getPackage(buildRelationship(packageGuid.toString()))
                                                       .build();
        return delegate.builds()
                       .create(request);
    }

    private Mono<? extends Build> getBuildResource(UUID buildGuid) {
        GetBuildRequest request = GetBuildRequest.builder()
                                                 .buildId(buildGuid.toString())
                                                 .build();
        return delegate.builds()
                       .get(request);
    }

    private Flux<? extends Build> getBuildResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListApplicationBuildsRequest> pageRequestSupplier = page -> ListApplicationBuildsRequest.builder()
                                                                                                            .applicationId(
                                                                                                                applicationGuid.toString())
                                                                                                            .page(page)
                                                                                                            .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .listBuilds(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Build> getBuildResourcesByPackageGuid(UUID packageGuid) {
        IntFunction<ListBuildsRequest> pageRequestSupplier = page -> ListBuildsRequest.builder()
                                                                                      .packageId(packageGuid.toString())
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.builds()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(target, "Unable to " + operation + " without specifying organization and space to use.");
    }

    private void removeRoutes(Set<CloudRoute> routes, UUID applicationGuid) {
        for (CloudRoute route : routes) {
            for (RouteDestination destination : route.getDestinations()) {
                if (destination.getApplicationGuid()
                               .equals(applicationGuid)) {
                    unbindRoute(route.getGuid(), destination.getGuid());
                }
            }
        }
    }

    private void validateDomainForRoute(CloudRoute route, Map<String, UUID> existingDomains) {
        String domain = route.getDomain()
                             .getName();
        if (!StringUtils.hasLength(domain) || !existingDomains.containsKey(domain)) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              "Domain '" + domain + "' not found for URI " + route.getUrl());
        }
    }

    private void unbindRoute(UUID routeGuid, UUID destinationGuid) {
        delegate.routesV3()
                .removeDestinations(RemoveRouteDestinationsRequest.builder()
                                                                  .routeId(routeGuid.toString())
                                                                  .destinationId(destinationGuid.toString())
                                                                  .build())
                .block();
    }

    private void bindRoute(UUID routeGuid, UUID applicationGuid, String protocol) {
        delegate.routesV3()
                .insertDestinations(InsertRouteDestinationsRequest.builder()
                                                                  .routeId(routeGuid.toString())
                                                                  .destination(createDestination(applicationGuid, protocol))
                                                                  .build())
                .block();
    }

    private Destination createDestination(UUID applicationGuid, String protocol) {
        return Destination.builder()
                          .application(org.cloudfoundry.client.v3.routes.Application.builder()
                                                                                    .applicationId(applicationGuid.toString())
                                                                                    .build())
                          .protocol(protocol)
                          .build();
    }

    private UUID getOrAddRoute(UUID domainGuid, String host, String path) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            routeGuid = doAddRoute(domainGuid, host, path);
        }
        return routeGuid;
    }

    private UUID doAddRoute(UUID domainGuid, String host, String path) {
        assertSpaceProvided("add route");
        CreateRouteResponse response = delegate.routesV3()
                                               .create(CreateRouteRequest.builder()
                                                                         .host(host)
                                                                         .path(path)
                                                                         .relationships(RouteRelationships.builder()
                                                                                                          .domain(buildToOneRelationship(
                                                                                                              domainGuid))
                                                                                                          .space(buildToOneRelationship(
                                                                                                              getTargetSpaceGuid()))
                                                                                                          .build())
                                                                         .build())
                                               .block();
        return getGuid(response);
    }

    private void doCreateDomain(String name) {
        delegate.domainsV3()
                .create(CreateDomainRequest.builder()
                                           .name(name)
                                           .relationships(DomainRelationships.builder()
                                                                             .organization(
                                                                                 buildToOneRelationship(getTargetOrganizationGuid()))
                                                                             .build())
                                           .build())
                .block();
    }

    private void doDeleteDomain(UUID guid) {
        delegate.domainsV3()
                .delete(DeleteDomainRequest.builder()
                                           .domainId(guid.toString())
                                           .build())
                .flatMap(jobId -> JobV3Util.waitForCompletion(delegate, DELETE_JOB_TIMEOUT, jobId))
                .block();
    }

    private void doDeleteRoute(UUID guid) {
        delegate.routesV3()
                .delete(DeleteRouteRequest.builder()
                                          .routeId(guid.toString())
                                          .build())
                .flatMap(jobId -> JobV3Util.waitForCompletion(delegate, DELETE_JOB_TIMEOUT, jobId))
                .block();
    }

    private void doDeleteServiceInstance(UUID serviceInstanceGuid) {
        delegate.serviceInstancesV3()
                .delete(DeleteServiceInstanceRequest.builder()
                                                    .serviceInstanceId(serviceInstanceGuid.toString())
                                                    .build())
                .block();
    }

    private Optional<String> doUnbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        UUID serviceBindingGuid = getServiceBindingGuid(applicationGuid, serviceInstanceGuid);
        if (serviceBindingGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                              "Service binding between service with GUID " + serviceInstanceGuid
                                                  + " and application with GUID " + applicationGuid + " not found.");
        }
        return doDeleteServiceBinding(serviceBindingGuid);
    }

    private Optional<String> doDeleteServiceBinding(UUID guid) {
        String jobId = delegate.serviceBindingsV3()
                               .delete(DeleteServiceBindingRequest.builder()
                                                                  .serviceBindingId(guid.toString())
                                                                  .build())
                               .block();
        return Optional.ofNullable(jobId);
    }

    private CloudDomain findDomainByName(String name, boolean required) {
        CloudDomain domain = findDomainByName(name);
        if (domain == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain " + name + " not found.");
        }
        return domain;
    }

    private CloudDomain findDomainByName(String name) {
        return fetch(() -> getDomainResourceByName(name), ImmutableRawCloudDomain::of);
    }

    private List<CloudDomain> findDomainsByOrganizationGuid(UUID organizationGuid) {
        return fetchList(() -> getDomainResourcesByOrganizationGuid(organizationGuid), ImmutableRawCloudDomain::of);
    }

    private Mono<DomainResource> getDomainResourceByName(String name) {
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
                                                                                        .name(name)
                                                                                        .page(page)
                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.domainsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private Flux<DomainResource> getDomainResources() {
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
                                                                                        .page(page)
                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.domainsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<DomainResource> getSharedDomainResources() {
        return getDomainResources().filter(domain -> domain.getRelationships()
                                                           .getOrganization()
                                                           .getData() == null);
    }

    private Flux<DomainResource> getPrivateDomainResources() {
        return getDomainResources().filter(domain -> domain.getRelationships()
                                                           .getOrganization()
                                                           .getData() != null);
    }

    private Flux<DomainResource> getDomainResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListOrganizationDomainsRequest> pageRequestSupplier = page -> ListOrganizationDomainsRequest.builder()
                                                                                                                .organizationId(
                                                                                                                    organizationGuid.toString())
                                                                                                                .page(page)
                                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.organizationsV3()
                                                                        .listDomains(pageRequestSupplier.apply(page)));
    }

    private Flux<DomainResource> getDomainResourcesByNamesInBatches(Set<String> names) {
        return Flux.fromIterable(toBatches(names, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST))
                   .flatMap(this::getDomainResourcesByNames);
    }

    private Flux<DomainResource> getDomainResourcesByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return Flux.empty();
        }
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
                                                                                        .names(names)
                                                                                        .page(page)
                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.domainsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudRoute> findRoutes(CloudDomain domain) {
        return fetchList(() -> getRouteResourcesByDomainGuidAndSpaceGuid(domain.getGuid(), getTargetSpaceGuid()),
                         ImmutableRawCloudRoute::of);
    }

    private Flux<RouteResource> getRouteResourcesByDomainGuidAndSpaceGuid(UUID domainGuid, UUID spaceGuid) {
        IntFunction<ListRoutesRequest> pageRequestSupplier = page -> ListRoutesRequest.builder()
                                                                                      .domainId(domainGuid.toString())
                                                                                      .spaceId(spaceGuid.toString())
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.routesV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<RouteResource> getRouteResourcesByDomainGuidHostAndPath(UUID domainGuid, String host, String path) {
        ListRoutesRequest.Builder requestBuilder = ListRoutesRequest.builder();
        if (host != null) {
            requestBuilder.host(host);
        }
        if (path != null) {
            requestBuilder.path(path);
        }
        requestBuilder.spaceId(getTargetSpaceGuid().toString())
                      .domainId(domainGuid.toString());

        return PaginationUtils.requestClientV3Resources(page -> delegate.routesV3()
                                                                        .list(requestBuilder.page(page)
                                                                                            .build()));
    }

    private Flux<RouteResource> getRouteResourcesByAppGuid(UUID applicationGuid) {
        IntFunction<ListApplicationRoutesRequest> pageSupplier = page -> ListApplicationRoutesRequest.builder()
                                                                                                     .applicationId(
                                                                                                         applicationGuid.toString())
                                                                                                     .page(page)
                                                                                                     .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .listRoutes(pageSupplier.apply(page)));
    }

    private List<CloudServiceOffering> findServiceOfferingsByBrokerGuid(UUID brokerGuid) {
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByBrokerGuid(brokerGuid),
                                             this::zipWithAuxiliaryServiceOfferingContent);
    }

    private List<CloudServiceOffering> findServiceOfferingsByLabel(String label) {
        Assert.notNull(label, "Service label must not be null");
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByLabel(label), this::zipWithAuxiliaryServiceOfferingContent);
    }

    private List<CloudServiceOffering> findServiceOfferingsByLabelAndBrokerName(String label, String brokerName) {
        Assert.notNull(label, "Service label must not be null");
        Assert.notNull(brokerName, "Service broker must not be null");
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByLabelAndBrokerName(label, brokerName),
                                             this::zipWithAuxiliaryServiceOfferingContent);
    }

    private Flux<? extends ServiceOfferingResource> getServiceResources() {
        IntFunction<ListServiceOfferingsRequest> pageRequestSupplier = page -> ListServiceOfferingsRequest.builder()
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .page(page)
                                                                                                          .build();
        return getServiceResources(pageRequestSupplier);
    }

    protected Mono<? extends ServiceOffering> getServiceOffering(String offeringId) {
        GetServiceOfferingRequest request = GetServiceOfferingRequest.builder()
                                                                     .serviceOfferingId(offeringId)
                                                                     .build();
        return delegate.serviceOfferingsV3()
                       .get(request)
                       .onErrorMap(t -> doesErrorMatchStatusCode(t, HttpStatus.FORBIDDEN),
                                   t -> new CloudOperationException(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase(),
                                                                    MessageFormat.format(
                                                                        Messages.SERVICE_OFFERING_WITH_GUID_0_IS_NOT_AVAILABLE, offeringId),
                                                                    t))
                       .onErrorMap(t -> doesErrorMatchStatusCode(t, HttpStatus.NOT_FOUND),
                                   t -> new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                                    MessageFormat.format(Messages.SERVICE_OFFERING_WITH_GUID_0_NOT_FOUND,
                                                                                         offeringId), t));
    }

    private Flux<? extends ServiceOfferingResource> getServiceResourcesByBrokerGuid(UUID brokerGuid) {
        IntFunction<ListServiceOfferingsRequest> pageRequestSupplier = page -> ListServiceOfferingsRequest.builder()
                                                                                                          .serviceBrokerId(
                                                                                                              brokerGuid.toString())
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .page(page)
                                                                                                          .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends ServiceOfferingResource> getServiceResourcesByLabel(String label) {
        IntFunction<ListServiceOfferingsRequest> pageRequestSupplier = page -> ListServiceOfferingsRequest.builder()
                                                                                                          .name(label)
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .page(page)
                                                                                                          .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends ServiceOfferingResource> getServiceResourcesByLabelAndBrokerName(String label, String brokerName) {
        IntFunction<ListServiceOfferingsRequest> pageRequestSupplier = page -> ListServiceOfferingsRequest.builder()
                                                                                                          .name(label)
                                                                                                          .serviceBrokerName(brokerName)
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .page(page)
                                                                                                          .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends ServiceOfferingResource> getServiceResources(IntFunction<ListServiceOfferingsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceOfferingsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceOffering>> zipWithAuxiliaryServiceOfferingContent(ServiceOfferingResource serviceOffering) {
        UUID serviceOfferingGuid = getGuid(serviceOffering);
        return getServicePlansFlux(serviceOfferingGuid).collectList()
                                                       .map(servicePlans -> ImmutableRawCloudServiceOffering.builder()
                                                                                                            .serviceOffering(
                                                                                                                serviceOffering)
                                                                                                            .servicePlans(servicePlans)
                                                                                                            .build());
    }

    private Flux<CloudServicePlan> getServicePlansFlux(UUID serviceOfferingGuid) {
        return fetchFlux(() -> getServicePlanResourcesByServiceOfferingGuid(serviceOfferingGuid), ImmutableRawCloudServicePlan::of);
    }

    protected Mono<? extends ServicePlan> getServicePlanResource(String servicePlanGuid, String serviceInstanceName) {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(servicePlanGuid)
                                                             .build();

        return delegate.servicePlansV3()
                       .get(request)
                       .onErrorMap(t -> doesErrorMatchStatusCode(t, HttpStatus.FORBIDDEN),
                                   t -> new CloudOperationException(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase(),
                                                                    MessageFormat.format(
                                                                        Messages.SERVICE_PLAN_WITH_GUID_0_NOT_AVAILABLE_FOR_SERVICE_INSTANCE_1,
                                                                        servicePlanGuid, serviceInstanceName), t))
                       .onErrorMap(t -> doesErrorMatchStatusCode(t, HttpStatus.NOT_FOUND),
                                   t -> new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                                                    MessageFormat.format(Messages.NO_SERVICE_PLAN_FOUND, servicePlanGuid,
                                                                                         serviceInstanceName), t));
    }

    private Flux<? extends ServicePlanResource> getServicePlanResourcesByServiceOfferingGuid(UUID serviceOfferingGuid) {
        IntFunction<ListServicePlansRequest> pageRequestSupplier = page -> ListServicePlansRequest.builder()
                                                                                                  .serviceOfferingId(
                                                                                                      serviceOfferingGuid.toString())
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.servicePlansV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends ServiceBindingResource> getServiceKeyResource(CloudServiceInstance serviceInstance) {
        UUID serviceInstanceGuid = getGuid(serviceInstance);
        return getServiceKeyResourcesByServiceInstanceGuid(serviceInstanceGuid);
    }

    private Flux<? extends ServiceBindingResource> getServiceKeyResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .serviceInstanceId(
                                                                                                            serviceInstanceGuid.toString())
                                                                                                        .type(ServiceBindingType.KEY)
                                                                                                        .page(page)
                                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceBindingsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends ServiceBindingResource> getServiceKeyResourceByNameAndServiceInstanceGuid(String name, UUID guid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .serviceInstanceId(guid.toString())
                                                                                                        .type(ServiceBindingType.KEY)
                                                                                                        .name(name)
                                                                                                        .page(page)
                                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceBindingsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private CloudStack findStackResource(String name) {
        return fetch(() -> getStackResourceByName(name), ImmutableRawCloudStack::of);
    }

    private Flux<? extends Stack> getStackResources() {
        IntFunction<ListStacksRequest> pageRequestSupplier = page -> ListStacksRequest.builder()
                                                                                      .page(page)
                                                                                      .build();
        return getStackResources(pageRequestSupplier);
    }

    private Mono<? extends Stack> getStackResourceByName(String name) {
        IntFunction<ListStacksRequest> pageRequestSupplier = page -> ListStacksRequest.builder()
                                                                                      .name(name)
                                                                                      .page(page)
                                                                                      .build();
        return getStackResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Stack> getStackResources(IntFunction<ListStacksRequest> requestForPage) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.stacksV3()
                                                                        .list(requestForPage.apply(page)));
    }

    private List<CloudEvent> findEventsByTarget(String target) {
        return fetchList(() -> getEventResourcesByTarget(target), ImmutableRawCloudEvent::of);
    }

    private Flux<AuditEventResource> getEventResources() {
        IntFunction<ListAuditEventsRequest> pageRequestSupplier = page -> ListAuditEventsRequest.builder()
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.auditEventsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<AuditEventResource> getEventResourcesByTarget(String target) {
        IntFunction<ListAuditEventsRequest> pageRequestSupplier = page -> ListAuditEventsRequest.builder()
                                                                                                .targetId(target)
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.auditEventsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private InstancesInfo findApplicationInstances(UUID applicationGuid) {
        return fetch(() -> getApplicationProcessStatsResource(applicationGuid), ImmutableRawInstancesInfo::of);
    }

    private Mono<GetApplicationProcessStatisticsResponse> getApplicationProcessStatsResource(UUID applicationGuid) {
        return delegate.applicationsV3()
                       .getProcessStatistics(GetApplicationProcessStatisticsRequest.builder()
                                                                                   .applicationId(applicationGuid.toString())
                                                                                   .type("web")
                                                                                   .build());
    }

    private CloudServiceBroker findServiceBrokerByName(String name) {
        return fetch(() -> getServiceBrokerResourceByName(name), ImmutableRawCloudServiceBroker::of);
    }

    private Flux<? extends ServiceBrokerResource> getServiceBrokerResources() {
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier = page -> ListServiceBrokersRequest.builder()
                                                                                                      .page(page)
                                                                                                      .build();
        return getServiceBrokerResources(pageRequestSupplier);
    }

    private Mono<? extends ServiceBrokerResource> getServiceBrokerResourceByName(String name) {
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier = page -> ListServiceBrokersRequest.builder()
                                                                                                      .page(page)
                                                                                                      .name(name)
                                                                                                      .build();
        return getServiceBrokerResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends ServiceBrokerResource> getServiceBrokerResources(IntFunction<ListServiceBrokersRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceBrokersV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudServicePlan findPlanForService(CloudServiceInstance service, String planName) {
        List<CloudServiceOffering> offerings = getServiceOfferings(service);
        for (var offering : offerings) {
            for (var plan : offering.getServicePlans()) {
                if (planName.equals(plan.getName())) {
                    return plan;
                }
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service plan " + service.getPlan() + " not found.");
    }

    private List<CloudServiceOffering> getServiceOfferings(CloudServiceInstance service) {
        if (service.getBroker() == null) {
            return findServiceOfferingsByLabel(service.getLabel());
        }
        return findServiceOfferingsByLabelAndBrokerName(service.getLabel(), service.getBroker());
    }

    private UUID getRequiredApplicationGuid(String name) {
        Resource applicationResource = getApplicationByName(name).block();
        if (applicationResource == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + name + " not found.");
        }
        return UUID.fromString(applicationResource.getId());
    }

    private Mono<ServiceInstanceResource> getServiceInstanceByName(String name) {
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .spaceId(
                                                                                                              getTargetSpaceGuid().toString())
                                                                                                          .serviceInstanceName(name)
                                                                                                          .page(page)
                                                                                                          .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceInstancesV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private UUID getRequiredDomainGuid(String name) {
        return getGuid(findDomainByName(name, true));
    }

    private Map<String, UUID> getDomainsFromRoutes(Set<CloudRoute> routes) {
        Set<String> domainNames = routes.stream()
                                        .map(route -> route.getDomain()
                                                           .getName())
                                        .filter(StringUtils::hasLength)
                                        .collect(Collectors.toSet());
        return getDomainResourcesByNamesInBatches(domainNames).collectMap(DomainResource::getName,
                                                                          domain -> UUID.fromString(domain.getId()))
                                                              .block();
    }

    private UUID getRouteGuid(UUID domainGuid, String host, String path) {
        List<RouteResource> routeEntitiesResource = getRouteResourcesByDomainGuidHostAndPath(domainGuid, host, path).collect(
                                                                                                                        Collectors.toList())
                                                                                                                    .block();
        if (CollectionUtils.isEmpty(routeEntitiesResource)) {
            return null;
        }
        return getGuid(routeEntitiesResource.get(0));
    }

    private UUID getServiceBindingGuid(UUID applicationGuid, UUID serviceInstanceGuid) {
        return getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(applicationGuid, serviceInstanceGuid).map(this::getGuid)
                                                                                                                     .block();
    }

    private void processAsyncUploadInBackground(CloudPackage cloudPackage, UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", cloudPackage.getGuid());
        new Thread(() -> processAsyncUpload(cloudPackage, callback), threadName).start();
    }

    private void processAsyncUpload(CloudPackage cloudPackage, UploadStatusCallback callback) {
        while (true) {
            Upload upload = getUploadStatus(cloudPackage.getGuid());
            Status uploadStatus = upload.getStatus();
            boolean unsubscribe = callback.onProgress(uploadStatus.toString());
            if (unsubscribe || isUploadReady(uploadStatus)) {
                return;
            }
            if (hasUploadFailed(uploadStatus)) {
                callback.onError(upload.getErrorDetails()
                                       .getDescription());
                return;
            }
            try {
                Thread.sleep(PACKAGE_UPLOAD_JOB_POLLING_PERIOD);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private boolean isUploadReady(Status status) {
        return status == Status.READY;
    }

    private boolean hasUploadFailed(Status status) {
        return status == Status.EXPIRED || status == Status.FAILED;
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return fetch(() -> getPackageResource(packageGuid), ImmutableRawCloudPackage::of);
    }

    private Mono<? extends Package> getPackageResource(UUID guid) {
        GetPackageRequest request = GetPackageRequest.builder()
                                                     .packageId(guid.toString())
                                                     .build();
        return delegate.packages()
                       .get(request);
    }

    private UUID getTargetOrganizationGuid() {
        return getGuid(target.getOrganization());
    }

    private UUID getTargetSpaceGuid() {
        return getGuid(target);
    }

    private UUID getGuid(CloudEntity entity) {
        return Optional.ofNullable(entity)
                       .map(CloudEntity::getMetadata)
                       .map(CloudMetadata::getGuid)
                       .orElse(null);
    }

    private UUID getGuid(Resource resource) {
        return Optional.ofNullable(resource)
                       .map(Resource::getId)
                       .map(UUID::fromString)
                       .orElse(null);
    }

    private boolean doesErrorMatchStatusCode(Throwable t, HttpStatus status) {
        if (t instanceof AbstractCloudFoundryException e) {
            return e.getStatusCode() == status.value();
        }
        return false;
    }

    private <T, R, D extends Derivable<T>> Flux<T> fetchFluxWithAuxiliaryContent(Supplier<Flux<R>> resourceSupplier,
                                                                                 Function<R, Mono<D>> resourceMapper) {
        return resourceSupplier.get()
                               .flatMap(resourceMapper)
                               .map(Derivable::derive);
    }

    private <T, R, D extends Derivable<T>> List<T> fetchListWithAuxiliaryContent(Supplier<Flux<R>> resourceSupplier,
                                                                                 Function<R, Mono<D>> resourceMapper) {
        return fetchFluxWithAuxiliaryContent(resourceSupplier, resourceMapper).collectList()
                                                                              .block();
    }

    private <T, R, D extends Derivable<T>> List<T> fetchList(Supplier<Flux<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return fetchFlux(resourceSupplier, resourceMapper).collectList()
                                                          .block();
    }

    private <T, R, D extends Derivable<T>> Flux<T> fetchFlux(Supplier<Flux<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return resourceSupplier.get()
                               .map(resourceMapper)
                               .map(Derivable::derive);
    }

    private <T, R, D extends Derivable<T>> T fetchWithAuxiliaryContent(Supplier<Mono<R>> resourceSupplier,
                                                                       Function<R, Mono<D>> resourceMapper) {
        return fetchMonoWithAuxiliaryContent(resourceSupplier, resourceMapper).block();
    }

    private <T, R, D extends Derivable<T>> T fetch(Supplier<Mono<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return fetchMono(resourceSupplier, resourceMapper).block();
    }

    private <T, R, D extends Derivable<T>> Mono<T> fetchMonoWithAuxiliaryContent(Supplier<Mono<R>> resourceSupplier,
                                                                                 Function<R, Mono<D>> resourceMapper) {
        return resourceSupplier.get()
                               .flatMap(resourceMapper)
                               .map(Derivable::derive);
    }

    private <T, R, D extends Derivable<T>> Mono<T> fetchMono(Supplier<Mono<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return resourceSupplier.get()
                               .map(resourceMapper)
                               .map(Derivable::derive);
    }

}
