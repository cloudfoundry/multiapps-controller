package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableServiceRouteBinding;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceRouteBinding;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

public class ServiceInstanceRoutesGetter extends CustomControllerClient {

    private static final String SERVICE_ROUTE_BINDINGS_URI_PREFIX = "/v3/service_route_bindings?";
    private static final String ROUTE_GUIDS_PARAM_PREFIX = "route_guids=";

    public ServiceInstanceRoutesGetter(ApplicationConfiguration configuration, WebClientFactory webClientFactory,
                                       CloudCredentials credentials, String correlationId) {
        super(configuration, webClientFactory, credentials, correlationId);
    }

    public List<ServiceRouteBinding> getServiceRouteBindings(List<String> routeGuids) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> doGetServiceRouteBindings(routeGuids));
    }

    private List<ServiceRouteBinding> doGetServiceRouteBindings(List<String> routeGuids) {
        return getListOfResourcesInBatches(new ServiceRouteBindingsResponseMapper(),
                                           SERVICE_ROUTE_BINDINGS_URI_PREFIX,
                                           ROUTE_GUIDS_PARAM_PREFIX,
                                           routeGuids);
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
