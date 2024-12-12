package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableBackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService.DescriptorBackupMapper;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

class DescriptorBackupServiceTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final LocalDateTime DATE_1 = LocalDateTime.parse("2024-12-05T13:30:25.010Z", DATE_TIME_FORMATTER);
    private static final LocalDateTime DATE_2 = LocalDateTime.parse("2020-11-30T13:30:25.020Z", DATE_TIME_FORMATTER);

    private static final BackupDescriptor BACKUP_DESCRIPTOR_1 = createBackupDescriptor(1, "space-1", "mta-1", "1.0.0", DATE_1, "checksum-1",
                                                                                       null);

    private static final BackupDescriptor BACKUP_DESCRIPTOR_2 = createBackupDescriptor(2, "space-1", "mta-2", "2.0.0", DATE_1, "checksum-2",
                                                                                       null);

    private static final BackupDescriptor BACKUP_DESCRIPTOR_3 = createBackupDescriptor(3, "sapce-2", "mta-2", "2.1.0", DATE_2, "checksum-3",
                                                                                       "dev");

    private final DescriptorBackupService descriptorBackupService = createDescriptorBackupService();

    @AfterEach
    void cleanup() {
        descriptorBackupService.createQuery()
                               .delete();
    }

    @Test
    void testAdd() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_1);

        List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                          .list();
        assertEquals(1, backupDescriptors.size());
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_1, backupDescriptors.get(0));
    }

    @Test
    void findById() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_2);
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_2, descriptorBackupService.createQuery()
                                                                                    .id(2L)
                                                                                    .singleResult());
    }

    @Test
    void findByMtaId() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_3);
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_3, descriptorBackupService.createQuery()
                                                                                    .mtaId("mta-2")
                                                                                    .singleResult());
    }

    @Test
    void findBySpaceId() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_1);
        descriptorBackupService.add(BACKUP_DESCRIPTOR_2);
        List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                          .spaceId("space-1")
                                                                          .list();

        assertEquals(2, backupDescriptors.size());
    }

    @Test
    void findByNamespace() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_3);
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_3, descriptorBackupService.createQuery()
                                                                                    .namespace("dev")
                                                                                    .singleResult());
    }

    @Test
    void findByChecksum() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_1);
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_1, descriptorBackupService.createQuery()
                                                                                    .checksum("checksum-1")
                                                                                    .singleResult());
    }

    @Test
    void findByChecksumsNotMatch() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_1);
        descriptorBackupService.add(BACKUP_DESCRIPTOR_2);
        descriptorBackupService.add(BACKUP_DESCRIPTOR_3);

        List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                          .checksumsNotMatch(List.of("checksum-1", "checksum-3"))
                                                                          .list();

        assertEquals(1, backupDescriptors.size());
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_2, backupDescriptors.get(0));
    }

    @Test
    void findByOlderThan() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_1);
        descriptorBackupService.add(BACKUP_DESCRIPTOR_2);
        descriptorBackupService.add(BACKUP_DESCRIPTOR_3);

        List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                          .olderThan(DATE_1)
                                                                          .list();
        assertEquals(1, backupDescriptors.size());
        verifyBackupDescriptorsAreEqual(BACKUP_DESCRIPTOR_3, backupDescriptors.get(0));
    }

    @Test
    void testThrowExceptionOnConflictingEntity() {
        descriptorBackupService.add(BACKUP_DESCRIPTOR_1);
        assertThrows(ConflictException.class, () -> descriptorBackupService.add(BACKUP_DESCRIPTOR_1));
    }

    @Test
    void testThrowExceptionOnEntityNotFound() {
        assertThrows(NotFoundException.class, () -> descriptorBackupService.update(BACKUP_DESCRIPTOR_2, BACKUP_DESCRIPTOR_3));
    }

    private DescriptorBackupService createDescriptorBackupService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        DescriptorBackupService descriptorBackupService = new DescriptorBackupService(entityManagerFactory);
        descriptorBackupService.descriptorBackupMapper = new DescriptorBackupMapper();
        return descriptorBackupService;
    }

    private static BackupDescriptor createBackupDescriptor(long id, String spaceId, String mtaId, String mtaVersion,
                                                           LocalDateTime timestamp, String checksum, String namespace) {
        return ImmutableBackupDescriptor.builder()
                                        .id(id)
                                        .descriptor(DeploymentDescriptor.createV3())
                                        .spaceId(spaceId)
                                        .mtaId(mtaId)
                                        .mtaVersion(mtaVersion)
                                        .timestamp(timestamp)
                                        .checksum(checksum)
                                        .namespace(namespace)
                                        .build();
    }

    private void verifyBackupDescriptorsAreEqual(BackupDescriptor expectedBackupDescriptor, BackupDescriptor backupDescriptor) {
        assertEquals(expectedBackupDescriptor.getId(), backupDescriptor.getId());
        assertEquals(expectedBackupDescriptor.getSpaceId(), backupDescriptor.getSpaceId());
        assertEquals(expectedBackupDescriptor.getMtaId(), backupDescriptor.getMtaId());
        assertEquals(expectedBackupDescriptor.getMtaVersion(), backupDescriptor.getMtaVersion());
        assertEquals(expectedBackupDescriptor.getChecksum(), backupDescriptor.getChecksum());
        assertEquals(expectedBackupDescriptor.getTimestamp(), backupDescriptor.getTimestamp());
    }

}
