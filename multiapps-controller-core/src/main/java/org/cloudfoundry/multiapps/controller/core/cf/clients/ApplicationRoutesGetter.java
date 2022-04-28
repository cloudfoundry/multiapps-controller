package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudRouteExtended;

public class ApplicationRoutesGetter extends CustomControllerClient {

    private static final int MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST = 4000;

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public ApplicationRoutesGetter(CloudControllerClient client) {
        super(client);
    }

    public List<CloudRouteExtended> getRoutes(String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> getRoutesInternal(appName));
    }

    private List<CloudRouteExtended> getRoutesInternal(String appName) {
        String appRoutesUrl = getAppRoutesUrl(client.getApplicationGuid(appName));
        var resourcesWithIncluded = getAllResourcesWithIncluded(appRoutesUrl);
        var batchedRouteGuids = getBatchedRouteGuids(resourcesWithIncluded.getResources());
        var serviceRouteBindingResources = getAllServiceRouteBindingResources(batchedRouteGuids);
        var serviceRouteBindingsByRouteGuid = groupByRouteGuid(serviceRouteBindingResources);

        return toCloudRoutes(resourcesWithIncluded, serviceRouteBindingsByRouteGuid);
    }

    private String getAppRoutesUrl(UUID appGuid) {
        return "/v3/routes?include=domain&app_guids=" + appGuid.toString();
    }

    private String getServiceRouteBindingsUrl(Collection<String> routeGuids) {
        return "/v3/service_route_bindings?route_guids=" + String.join(",", routeGuids);
    }

    private List<List<String>> getBatchedRouteGuids(List<Map<String, Object>> routeResources) {
        List<List<String>> batches = new ArrayList<>();
        int currentBatchLength = 0, currentBatchIndex = 0;
        batches.add(new ArrayList<>());

        for (Map<String, Object> routeResource : routeResources) {
            String routeGuid = (String) routeResource.get("guid");
            int elementLength = routeGuid.length();
            if (elementLength + currentBatchLength >= MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST) {
                batches.add(new ArrayList<>());
                currentBatchIndex++;
                currentBatchLength = 0;
            }
            batches.get(currentBatchIndex)
                   .add(routeGuid);
            currentBatchLength += elementLength;
        }
        return batches;
    }

    private List<Map<String, Object>> getAllServiceRouteBindingResources(List<List<String>> batchedRouteGuids) {
        return batchedRouteGuids.stream()
                                .map(this::getServiceRouteBindingsUrl)
                                .map(this::getAllResources)
                                .flatMap(List::stream)
                                .collect(Collectors.toList());
    }

    private Map<String, List<Map<String, Object>>> groupByRouteGuid(List<Map<String, Object>> serviceRouteBindings) {
        return serviceRouteBindings.stream()
                                   .collect(Collectors.groupingBy(
                                       serviceRouteBinding -> resourceMapper.getRelationshipData(serviceRouteBinding, "route")));
    }

    private List<CloudRouteExtended> toCloudRoutes(CloudResourcesWithIncluded resourcesWithIncluded,
                                                   Map<String, List<Map<String, Object>>> serviceRouteBindings) {
        List<CloudRouteExtended> result = new ArrayList<>();
        List<Map<String, Object>> routeResources = resourcesWithIncluded.getResources();
        @SuppressWarnings("unchecked")
        var domainResources = (List<Map<String, Object>>) resourcesWithIncluded.getIncludedResource("domains");

        for (var routeResource : routeResources) {
            String domainGuid = resourceMapper.getRelationshipData(routeResource, "domain");
            var domainResource = findDomainResource(domainResources, domainGuid);
            String routeGuid = (String) routeResource.get("guid");
            var serviceRouteBindingsForRoute = serviceRouteBindings.get(routeGuid);

            result.add(resourceMapper.mapRouteResource(routeResource, domainResource, serviceRouteBindingsForRoute));
        }
        return result;
    }

    private Map<String, Object> findDomainResource(List<Map<String, Object>> domainResources, String domainGuid) {
        return domainResources.stream()
                              .filter(domainResource -> domainGuid.equals(domainResource.get("guid")))
                              .findFirst()
                              .orElse(Collections.emptyMap());
    }

}
