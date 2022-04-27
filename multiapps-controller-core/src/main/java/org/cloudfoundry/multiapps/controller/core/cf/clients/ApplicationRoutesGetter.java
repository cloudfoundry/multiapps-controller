package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudRouteExtended;

public class ApplicationRoutesGetter extends CustomControllerClient {

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
        String routeGuids = getRouteGuidsAsString(resourcesWithIncluded.getResources());
        String serviceRouteBindingsUrl = getServiceRouteBindingsUrl(routeGuids);
        var serviceRouteBindingResources = getAllResources(serviceRouteBindingsUrl);
        var serviceRouteBindingsByRouteGuid = groupByRouteGuid(serviceRouteBindingResources);

        return toCloudRoutes(resourcesWithIncluded, serviceRouteBindingsByRouteGuid);
    }

    private String getAppRoutesUrl(UUID appGuid) {
        return "/v3/routes?include=domain&app_guids=" + appGuid.toString();
    }

    private String getServiceRouteBindingsUrl(String routeGuids) {
        return "/v3/service_route_bindings?route_guids=" + routeGuids;
    }

    private String getRouteGuidsAsString(List<Map<String, Object>> routeResources) {
        return routeResources.stream()
                             .map(routeResource -> (String) routeResource.get("guid"))
                             .collect(Collectors.joining(","));
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
