package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Droplet;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class OperationsUtil {

    private OperationsUtil() {
    }

    public static Map<String, Object> buildApplicationBody(ApplicationToCreateDto dto, String targetSpaceGuid) {
        Map<String, Object> body = new HashMap<>();
        body.put(V3Fields.NAME, dto.getName());
        body.put(V3Fields.LIFECYCLE, buildLifecycle(dto.getStaging()));
        body.put(V3Fields.RELATIONSHIPS,
                 java.util.Map.of(V3Fields.SPACE, java.util.Map.of(V3Fields.DATA, java.util.Map.of(V3Fields.GUID, targetSpaceGuid))));
        if (dto.getEnv() != null) {
            body.put(V3Fields.ENVIRONMENT_VARIABLES, dto.getEnv());
        }

        if (dto.getMetadata() != null) {
            body.put(V3Fields.METADATA, java.util.Map.of(V3Fields.LABELS, dto.getMetadata()
                                                                             .getLabels(),
                                                         V3Fields.ANNOTATIONS, dto.getMetadata()
                                                                                  .getAnnotations()));
        }

        return body;
    }

    private static Map<String, Object> buildLifecycle(Staging staging) {
        if (staging == null) {
            return java.util.Map.of(V3Fields.TYPE, "buildpack", V3Fields.DATA, java.util.Map.of());
        }

        if (staging.getDockerInfo() != null) {
            return Map.of(V3Fields.TYPE, "docker", V3Fields.DATA, Map.of());
        }

        String type = staging.getLifecycleType() != null ? staging.getLifecycleType()
                                                                  .name()
                                                                  .toLowerCase()
            : "buildpack";

        Map<String, Object> data = new HashMap<>();
        if (staging.getBuildpacks() != null) {
            data.put(V3Fields.BUILDPACKS, staging.getBuildpacks());
        }

        if (staging.getStackName() != null) {
            data.put(V3Fields.STACK, staging.getStackName());
        }

        return Map.of(V3Fields.TYPE, type, V3Fields.DATA, data);
    }

    public static DropletInfo parseDropletInfo(V3Droplet droplet) {
        String packageUrl = droplet.links()
                                   .get(Constants.PACKAGE_LINK)
                                   .href();

        if (packageUrl.endsWith("/")) {
            packageUrl = packageUrl.substring(0, packageUrl.lastIndexOf("/"));
        }

        String packageGuid = packageUrl.substring(packageUrl.lastIndexOf("/") + 1);
        return ImmutableDropletInfo.builder()
                                   .guid(UUID.fromString(droplet.guid()))
                                   .packageGuid(UUID.fromString(packageGuid))
                                   .build();
    }

    public static Map<String, Object> buildApplicationLifecycle(Staging staging) {
        if (staging.getDockerInfo() != null) {
            return Map.of(V3Fields.TYPE, "docker", V3Fields.DATA, Map.of());
        }

        String lifecycleType = staging.getLifecycleType() != null ? staging.getLifecycleType()
                                                                           .name()
                                                                           .toLowerCase()
            : "buildpack";

        if (Constants.CLOUD_NATIVE_BUILDPACK.equals(lifecycleType) && (staging.getBuildpacks() == null || staging.getBuildpacks()
                                                                                                                 .isEmpty())) {
            throw new IllegalArgumentException(Messages.BUILDPACKS_ARE_REQUIRED_FOR_THE_BUILDPACK_LIFECYCLE_TYPE);
        }

        Map<String, Object> data = new HashMap<>();
        if (staging.getStackName() != null) {
            data.put(V3Fields.STACK, staging.getStackName());
        }

        if (staging.getBuildpacks() != null) {
            data.put(V3Fields.BUILDPACKS, staging.getBuildpacks());
        }

        return Map.of(V3Fields.TYPE, lifecycleType, V3Fields.DATA, data);
    }

    private static Map<String, Object> buildHealthCheck(Staging staging) {
        Map<String, Object> data = new HashMap<>();
        MapUtil.addNonNull(data, V3Fields.ENDPOINT, staging.getHealthCheckHttpEndpoint());
        MapUtil.addNonNull(data, V3Fields.TIMEOUT, staging.getHealthCheckTimeout());
        MapUtil.addNonNull(data, V3Fields.INVOCATION_TIMEOUT, staging.getInvocationTimeout());
        MapUtil.addNonNull(data, V3Fields.INTERVAL, staging.getHealthCheckInterval());

        Map<String, Object> healthCheck = new HashMap<>();
        healthCheck.put(V3Fields.TYPE, staging.getHealthCheckType());
        healthCheck.put(V3Fields.DATA, data);
        return healthCheck;
    }

    private static Map<String, Object> buildReadinessHealthCheck(Staging staging) {
        Map<String, Object> data = new HashMap<>();
        MapUtil.addNonNull(data, V3Fields.INVOCATION_TIMEOUT, staging.getReadinessHealthCheckInvocationTimeout());
        MapUtil.addNonNull(data, V3Fields.ENDPOINT, staging.getReadinessHealthCheckHttpEndpoint());
        MapUtil.addNonNull(data, V3Fields.INTERVAL, staging.getReadinessHealthCheckInterval());

        Map<String, Object> readinessHealthCheck = new HashMap<>();
        readinessHealthCheck.put(V3Fields.TYPE, staging.getReadinessHealthCheckType());
        readinessHealthCheck.put(V3Fields.DATA, data);
        return readinessHealthCheck;
    }

    public static Map<String, Object> buildUpdatedProcessBody(Staging staging) {
        Map<String, Object> updatedProcessBody = new HashMap<>();
        updatedProcessBody.put(V3Fields.COMMAND, staging.getCommand());

        if (staging.getHealthCheckType() != null) {
            updatedProcessBody.put(V3Fields.HEALTH_CHECK, buildHealthCheck(staging));
        }

        if (staging.getReadinessHealthCheckType() != null) {
            updatedProcessBody.put(V3Fields.READINESS_HEALTH_CHECK, buildReadinessHealthCheck(staging));
        }

        return updatedProcessBody;
    }

    public static Map<String, Object> buildCreateRouteBody(UUID domainGuid, String host, String path, UUID targetSpaceGuid) {
        Map<String, Object> relationships = java.util.Map.of(V3Fields.DOMAIN, buildSingleResourceRelationship(domainGuid),
                                                             V3Fields.SPACE, buildSingleResourceRelationship(targetSpaceGuid));

        return Map.of(V3Fields.HOST, host == null ? "" : host,
                      V3Fields.PATH, path == null ? "" : path,
                      V3Fields.RELATIONSHIPS, relationships);
    }

    private static Map<String, Object> buildSingleResourceRelationship(UUID guid) {
        return Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, guid.toString()));
    }

    public static Map<String, Object> createDestination(UUID applicationGuid, String protocol) {
        if (protocol == null) {
            return Map.of(V3Fields.APP, Map.of(V3Fields.GUID, applicationGuid.toString()));
        }

        return Map.of(V3Fields.APP, Map.of(V3Fields.GUID, applicationGuid.toString()), V3Fields.PROTOCOL, protocol);
    }

    public static Set<CloudRoute> getOutdatedRoutes(UUID applicationGuid, List<CloudRoute> currentRoutes, Set<CloudRoute> updatedRoutes) {
        return currentRoutes.stream()
                            .filter(currentRoute -> isRouteOutdated(applicationGuid, currentRoute, updatedRoutes))
                            .collect(Collectors.toSet());
    }

    private static boolean isRouteOutdated(UUID applicationGuid, CloudRoute currentRoute, Set<CloudRoute> updatedRoutes) {
        Optional<CloudRoute> updatedRoute = findRoute(currentRoute.getUrl(), updatedRoutes);

        return updatedRoute.map(cloudRoute -> isProtocolChanged(applicationGuid, currentRoute, cloudRoute))
                           .orElse(true);

    }

    public static Set<CloudRoute> getNewRoutes(UUID applicationGuid, List<CloudRoute> currentRoutes, Set<CloudRoute> updatedRoutes) {
        return updatedRoutes.stream()
                            .filter(updatedRoute -> isRouteUpdated(applicationGuid, updatedRoute, currentRoutes))
                            .collect(Collectors.toSet());
    }

    private static boolean isRouteUpdated(UUID applicationGuid, CloudRoute updatedRoute, List<CloudRoute> currentRoutes) {
        Optional<CloudRoute> currentRoute = findRoute(updatedRoute.getUrl(), currentRoutes);

        return currentRoute.map(cloudRoute -> isProtocolChanged(applicationGuid, cloudRoute, updatedRoute))
                           .orElse(true);

    }

    private static Optional<CloudRoute> findRoute(String url, Collection<CloudRoute> routes) {
        return routes.stream()
                     .filter(route -> Objects.equals(url, route.getUrl()))
                     .findFirst();
    }

    private static boolean isProtocolChanged(UUID applicationGuid, CloudRoute currentRoute, CloudRoute updatedRoute) {
        if (updatedRoute.getRequestedProtocol() == null) {
            return false;
        }

        return currentRoute.getDestinations()
                           .stream()
                           .filter(routeDestination -> Objects.equals(routeDestination.getApplicationGuid(), applicationGuid))
                           .noneMatch(routeDestination -> Objects.equals(routeDestination.getProtocol(),
                                                                         updatedRoute.getRequestedProtocol()));
    }

    public static void validateDomainForRoute(CloudRoute route, Map<String, UUID> existingDomains) {
        String domain = route.getDomain()
                             .getName();

        if (!StringUtils.hasLength(domain) || !existingDomains.containsKey(domain)) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.DOMAIN_0_NOT_FOUND_FOR_URI_1, domain, route.getUrl()));
        }

    }

    public static Map<String, Object> buildServiceInstanceBody(String bindingName, UUID applicationGuid, UUID serviceInstanceGuid,
                                                               Map<String, Object> parameters) {
        Map<String, Object> body = new HashMap<>();
        body.put(V3Fields.NAME, bindingName);
        body.put(V3Fields.TYPE, "app");
        body.put(V3Fields.RELATIONSHIPS, Map.of(V3Fields.APP, toOneRelationship(applicationGuid), V3Fields.SERVICE_INSTANCE,
                                                toOneRelationship(serviceInstanceGuid)));

        if (!CollectionUtils.isEmpty(parameters)) {
            body.put(V3Fields.PARAMETERS, parameters);
        }

        return body;
    }

    private static Map<String, Object> toOneRelationship(UUID guid) {
        return Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, guid.toString()));
    }

    public static UUID getGuid(CloudEntity entity) {
        if (entity == null || entity.getMetadata() == null) {
            return null;
        }

        return entity.getMetadata()
                     .getGuid();
    }

    public static Map<String, Object> organizationRelationship(UUID organizationGuid) {
        return Map.of(V3Fields.ORGANIZATION, Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, organizationGuid.toString())));
    }

    public static boolean isUploadReady(Status status) {
        return status == Status.READY;
    }

    public static boolean hasUploadFailed(Status status) {
        return status == Status.EXPIRED || status == Status.FAILED;
    }

    public static void throwOnErrors(List<CloudOperationException> errors) {
        if (errors.isEmpty()) {
            return;
        }

        CloudOperationException firstException = errors.getFirst();
        errors.subList(1, errors.size())
              .forEach(firstException::addSuppressed);

        throw firstException;
    }

    public static Optional<String> extractJobGuid(ResponseEntity<Void> response) {
        URI location = response.getHeaders()
                               .getLocation();

        if (location == null) {
            return Optional.empty();
        }

        String path = location.getPath();
        return Optional.of(path.substring(path.lastIndexOf('/') + 1));
    }

    public static Map<String, Object> basicAuthentication(CloudServiceBroker serviceBroker) {
        return Map.of(V3Fields.TYPE, "basic", V3Fields.CREDENTIALS,
                      Map.of(V3Fields.USERNAME, nullToEmpty(serviceBroker.getUsername()), V3Fields.PASSWORD,
                             nullToEmpty(serviceBroker.getPassword())));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static Map<String, Object> buildCreateServiceBrokerBody(CloudServiceBroker serviceBroker) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> basicAuthentication = OperationsUtil.basicAuthentication(serviceBroker);

        body.put(V3Fields.NAME, serviceBroker.getName());
        body.put(V3Fields.URL, serviceBroker.getUrl());
        body.put(V3Fields.AUTHENTICATION, basicAuthentication);

        if (serviceBroker.getSpaceGuid() != null) {
            body.put(V3Fields.RELATIONSHIPS,
                     Map.of(V3Fields.SPACE, Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, serviceBroker.getSpaceGuid()))));
        }

        return body;
    }

    public static Map<String, Object> buildCreateServiceInstanceBody(CloudServiceInstance serviceInstance, UUID servicePlanGuid,
                                                                     CloudSpace targetSpace) {
        Map<String, Object> body = new HashMap<>();
        body.put(V3Fields.TYPE, "managed");
        body.put(V3Fields.NAME, serviceInstance.getName());
        MapUtil.addNonNull(body, V3Fields.METADATA, toMetadataMap(serviceInstance.getV3Metadata()));
        MapUtil.addNonNull(body, V3Fields.TAGS, serviceInstance.getTags());
        MapUtil.addNonNull(body, V3Fields.PARAMETERS, serviceInstance.getCredentials());
        body.put(V3Fields.RELATIONSHIPS, Map.of(V3Fields.SERVICE_PLAN, toOneRelationship(servicePlanGuid.toString()), V3Fields.SPACE,
                                                toOneRelationship(getGuid(targetSpace).toString())));

        return body;
    }

    public static Map<String, Object> toMetadataMap(Metadata metadata) {
        if (metadata == null) {
            return null;
        }

        Map<String, Object> metadataMapResult = new HashMap<>();
        metadataMapResult.put(V3Fields.LABELS, metadata.getLabels() == null ? Map.of() : metadata.getLabels());
        metadataMapResult.put(V3Fields.ANNOTATIONS, metadata.getAnnotations() == null ? Map.of() : metadata.getAnnotations());

        return metadataMapResult;
    }

    public static Map<String, Object> toOneRelationship(String guid) {
        return Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, guid));
    }

    public static Map<String, Object> buildCreateUserProvidedServiceInstance(CloudServiceInstance serviceInstance, String syslogDrainUrl,
                                                                             CloudSpace targetSpace) {
        Map<String, Object> body = new HashMap<>();
        body.put(V3Fields.TYPE, "user-provided");
        body.put(V3Fields.NAME, serviceInstance.getName());
        MapUtil.addNonNull(body, V3Fields.METADATA, toMetadataMap(serviceInstance.getV3Metadata()));
        MapUtil.addNonNull(body, V3Fields.CREDENTIALS, serviceInstance.getCredentials());
        body.put(V3Fields.SYSLOG_DRAIN_URL, syslogDrainUrl);
        MapUtil.addNonNull(body, V3Fields.TAGS, serviceInstance.getTags());
        body.put(V3Fields.RELATIONSHIPS, Map.of(V3Fields.SPACE, toOneRelationship(getGuid(targetSpace).toString())));
        return body;
    }

    public static boolean isUserProvided(V3ServiceInstance resource) {
        return Constants.USER_PROVIDED.equals(resource.type());
    }

    public static String servicePlanGuidOf(V3ServiceInstance resource) {
        if (resource.relationships() == null || resource.relationships()
                                                        .servicePlan() == null
            || resource.relationships()
                       .servicePlan()
                       .data() == null) {
            return null;
        }

        return resource.relationships()
                       .servicePlan()
                       .data()
                       .guid();
    }

    public static CloudOperationException determineNameResolutionError(CloudOperationException e, String forbiddenMessage,
                                                                       String notFoundMessage) {
        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new CloudOperationException(HttpStatus.FORBIDDEN, Messages.FORBIDDEN, forbiddenMessage, e);
        }

        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND, notFoundMessage, e);
        }

        return e;
    }

    public static String getServiceOfferingGuidFromServicePlan(V3ServicePlan plan) {
        if (plan.relationships() == null || plan.relationships()
                                                .serviceOffering() == null
            || plan.relationships()
                   .serviceOffering()
                   .data() == null) {
            return null;
        }

        return plan.relationships()
                   .serviceOffering()
                   .data()
                   .guid();
    }

    public static <T> List<List<T>> toBatches(Collection<T> largeList, int maxCharLength) {
        if (largeList.isEmpty()) {
            return List.of();
        }

        List<List<T>> batches = new ArrayList<>();
        int currentBatchLength = 0;
        int currentBatchIndex = 0;
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

    public static Map<String, Object> buildCreateServiceKeyBody(String name, Map<String, Object> parameters, Metadata metadata,
                                                                UUID serviceInstanceGuid) {
        Map<String, Object> resultBody = new HashMap<>();
        resultBody.put(V3Fields.TYPE, "key");
        resultBody.put(V3Fields.NAME, name);
        resultBody.put(V3Fields.RELATIONSHIPS,
                       Map.of(V3Fields.SERVICE_INSTANCE, Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, serviceInstanceGuid.toString()))));

        if (parameters != null && !parameters.isEmpty()) {
            resultBody.put(V3Fields.PARAMETERS, parameters);
        }

        MapUtil.addNonNull(resultBody, V3Fields.METADATA, toMetadataMap(metadata));

        return resultBody;
    }

    public static Map<String, Object> buildCreateTaskBody(CloudTask task) {
        Map<String, Object> body = new HashMap<>();
        body.put(V3Fields.COMMAND, task.getCommand());
        body.put(V3Fields.NAME, task.getName());

        CloudTask.Limits limits = task.getLimits();
        if (limits != null) {
            if (limits.getMemory() != null) {
                body.put(V3Fields.MEMORY_IN_MB, limits.getMemory());
            }

            if (limits.getDisk() != null) {
                body.put(V3Fields.DISK_IN_MB, limits.getDisk());
            }
        }

        return body;
    }

}
