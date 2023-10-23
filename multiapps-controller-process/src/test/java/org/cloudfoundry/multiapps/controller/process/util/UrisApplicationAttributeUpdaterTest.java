package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableRouteDestination;

class UrisApplicationAttributeUpdaterTest {

    private static final UUID APP_GUID = UUID.randomUUID();

    private ControllerClientFacade.Context context;
    @Mock
    private CloudControllerClient client;
    @Mock
    private ProcessContext processContext;
    @Mock
    private StepLogger logger;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        context = new ControllerClientFacade.Context(client, processContext, logger);
        when(client.getApplicationGuid(any())).thenReturn(APP_GUID);
    }

    static Stream<Arguments> shouldUpdateRoutes() {
        return Stream.of(Arguments.of(getExistingCloudRoutes(), Collections.emptyList(), true),
                         Arguments.of(getExistingCloudRoutes(), getExistingCloudRoutes(), false),
                         Arguments.of(getExistingCloudRoutes(), getNewCloudRoutesWithExistingRouteAndModifiedProtocol(), true));
    }

    @ParameterizedTest
    @MethodSource
    void shouldUpdateRoutes(List<CloudRoute> existingRoutes, List<CloudRoute> newRoutes, boolean result) {
        ApplicationAttributeUpdater applicationAttributeUpdater = new UrisApplicationAttributeUpdater(context,
                                                                                                      ElementUpdater.UpdateStrategy.REPLACE,
                                                                                                      existingRoutes);
        boolean shouldUpdateAttribute = applicationAttributeUpdater.shouldUpdateAttribute(buildCloudApplication(Collections.emptyList()),
                                                                                          buildCloudApplication(newRoutes));
        assertEquals(result, shouldUpdateAttribute);
    }

    private static List<CloudRoute> getExistingCloudRoutes() {
        CloudRoute existingHttp1Route = buildCloudRouteWithDestination("abc", "cfapps.sap.hana.ondemand.com", "http1");
        CloudRoute existingHtt2Route = buildCloudRouteWithDestination("abvd", "cfapps.sap.hana.ondemand.com", "http2");
        return List.of(existingHttp1Route, existingHtt2Route);
    }

    private static CloudRoute buildCloudRouteWithDestination(String host, String domain, String requestedProtocol) {
        return ImmutableCloudRoute.builder()
                                  .url(host + "." + domain)
                                  .host(host)
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(domain)
                                                              .build())
                                  .destinations(List.of(ImmutableRouteDestination.builder()
                                                                                 .applicationGuid(APP_GUID)
                                                                                 .protocol(requestedProtocol)
                                                                                 .build()))
                                  .build();
    }

    private static List<CloudRoute> getNewCloudRoutesWithExistingRouteAndModifiedProtocol() {
        CloudRoute newHttp1Route = buildCloudRouteRequestedProtocol("abc", "cfapps.sap.hana.ondemand.com", "http2");
        CloudRoute newHtt2Route = buildCloudRouteRequestedProtocol("abvd", "cfapps.sap.hana.ondemand.com", "http1");
        return List.of(newHttp1Route, newHtt2Route);
    }

    private static CloudRoute buildCloudRouteRequestedProtocol(String host, String domain, String requestedProtocol) {
        return ImmutableCloudRoute.builder()
                                  .url(host + "." + domain)
                                  .host(host)
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(domain)
                                                              .build())
                                  .requestedProtocol(requestedProtocol)
                                  .build();
    }

    private static CloudApplicationExtended buildCloudApplication(List<CloudRoute> cloudRoutes) {
        return ImmutableCloudApplicationExtended.builder()
                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                .guid(APP_GUID)
                                                                                .build())
                                                .routes(cloudRoutes)
                                                .build();
    }

}
