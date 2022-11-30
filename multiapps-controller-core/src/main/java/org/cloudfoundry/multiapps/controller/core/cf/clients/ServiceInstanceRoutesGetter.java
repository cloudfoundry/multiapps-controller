package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableServiceRouteBinding;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceRouteBinding;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public class ServiceInstanceRoutesGetter extends CustomControllerClient {

    private static final int MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST = 4000;

    public ServiceInstanceRoutesGetter(CloudControllerClient client, String correlationId) {
        super(client, correlationId);
    }

    public List<ServiceRouteBinding> getServiceRouteBindings(Collection<String> routeGuids) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> doGetServiceRouteBindings(routeGuids));
    }

    private List<ServiceRouteBinding> doGetServiceRouteBindings(Collection<String> routeGuids) {
        var batchedRouteGuids = getBatchedRouteGuids(routeGuids);
        var responseMapper = new ServiceRouteBindingsResponseMapper();
        return batchedRouteGuids.stream()
                                .map(this::getServiceRouteBindingsUrl)
                                .map(url -> getListOfResources(responseMapper, url))
                                .flatMap(List::stream)
                                .collect(Collectors.toList());
    }

    private List<List<String>> getBatchedRouteGuids(Collection<String> routeGuids) {
        List<List<String>> batches = new ArrayList<>();
        int currentBatchLength = 0, currentBatchIndex = 0;
        batches.add(new ArrayList<>());

        for (String routeGuid : routeGuids) {
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

    private String getServiceRouteBindingsUrl(Collection<String> routeGuids) {
        return "/v3/service_route_bindings?route_guids=" + String.join(",", routeGuids);
    }

    protected static class ServiceRouteBindingsResponseMapper extends ResourcesResponseMapper<ServiceRouteBinding> {

        private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

        @Override
        public List<ServiceRouteBinding> getMappedResources() {
            return getQueriedResources().stream()
                                        .map(this::buildServiceRouteBinding)
                                        .collect(Collectors.toList());
        }

        private ServiceRouteBinding buildServiceRouteBinding(Map<String, Object> resource) {
            return ImmutableServiceRouteBinding.builder()
                                               .routeId(resourceMapper.getRelatedObjectGuid(resource, "route"))
                                               .serviceInstanceId(resourceMapper.getRelatedObjectGuid(resource, "service_instance"))
                                               .build();
        }
    }

}
