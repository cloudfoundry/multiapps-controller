package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
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
    private final CloudPackagesGetter cloudPackagesGetter = new CloudPackagesGetter();
    private final CloudControllerClient client = Mockito.mock(CloudControllerClient.class);

    @Test
    void getAppPackageWithNoPackagesNoDroplet() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenThrow(getNotFoundCloudOperationException());
        Optional<CloudPackage> latestUnusedPackage = cloudPackagesGetter.getAppPackage(client, APPLICATION_GUID);
        assertFalse(latestUnusedPackage.isPresent());
    }

    @Test
    void getAppPackageExceptionIsThrown() {
        Mockito.when(client.getCurrentDropletForApplication(APPLICATION_GUID))
               .thenThrow(getInternalServerErrorCloudOperationException());
        Exception exception = assertThrows(CloudOperationException.class,
                                           () -> cloudPackagesGetter.getAppPackage(client, APPLICATION_GUID));
        assertEquals("500 Internal Server Error", exception.getMessage());
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
        Optional<CloudPackage> currentPackage = cloudPackagesGetter.getAppPackage(client, APPLICATION_GUID);
        Optional<CloudPackage> latestPackage = cloudPackagesGetter.getMostRecentAppPackage(client, APPLICATION_GUID);
        assertTrue(currentPackage.isPresent());
        assertTrue(latestPackage.isPresent());
        assertEquals(currentPackage.get()
                                   .getGuid(), latestPackage.get()
                                                            .getGuid());
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
