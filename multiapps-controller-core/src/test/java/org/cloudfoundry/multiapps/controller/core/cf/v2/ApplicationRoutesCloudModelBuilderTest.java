package org.cloudfoundry.multiapps.controller.core.cf.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

class ApplicationRoutesCloudModelBuilderTest {

    @Mock
    private DeploymentDescriptor deploymentDescriptor;
    @Mock
    private CloudControllerClient client;
    @Mock
    private CloudApplicationExtended.AttributeUpdateStrategy applicationAttributeUpdateStrategy;
    private ApplicationRoutesCloudModelBuilder applicationRoutesCloudModelBuilder;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        applicationRoutesCloudModelBuilder = new ApplicationRoutesCloudModelBuilder(deploymentDescriptor,
                                                                                    client,
                                                                                    applicationAttributeUpdateStrategy);
    }

    @Test
    void testGetApplicationRoutes() {
        Map<String, Object> route = Map.of(SupportedParameters.ROUTE, "abc.cfapps.sap.hana.ondemand.com", SupportedParameters.PROTOCOL,
                                           "http2");
        List<Map<String, Object>> moduleParameters = List.of(Map.of(SupportedParameters.ROUTES, List.of(route)));
        Module module = Mockito.mock(Module.class);
        DeployedMtaApplication deployedMtaApplication = Mockito.mock(DeployedMtaApplication.class);
        List<CloudRoute> applicationRoutes = new ArrayList<>(applicationRoutesCloudModelBuilder.getApplicationRoutes(module,
                                                                                                                     moduleParameters,
                                                                                                                     deployedMtaApplication));
        assertEquals(1, applicationRoutes.size());
        assertEquals("abc.cfapps.sap.hana.ondemand.com", applicationRoutes.get(0)
                                                                          .getUrl());
        assertEquals("http2", applicationRoutes.get(0)
                                               .getRequestedProtocol());
    }

    @Test
    void testGetApplicationRoutesWhenKeepExistingIsTrue() {
        List<Map<String, Object>> moduleParameters = getModuleParameters();
        List<CloudRoute> existingRoutes = getExistingRoutes();
        when(client.getApplicationRoutes(any())).thenReturn(existingRoutes);
        Module module = Mockito.mock(Module.class);
        DeployedMtaApplication deployedMtaApplication = Mockito.mock(DeployedMtaApplication.class);
        Set<CloudRoute> applicationRoutes = applicationRoutesCloudModelBuilder.getApplicationRoutes(module, moduleParameters,
                                                                                                    deployedMtaApplication);
        assertRouteExists(applicationRoutes, "bbc.cfapps.sap.hana.ondemand.com", "http1");
        assertRouteExists(applicationRoutes, "abc.cfapps.sap.hana.ondemand.com", "http2");
        assertRouteExists(applicationRoutes, "ccc.cfapps.sap.hana.ondemand.com", "http2");
        assertRouteExists(applicationRoutes, "xxx.cfapps.sap.hana.ondemand.com", "http2");
    }

    private List<Map<String, Object>> getModuleParameters() {
        Map<String, Object> newHttp1Route = Map.of(SupportedParameters.ROUTE, "bbc.cfapps.sap.hana.ondemand.com",
                                                   SupportedParameters.PROTOCOL, "http1");
        Map<String, Object> newHttp2Route = Map.of(SupportedParameters.ROUTE, "abc.cfapps.sap.hana.ondemand.com",
                                                   SupportedParameters.PROTOCOL, "http2");
        Map<String, Object> newRouteWithoutProtocol = Map.of(SupportedParameters.ROUTE, "ccc.cfapps.sap.hana.ondemand.com");
        return List.of(Map.of(SupportedParameters.ROUTES, List.of(newHttp1Route, newHttp2Route, newRouteWithoutProtocol),
                              SupportedParameters.KEEP_EXISTING_ROUTES, true));
    }

    private List<CloudRoute> getExistingRoutes() {
        var existingHttp2RouteAlreadyDefined = buildCloudRoute("ccc", "cfapps.sap.hana.ondemand.com", "http2");
        var existingHttp2RouteNotDefined = buildCloudRoute("xxx", "cfapps.sap.hana.ondemand.com", "http2");
        var existingHttp2RouteOverridden = buildCloudRoute("bbc", "cfapps.sap.hana.ondemand.com", "http1");
        return List.of(existingHttp2RouteAlreadyDefined, existingHttp2RouteNotDefined, existingHttp2RouteOverridden);
    }

    private CloudRoute buildCloudRoute(String host, String domain, String protocol) {
        return ImmutableCloudRoute.builder()
                                  .url(host + "." + domain)
                                  .host(host)
                                  .requestedProtocol(protocol)
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(domain)
                                                              .build())
                                  .build();
    }

    private void assertRouteExists(Set<CloudRoute> routes, String url, String protocol) {
        boolean routeExists = routes.stream()
                                    .anyMatch(route -> route.getUrl()
                                                            .equals(url)
                                        && route.getRequestedProtocol()
                                                .equals(protocol));
        if (!routeExists) {
            fail(MessageFormat.format("Route with URL: {0} and protocol {1} does not exists", url, protocol));
        }
    }

}
