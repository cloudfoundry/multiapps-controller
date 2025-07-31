package org.cloudfoundry.multiapps.controller.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.ClientV3Exception;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Pagination;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationResponse;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationResponse;
import org.cloudfoundry.client.v3.serviceofferings.GetServiceOfferingRequest;
import org.cloudfoundry.client.v3.serviceofferings.GetServiceOfferingResponse;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOffering;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingsV3;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanResponse;
import org.cloudfoundry.client.v3.serviceplans.ServicePlan;
import org.cloudfoundry.client.v3.serviceplans.ServicePlansV3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.RawCloudServiceOfferingTest;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.RawCloudServicePlanTest;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;

import reactor.core.publisher.Mono;

class CloudControllerRestClientImplTest {

    private static final String SERVICE_PLAN_GUID = "1803e5a7-40c7-438e-b2be-e2045c9b7cda";
    private static final String SERVICE_INSTANCE_GUID = "26949ebb-a624-35c0-000-1110a01f1880";
    private static final String SERVICE_OFFERING_GUID = "1803e5a7-40c7-438e-b2be-e2045c9b7cda";
    private static final String PLAN_NAME = "test-plan";
    private static final String OLD_APPLICATION_NAME = "old-app-name";
    private static final String NEW_APPLICATION_NAME = "new-app-name";
    private static final String APPLICATION_CREATED_AT = "2024-01-26T10:00:00";
    private static final UUID APPLICATION_GUID = UUID.randomUUID();
    private static final UUID SPACE_GUID = UUID.randomUUID();

    @Mock
    private CloudFoundryClient delegate;
    private CloudControllerRestClientImpl controllerClient;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        controllerClient = new CloudControllerRestClientImpl(delegate);
    }

    @Test
    void testGetServiceOffering() {
        GetServiceOfferingRequest request = GetServiceOfferingRequest.builder()
                                                                     .serviceOfferingId(SERVICE_PLAN_GUID)
                                                                     .build();
        GetServiceOfferingResponse response = GetServiceOfferingResponse.builder()
                                                                        .from(RawCloudServiceOfferingTest.buildTestServiceOffering())
                                                                        .build();

        ServiceOfferingsV3 serviceOfferingsV3 = Mockito.mock(ServiceOfferingsV3.class);
        Mockito.when(delegate.serviceOfferingsV3())
               .thenReturn(serviceOfferingsV3);
        Mockito.when(serviceOfferingsV3.get(request))
               .thenReturn(Mono.just(response));

        ServiceOffering serviceOffering = controllerClient.getServiceOffering(SERVICE_PLAN_GUID)
                                                          .block();

        assertEquals(response, serviceOffering);
    }

    public static Stream<Arguments> testGetServiceOfferingWithError() {
        return Stream.of(
// @formatter:off
                Arguments.of(HttpStatus.FORBIDDEN.value(),
                        "403 Forbidden: Service offering with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" is not available."),
                Arguments.of(HttpStatus.NOT_FOUND.value(),
                        "404 Not Found: Service offering with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" not found.")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetServiceOfferingWithError(int errorCode, String expectedErrorMessage) {
        GetServiceOfferingRequest request = GetServiceOfferingRequest.builder()
                                                                     .serviceOfferingId(SERVICE_OFFERING_GUID)
                                                                     .build();
        ServiceOfferingsV3 serviceOfferingsV3 = Mockito.mock(ServiceOfferingsV3.class);
        Mockito.when(delegate.serviceOfferingsV3())
               .thenReturn(serviceOfferingsV3);
        Mockito.when(serviceOfferingsV3.get(request))
               .thenReturn(Mono.error(clientV3Exception(errorCode)));
        Exception cloudControllerException = assertThrows(CloudOperationException.class,
                                                          () -> controllerClient.getServiceOffering(SERVICE_OFFERING_GUID)
                                                                                .block());
        assertEquals(expectedErrorMessage, cloudControllerException.getMessage());
    }

    public static Stream<Arguments> testGetServicePlanWithError() {
        return Stream.of(
// @formatter:off
                Arguments.of(HttpStatus.FORBIDDEN.value(),
                        "403 Forbidden: Service plan with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" is not available for service instance \"26949ebb-a624-35c0-000-1110a01f1880\"."),
                Arguments.of(HttpStatus.NOT_FOUND.value(),
                        "404 Not Found: Service plan with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" for service instance with name \"26949ebb-a624-35c0-000-1110a01f1880\" was not found.")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetServicePlanWithError(int errorCode, String expectedErrorMessage) {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(SERVICE_PLAN_GUID)
                                                             .build();

        ServicePlansV3 servicePlans = Mockito.mock(ServicePlansV3.class);
        Mockito.when(delegate.servicePlansV3())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.error(clientV3Exception(errorCode)));

        Exception cloudControllerException = assertThrows(CloudOperationException.class,
                                                          () -> controllerClient.getServicePlanResource(SERVICE_PLAN_GUID,
                                                                                                        SERVICE_INSTANCE_GUID)
                                                                                .block());
        assertEquals(expectedErrorMessage, cloudControllerException.getMessage());
    }

    @Test
    void testGetServicePlanResource() {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(SERVICE_PLAN_GUID)
                                                             .build();
        GetServicePlanResponse response = GetServicePlanResponse.builder()
                                                                .from(RawCloudServicePlanTest.buildTestServicePlan(PLAN_NAME))
                                                                .build();

        ServicePlansV3 servicePlans = Mockito.mock(ServicePlansV3.class);
        Mockito.when(delegate.servicePlansV3())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.just(response));

        ServicePlan servicePlanResource = controllerClient.getServicePlanResource(SERVICE_PLAN_GUID, SERVICE_INSTANCE_GUID)
                                                          .block();

        assertEquals(response, servicePlanResource);
    }

    private static Stream<Arguments> testRenameApplication() {
        return Stream.of(
                         // (1) Successful application rename
                         Arguments.of(Mono.just(UpdateApplicationResponse.builder()
                                                                         .name(NEW_APPLICATION_NAME)
                                                                         .id(APPLICATION_GUID.toString())
                                                                         .createdAt(APPLICATION_CREATED_AT)
                                                                         .state(ApplicationState.STARTED)
                                                                         .lifecycle(getApplicationLifecycle())
                                                                         .build()),
                                      null, false),
                         // (2) Controller's API returns 503 and rename was actually done
                         Arguments.of(Mono.error(new ClientV3Exception(503,
                                                                       List.of(org.cloudfoundry.client.v3.Error.builder()
                                                                                                               .code(170015)
                                                                                                               .title("CF-RunnerUnavailable")
                                                                                                               .detail("Runner is unavailable")
                                                                                                               .build()))),
                                      Mono.just(GetApplicationResponse.builder()
                                                                      .name(NEW_APPLICATION_NAME)
                                                                      .id(APPLICATION_GUID.toString())
                                                                      .createdAt(APPLICATION_CREATED_AT)
                                                                      .state(ApplicationState.STARTED)
                                                                      .lifecycle(getApplicationLifecycle())
                                                                      .build()),
                                      false),
                         // (3) Controller's API fails with 500 and application wasn't renamed
                         Arguments.of(Mono.error(new ClientV3Exception(500, Collections.emptyList())), null, true),
                         // (4) Controller's API returns 503 and while make request to get current application name the request fails
                         Arguments.of(Mono.error(new ClientV3Exception(503,
                                                                       List.of(org.cloudfoundry.client.v3.Error.builder()
                                                                                                               .code(170015)
                                                                                                               .title("CF-RunnerUnavailable")
                                                                                                               .detail("Runner is unavailable")
                                                                                                               .build()))),
                                      Mono.error(new ClientV3Exception(500, Collections.emptyList())), true),
                         // (5) Controller's API returns 503 and application wasn't renamed
                         Arguments.of(Mono.error(new ClientV3Exception(503,
                                                                       List.of(org.cloudfoundry.client.v3.Error.builder()
                                                                                                               .code(170015)
                                                                                                               .title("CF-RunnerUnavailable")
                                                                                                               .detail("Runner is unavailable")
                                                                                                               .build()))),
                                      Mono.just(GetApplicationResponse.builder()
                                                                      .name(OLD_APPLICATION_NAME)
                                                                      .id(APPLICATION_GUID.toString())
                                                                      .createdAt(APPLICATION_CREATED_AT)
                                                                      .state(ApplicationState.STARTED)
                                                                      .lifecycle(getApplicationLifecycle())
                                                                      .build()),
                                      true));
    }

    private static Lifecycle getApplicationLifecycle() {
        return Lifecycle.builder()
                        .type(LifecycleType.BUILDPACK)
                        .data(BuildpackData.builder()
                                           .build())
                        .build();
    }

    @ParameterizedTest
    @MethodSource
    void testRenameApplication(Mono<UpdateApplicationResponse> updateApplicationResponse,
                               Mono<GetApplicationResponse> getApplicationResponse, boolean expectedFailure) {
        ApplicationsV3 applicationsV3 = Mockito.mock(ApplicationsV3.class);
        var updateAppRequest = UpdateApplicationRequest.builder()
                                                       .applicationId(APPLICATION_GUID.toString())
                                                       .name(NEW_APPLICATION_NAME)
                                                       .build();
        prepareClientDelegate(applicationsV3, updateAppRequest, updateApplicationResponse, getApplicationResponse);

        if (expectedFailure) {
            assertThrows(ClientV3Exception.class, () -> controllerClient.rename(OLD_APPLICATION_NAME, NEW_APPLICATION_NAME));
            return;
        }
        controllerClient.rename(OLD_APPLICATION_NAME, NEW_APPLICATION_NAME);

        Mockito.verify(applicationsV3, Mockito.atMostOnce())
               .update(updateAppRequest);
    }

    private void prepareClientDelegate(ApplicationsV3 applicationsV3, UpdateApplicationRequest updateAppRequest,
                                       Mono<UpdateApplicationResponse> updateApplicationResponse,
                                       Mono<GetApplicationResponse> getApplicationResponse) {
        initControllerClientWithTargetSpace(SPACE_GUID);

        Mockito.when(delegate.applicationsV3())
               .thenReturn(applicationsV3);
        var listAppsRequest = ListApplicationsRequest.builder()
                                                     .spaceId(SPACE_GUID.toString())
                                                     .name(OLD_APPLICATION_NAME)
                                                     .page(1)
                                                     .build();

        var listAppsResponse = ListApplicationsResponse.builder()
                                                       .resource(ApplicationResource.builder()
                                                                                    .name(OLD_APPLICATION_NAME)
                                                                                    .id(APPLICATION_GUID.toString())
                                                                                    .createdAt(APPLICATION_CREATED_AT)
                                                                                    .state(ApplicationState.STARTED)
                                                                                    .lifecycle(getApplicationLifecycle())
                                                                                    .build())
                                                       .pagination(Pagination.builder()
                                                                             .totalResults(1)
                                                                             .totalPages(1)
                                                                             .build())
                                                       .build();
        Mockito.when(applicationsV3.list(listAppsRequest))
               .thenReturn(Mono.just(listAppsResponse));
        Mockito.when(applicationsV3.update(updateAppRequest))
               .thenReturn(updateApplicationResponse);
        var getAppRequest = GetApplicationRequest.builder()
                                                 .applicationId(APPLICATION_GUID.toString())
                                                 .build();

        Mockito.when(applicationsV3.get(getAppRequest))
               .thenReturn(getApplicationResponse);
    }

    private void initControllerClientWithTargetSpace(UUID spaceGuid) {
        CloudSpace target = ImmutableCloudSpace.builder()
                                               .metadata(ImmutableCloudMetadata.builder()
                                                                               .guid(spaceGuid)
                                                                               .build())
                                               .build();
        this.controllerClient = new CloudControllerRestClientImpl(delegate, target);
    }

    private ClientV3Exception clientV3Exception(int statusCode) {
        return new ClientV3Exception(statusCode, Collections.emptyList());
    }
}
