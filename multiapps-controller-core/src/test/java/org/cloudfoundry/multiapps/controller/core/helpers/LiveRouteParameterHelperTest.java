package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RoutesValidator;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

public class LiveRouteParameterHelperTest {

    private static final String MODULE_NAME = "test";
    private static final String ROUTE_NAME = "route_name";
    private static final String ROUTE_REFERENCE_PLACEHOLDER = "${routes/0/route}";
    private static LiveRouteParameterHelper liveRouteParameterHelper;
    private Map<String, Object> MODULE_PARAMETERS = buildRoutes();

    @BeforeEach
    void init() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        liveRouteParameterHelper = new LiveRouteParameterHelper();
    }

    @Test
    void testAddLiveRoute() {
        DeploymentDescriptor descriptor = liveRouteParameterHelper.addLiveRoutesWithReferenceToRoute(buildDeploymentDescriptor(buildModuleWithRoutes()));
        Module module = descriptor.getModules()
                                  .get(0);
        Object moduleRoutes = module.getParameters()
                                    .get(SupportedParameters.ROUTES);
        var moduleRoute = RoutesValidator.applyRoutesType(moduleRoutes)
                                         .get(0);

        assertEquals(ROUTE_REFERENCE_PLACEHOLDER, moduleRoute.get(SupportedParameters.LIVE_ROUTE));
    }

    @Test
    void testWithoutRoutes() {
        DeploymentDescriptor descriptor = liveRouteParameterHelper.addLiveRoutesWithReferenceToRoute(buildDeploymentDescriptor(buildModuleWithoutRoutes()));
        Module module = descriptor.getModules()
                                  .get(0);
        Object moduleRoutes = module.getParameters()
                                    .get(SupportedParameters.ROUTES);

        assertNull(moduleRoutes);
    }

    private DeploymentDescriptor buildDeploymentDescriptor(Module module) {
        return DeploymentDescriptor.createV2()
                                   .setModules(List.of(module));
    }

    private Module buildModuleWithRoutes() {
        return Module.createV2()
                     .setName(MODULE_NAME)
                     .setParameters(MODULE_PARAMETERS);
    }

    private Module buildModuleWithoutRoutes() {
        return Module.createV2()
                     .setName(MODULE_NAME);
    }

    private Map<String, Object> buildRoutes() {
        Map<String, Object> routes = new HashMap<>();
        List<Object> routeList = new ArrayList<>();
        Map<String, Object> route = new HashMap<>();

        route.put(SupportedParameters.ROUTE, ROUTE_NAME);
        routeList.add(route);
        routes.put(SupportedParameters.ROUTES, routeList);

        return routes;
    }
}
