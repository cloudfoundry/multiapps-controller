package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.DropletInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage;
import org.cloudfoundry.client.lib.domain.ImmutableDropletInfo;
import org.cloudfoundry.client.lib.domain.Status;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class CloudPackagesGetterTest {

    private static final UUID APPLICATION_GUID = UUID.randomUUID();
    private static final UUID PACKAGE_GUID = UUID.randomUUID();
    private static final UUID DROPLET_GUID = UUID.randomUUID();
    private static final String DATE_FORMAT = "dd-MM-yyyy";
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
               .thenReturn(List.of(createCloudPackage(PACKAGE_GUID, Status.PROCESSING_UPLOAD, new Date())));
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
        CloudPackage cloudPackage = createCloudPackage(PACKAGE_GUID, Status.READY, new Date());
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
        Mockito.when(client.getPackage(PACKAGE_GUID))
               .thenReturn(createCloudPackage(PACKAGE_GUID, Status.READY, new Date(System.currentTimeMillis())));
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getLatestUnusedPackage(client, APPLICATION_GUID);
        assertFalse(latestUnusedPackage.isPresent());
    }

    @Test
    void getLatestUnusedPackageWhenThereIsNewerPackage() throws ParseException {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenReturn(createDropletInfo(DROPLET_GUID, PACKAGE_GUID));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        Mockito.when(client.getPackage(PACKAGE_GUID))
               .thenReturn(createCloudPackage(PACKAGE_GUID, Status.READY, simpleDateFormat.parse("01-01-2020")));
        CloudPackage olderCloudPackage = createCloudPackage(UUID.randomUUID(), Status.READY, simpleDateFormat.parse("02-01-2020"));
        CloudPackage newestCloudPackage = createCloudPackage(UUID.randomUUID(), Status.READY, simpleDateFormat.parse("03-01-2020"));
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

    private CloudPackage createCloudPackage(UUID guid, Status status, Date createdAt) {
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
