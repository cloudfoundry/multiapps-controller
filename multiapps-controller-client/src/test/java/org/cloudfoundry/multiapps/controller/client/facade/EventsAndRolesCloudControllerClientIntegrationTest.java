package org.cloudfoundry.multiapps.controller.client.facade;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventsAndRolesCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final String APP_NAME = "test-events-app";

    @BeforeAll
    static void setUp() throws InterruptedException {
        Staging staging = ImmutableStaging.builder()
                                          .addBuildpack(IntegrationTestConstants.STATICFILE_BUILDPACK)
                                          .build();

        ApplicationToCreateDto applicationDto = ImmutableApplicationToCreateDto.builder()
                                                                               .name(APP_NAME)
                                                                               .staging(staging)
                                                                               .diskQuotaInMb(IntegrationTestConstants.DISK_IN_MB)
                                                                               .memoryInMb(IntegrationTestConstants.MEMORY_IN_MB)
                                                                               .build();

        client.createApplication(applicationDto);
        CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, APP_NAME, getStaticfileApplicationContentPath());
        ApplicationUtil.stageApplication(client, APP_NAME, cloudPackage);
    }

    @AfterAll
    static void tearDown() {
        client.deleteApplication(APP_NAME);
    }

    @Test
    @DisplayName("Get all events returns a list of mapped audit events")
    void getEvents() {
        List<CloudEvent> events = client.getEvents();
        assertNotNull(events);
        assertAllEventsAreMapped(events);
    }

    @Test
    @DisplayName("Get events by actee returns the application's audit events")
    void getEventsByActee() {
        UUID appGuid = client.getApplicationGuid(APP_NAME);

        List<CloudEvent> events = client.getEventsByActee(appGuid);
        assertNotNull(events);
        assertAllEventsAreMapped(events);
    }

    @Test
    @DisplayName("Get application events returns the application's audit events")
    void getApplicationEvents() {
        List<CloudEvent> events = client.getApplicationEvents(APP_NAME);
        assertNotNull(events);
        assertAllEventsAreMapped(events);
    }

    @Test
    @DisplayName("Get user roles for an unknown user in the target space returns an empty set")
    void getUserRolesForUnknownUser() {
        String testUserGuidString = "00000000-1111-9999-8888-000000000000";
        UUID testUserGuid = UUID.fromString(testUserGuidString);
        UUID spaceGuid = target.getMetadata()
                               .getGuid();

        Set<UserRole> roles = client.getUserRolesBySpaceAndUser(spaceGuid, testUserGuid);
        assertNotNull(roles);
        assertTrue(roles.isEmpty(), "Expected no roles for an unknown test user in the space");
    }

    private void assertAllEventsAreMapped(List<CloudEvent> events) {
        for (CloudEvent event : events) {
            assertNotNull(event.getType(), "A mapped audit event must have a non-null type");
            assertNotNull(event.getGuid(), "A mapped audit event must have a non-null guid");
        }
    }

}
