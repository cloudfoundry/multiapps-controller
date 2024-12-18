package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutablePreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService.DescriptorPreserverMapper;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DescriptorPreserverServiceTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final LocalDateTime DATE_1 = LocalDateTime.parse("2024-12-05T13:30:25.010Z", DATE_TIME_FORMATTER);
    private static final LocalDateTime DATE_2 = LocalDateTime.parse("2020-11-30T13:30:25.020Z", DATE_TIME_FORMATTER);

    private static final PreservedDescriptor PRESERVED_DESCRIPTOR_1 = createPreservedDescriptor(1, "space-1", "mta-1", "1.0.0", DATE_1,
                                                                                                "checksum-1", null);

    private static final PreservedDescriptor PRESERVED_DESCRIPTOR_2 = createPreservedDescriptor(2, "space-1", "mta-2", "2.0.0", DATE_1,
                                                                                                "checksum-2", null);

    private static final PreservedDescriptor PRESERVED_DESCRIPTOR_3 = createPreservedDescriptor(3, "sapce-2", "mta-2", "2.1.0", DATE_2,
                                                                                                "checksum-3", "dev");

    private final DescriptorPreserverService descriptorPreserverService = createDescriptorPreserverService();

    @AfterEach
    void cleanup() {
        descriptorPreserverService.createQuery()
                                  .delete();
    }

    @Test
    void testAdd() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1);

        List<PreservedDescriptor> preservedDescriptors = descriptorPreserverService.createQuery()
                                                                                   .list();
        assertEquals(1, preservedDescriptors.size());
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_1, preservedDescriptors.get(0));
    }

    @Test
    void findById() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_2);
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_2, descriptorPreserverService.createQuery()
                                                                                             .id(2L)
                                                                                             .singleResult());
    }

    @Test
    void findByMtaId() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_3);
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_3, descriptorPreserverService.createQuery()
                                                                                             .mtaId("mta-2")
                                                                                             .singleResult());
    }

    @Test
    void findBySpaceId() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1);
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_2);
        List<PreservedDescriptor> preservedDescriptors = descriptorPreserverService.createQuery()
                                                                                   .spaceId("space-1")
                                                                                   .list();

        assertEquals(2, preservedDescriptors.size());
    }

    @Test
    void findByNamespace() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_3);
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_3, descriptorPreserverService.createQuery()
                                                                                             .namespace("dev")
                                                                                             .singleResult());
    }

    @Test
    void findByChecksum() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1);
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_1, descriptorPreserverService.createQuery()
                                                                                             .checksum("checksum-1")
                                                                                             .singleResult());
    }

    @Test
    void findByChecksumsNotMatch() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1);
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_2);
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_3);

        List<PreservedDescriptor> preservedDescriptors = descriptorPreserverService.createQuery()
                                                                                   .checksumsNotMatch(List.of("checksum-1", "checksum-3"))
                                                                                   .list();

        assertEquals(1, preservedDescriptors.size());
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_2, preservedDescriptors.get(0));
    }

    @Test
    void findByOlderThan() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1);
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_2);
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_3);

        List<PreservedDescriptor> preservedDescriptors = descriptorPreserverService.createQuery()
                                                                                   .olderThan(DATE_1)
                                                                                   .list();
        assertEquals(1, preservedDescriptors.size());
        verifyPreservedDescriptorsAreEqual(PRESERVED_DESCRIPTOR_3, preservedDescriptors.get(0));
    }

    @Test
    void testThrowExceptionOnConflictingEntity() {
        descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1);
        assertThrows(ConflictException.class, () -> descriptorPreserverService.add(PRESERVED_DESCRIPTOR_1));
    }

    @Test
    void testThrowExceptionOnEntityNotFound() {
        assertThrows(NotFoundException.class, () -> descriptorPreserverService.update(PRESERVED_DESCRIPTOR_2, PRESERVED_DESCRIPTOR_3));
    }

    private DescriptorPreserverService createDescriptorPreserverService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        DescriptorPreserverService descriptorPreserverService = new DescriptorPreserverService(entityManagerFactory);
        descriptorPreserverService.descriptorPreserverMapper = new DescriptorPreserverMapper();
        return descriptorPreserverService;
    }

    private static PreservedDescriptor createPreservedDescriptor(long id, String spaceId, String mtaId, String mtaVersion,
                                                                 LocalDateTime timestamp, String checksum, String namespace) {
        return ImmutablePreservedDescriptor.builder()
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

    private void verifyPreservedDescriptorsAreEqual(PreservedDescriptor expectedPreservedDescriptor,
                                                    PreservedDescriptor preservedDescriptor) {
        assertEquals(expectedPreservedDescriptor.getId(), preservedDescriptor.getId());
        assertEquals(expectedPreservedDescriptor.getSpaceId(), preservedDescriptor.getSpaceId());
        assertEquals(expectedPreservedDescriptor.getMtaId(), preservedDescriptor.getMtaId());
        assertEquals(expectedPreservedDescriptor.getMtaVersion(), preservedDescriptor.getMtaVersion());
        assertEquals(expectedPreservedDescriptor.getChecksum(), preservedDescriptor.getChecksum());
        assertEquals(expectedPreservedDescriptor.getTimestamp(), preservedDescriptor.getTimestamp());
    }

}
