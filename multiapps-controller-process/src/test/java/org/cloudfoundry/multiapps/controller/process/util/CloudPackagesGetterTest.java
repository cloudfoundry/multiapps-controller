package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDropletInfo;
import com.sap.cloudfoundry.client.facade.domain.Status;

class CloudPackagesGetterTest {

    private static final UUID APPLICATION_GUID = UUID.randomUUID();
    private static final UUID PACKAGE_GUID = UUID.randomUUID();
    private static final UUID DROPLET_GUID = UUID.randomUUID();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final CloudPackagesGetter cloudPackagesGetter = new CloudPackagesGetter();
    private final CloudControllerClient client = Mockito.mock(CloudControllerClient.class);

    @Test
    void getLatestUnusedPackageWithNoPackagesNoDroplet() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenThrow(getNotFoundCloudOperationException());
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertFalse(latestUnusedPackage.isPresent());
    }

    @Test
    void getLatestUnusedPackageExceptionIsThrown() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenThrow(getInternalServerErrorCloudOperationException());
        Exception exception = assertThrows(CloudOperationException.class,
                                           () -> cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID));
        assertEquals("500 Internal Server Error", exception.getMessage());
    }

    @Test
    void getLatestUnusedPackageWithOneValidPackageNoDroplet() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenThrow(getNotFoundCloudOperationException());
        Mockito.when(client.getPackagesForApplication(APPLICATION_GUID))
               .thenReturn(List.of(createCloudPackage(PACKAGE_GUID, Status.PROCESSING_UPLOAD, LocalDateTime.now())));
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertTrue(latestUnusedPackage.isPresent());
        assertEquals(PACKAGE_GUID, latestUnusedPackage.get()
                                                      .getGuid());
    }

    @Test
    void getLatestUnusedPackageWithDropletAndWithoutPackages() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenReturn(createDropletInfo(DROPLET_GUID, PACKAGE_GUID));
        Mockito.when(client.getPackage(PACKAGE_GUID))
               .thenThrow(getNotFoundCloudOperationException());
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertFalse(latestUnusedPackage.isPresent());
    }

    @Test
    void getLatestUnusedPackageWhenCurrentPackageIsTheSameAsNewestPackage() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenReturn(createDropletInfo(DROPLET_GUID, PACKAGE_GUID));
        CloudPackage cloudPackage = createCloudPackage(PACKAGE_GUID, Status.READY, LocalDateTime.now());
        Mockito.when(client.getPackage(PACKAGE_GUID))
               .thenReturn(cloudPackage);
        Mockito.when(client.getPackagesForApplication(APPLICATION_GUID))
               .thenReturn(List.of(cloudPackage));
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertFalse(latestUnusedPackage.isPresent());
    }

    @Test
    void getLatestUnusedPackageWhenNoPackagesAreFound() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenReturn(createDropletInfo(DROPLET_GUID, PACKAGE_GUID));
        CloudPackage cloudPackage = createCloudPackage(PACKAGE_GUID, Status.READY, LocalDateTime.now());
        Mockito.when(client.getPackage(PACKAGE_GUID))
               .thenReturn(cloudPackage);
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertFalse(latestUnusedPackage.isPresent());
    }

    @Test
    void getLatestUnusedPackageWhenThereIsNewerPackage() throws DateTimeParseException {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenReturn(createDropletInfo(DROPLET_GUID, PACKAGE_GUID));
        LocalDate date = LocalDate.parse("01-01-2020", DATE_TIME_FORMATTER);
        Mockito.when(client.getPackage(PACKAGE_GUID))
               .thenReturn(createCloudPackage(PACKAGE_GUID, Status.READY, LocalDateTime.of(date, LocalTime.NOON)));
        date = LocalDate.parse("02-01-2020", DATE_TIME_FORMATTER);
        CloudPackage olderCloudPackage = createCloudPackage(UUID.randomUUID(), Status.READY, LocalDateTime.of(date, LocalTime.NOON));
        date = LocalDate.parse("03-01-2020", DATE_TIME_FORMATTER);
        CloudPackage newestCloudPackage = createCloudPackage(UUID.randomUUID(), Status.READY, LocalDateTime.of(date, LocalTime.NOON));
        Mockito.when(client.getPackagesForApplication(APPLICATION_GUID))
               .thenReturn(List.of(olderCloudPackage, newestCloudPackage));
        Optional<CloudPackage> latestUnusedCloudPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertTrue(latestUnusedCloudPackage.isPresent());
        assertEquals(newestCloudPackage, latestUnusedCloudPackage.get());
    }

    private CloudOperationException getNotFoundCloudOperationException() {
        return new CloudControllerException(HttpStatus.NOT_FOUND);
    }

    private CloudOperationException getInternalServerErrorCloudOperationException() {
        return new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private CloudPackage createCloudPackage(UUID guid, Status status, LocalDateTime createdAt) {
        CloudMetadata cloudMetadata = ImmutableCloudMetadata.builder()
                                                            .guid(guid)
                                                            .createdAt(createdAt)
                                                            .build();
        return ImmutableCloudPackage.builder()
                                    .metadata(cloudMetadata)
                                    .status(status)
                                    .build();

    }

    private DropletInfo createDropletInfo(UUID guid, UUID packageGuid) {
        return ImmutableDropletInfo.builder()
                                   .guid(guid)
                                   .packageGuid(packageGuid)
                                   .build();
    }

}
