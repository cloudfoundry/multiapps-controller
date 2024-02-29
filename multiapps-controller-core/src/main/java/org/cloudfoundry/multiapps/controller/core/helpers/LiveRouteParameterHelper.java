package org.cloudfoundry.multiapps.controller.core.helpers;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Descriptor;
import org.cloudfoundry.multiapps.mta.model.Module;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LiveRouteParameterHelper {

    private static final String ROUTE_REFERENCE_PLACEHOLDER = "${routes/%s/route}";

    public DeploymentDescriptor addLiveRoutesWithReferenceToRoute(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules()) {
            addLiveRoutesToModuleRoutes(module);
        }
        return descriptor;
    }

    private void addLiveRoutesToModuleRoutes(Module module) {
        List<Map<String, Object>> routes = getModuleRoutes(module);
        routes.forEach(route -> addLiveRouteToRouteMap(routes, route));
    }

    private List<Map<String, Object>> getModuleRoutes(Module module) {
        return RoutesValidator.applyRoutesType(module.getParameters().get(SupportedParameters.ROUTES));
    }

    private void addLiveRouteToRouteMap(List<Map<String, Object>> routes, Map<String, Object> route) {
        Object liveRoute = route.get(SupportedParameters.LIVE_ROUTE);

        if (Objects.isNull(liveRoute)) {
            int routeIndex = routes.indexOf(route);

            route.put(SupportedParameters.LIVE_ROUTE, String.format(ROUTE_REFERENCE_PLACEHOLDER, routeIndex));
        }
    }
}
