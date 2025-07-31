package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.v3.LastOperation;
import org.cloudfoundry.client.v3.MaintenanceInfo;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceRelationships;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;

class RawCloudServiceInstanceTest {

    private static final String SERVICE_INSTANCE_ID = "7cb26016-342d-11ed-a261-0242ac120002";
    private static final String SERVICE_INSTANCE_NAME = "v3-service-instance";
    private static final String DATE = "2022-01-01T13:35:11Z";
    private static final String DASHBOARD_URL = "dashboard-url";
    private static final String LAST_OPERATION_DESCRIPTION = "Last operation has succeeded";
    private static final String LAST_OPERATION_STATE = "succeeded";
    private static final String LAST_OPERATION_TYPE = "create";
    private static final String MAINTENANCE_INFO_DESCRIPTION = "A long time ago";
    private static final String MAINTENANCE_INFO_VERSION = "0.9.9";
    private static final Map<String, String> METADATA_ANNOTATIONS = Map.of("foo", "bar");
    private static final Map<String, String> METADATA_LABELS = Map.of("baz", "mop");
    private static final String SPACE_GUID = "5f5a8db4-342c-11ed-a261-0242ac120002";
    private static final String SERVICE_PLAN_GUID = "810a0322-342c-11ed-a261-0242ac120002";
    private static final String ROUTE_SERVICE_URL = "https://route-service-url";
    private static final String SYSLOG_DRAIN_URL = "https://syslog-drain";
    private static final List<String> TAGS = List.of("one", "two", "three");
    private static final ServiceInstanceType SERVICE_INSTANCE_TYPE = ServiceInstanceType.MANAGED;
    private static final boolean UPDATE_AVAILABLE = false;

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceInstance(), buildRawServiceInstance());
    }

    private CloudServiceInstance buildExpectedServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .createdAt(ZonedDateTime.parse(DATE,
                                                                                                           DateTimeFormatter.ISO_DATE_TIME)
                                                                                                    .toLocalDateTime())
                                                                            .updatedAt(ZonedDateTime.parse(DATE,
                                                                                                           DateTimeFormatter.ISO_DATE_TIME)
                                                                                                    .toLocalDateTime())
                                                                            .guid(UUID.fromString(SERVICE_INSTANCE_ID))
                                                                            .build())
                                            .name(SERVICE_INSTANCE_NAME)
                                            .v3Metadata(Metadata.builder()
                                                                .annotations(METADATA_ANNOTATIONS)
                                                                .labels(METADATA_LABELS)
                                                                .build())
                                            .tags(TAGS)
                                            .build();
    }

    private RawV3CloudServiceInstance buildRawServiceInstance() {
        return ImmutableRawV3CloudServiceInstance.builder()
                                                 .serviceInstance(buildServiceInstanceResource())
                                                 .build();
    }

    private ServiceInstanceResource buildServiceInstanceResource() {
        return ServiceInstanceResource.builder()
                                      .id(SERVICE_INSTANCE_ID)
                                      .name(SERVICE_INSTANCE_NAME)
                                      .createdAt(DATE)
                                      .updatedAt(DATE)
                                      .dashboardUrl(DASHBOARD_URL)
                                      .lastOperation(LastOperation.builder()
                                                                  .createdAt(DATE)
                                                                  .description(LAST_OPERATION_DESCRIPTION)
                                                                  .state(LAST_OPERATION_STATE)
                                                                  .type(LAST_OPERATION_TYPE)
                                                                  .updatedAt(DATE)
                                                                  .build())
                                      .maintenanceInfo(MaintenanceInfo.builder()
                                                                      .description(MAINTENANCE_INFO_DESCRIPTION)
                                                                      .version(MAINTENANCE_INFO_VERSION)
                                                                      .build())
                                      .metadata(Metadata.builder()
                                                        .annotations(METADATA_ANNOTATIONS)
                                                        .labels(METADATA_LABELS)
                                                        .build())
                                      .relationships(ServiceInstanceRelationships.builder()
                                                                                 .space(ToOneRelationship.builder()
                                                                                                         .data(Relationship.builder()
                                                                                                                           .id(SPACE_GUID)
                                                                                                                           .build())
                                                                                                         .build())
                                                                                 .servicePlan(ToOneRelationship.builder()
                                                                                                               .data(Relationship.builder()
                                                                                                                                 .id(SERVICE_PLAN_GUID)
                                                                                                                                 .build())
                                                                                                               .build())
                                                                                 .build())
                                      .routeServiceUrl(ROUTE_SERVICE_URL)
                                      .syslogDrainUrl(SYSLOG_DRAIN_URL)
                                      .tags(TAGS)
                                      .type(SERVICE_INSTANCE_TYPE)
                                      .updateAvailable(UPDATE_AVAILABLE)
                                      .build();
    }

}
