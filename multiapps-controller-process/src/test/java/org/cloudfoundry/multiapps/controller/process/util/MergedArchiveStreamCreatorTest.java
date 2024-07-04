package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveStreamWithName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MergedArchiveStreamCreatorTest {

    @Mock
    private FileService fileService;
    @Mock
    private StepLogger stepLogger;

    private MergedArchiveStreamCreator mergedArchiveStreamCreator;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testArchiveStreamCreation() {
        List<FileEntry> archiveParts = buildMockedArchiveParts();
        mergedArchiveStreamCreator = new MergedArchiveStreamCreator(fileService, stepLogger, archiveParts, 30);
        ArchiveStreamWithName archiveStreamWithName = mergedArchiveStreamCreator.createArchiveStream();
        assertEquals("test-archive", archiveStreamWithName.getArchiveName());
    }

    @Test
    void testArchivePartsSorting() {
        List<FileEntry> archiveParts = buildMockedArchiveParts();
        mergedArchiveStreamCreator = new MergedArchiveStreamCreator(fileService, stepLogger, archiveParts, 30);
        List<FileEntry> sortedArchiveParts = mergedArchiveStreamCreator.getSortedArchiveParts();
        assertEquals(3, sortedArchiveParts.size());
        assertEquals("test-archive.part.1", sortedArchiveParts.get(0)
                                                              .getName());
        assertEquals("test-archive.part.2", sortedArchiveParts.get(1)
                                                              .getName());
        assertEquals("test-archive.part.3", sortedArchiveParts.get(2)
                                                              .getName());
    }

    @Test
    void testArchivePartsSortingWithInvalidPartName() {
        ImmutableFileEntry fileEntry1 = buildMockedArchivePart("test-archive.part.1");
        ImmutableFileEntry fileEntry2 = buildMockedArchivePart("test-archive.part.invalid.2");
        List<FileEntry> archiveParts = List.of(fileEntry1, fileEntry2);
        mergedArchiveStreamCreator = new MergedArchiveStreamCreator(fileService, stepLogger, archiveParts, 20);
        Exception exception = assertThrows(SLException.class, () -> mergedArchiveStreamCreator.getSortedArchiveParts());
        assertEquals("Invalid file entry name: \"test-archive.part.invalid.2\"", exception.getMessage());
    }

    private List<FileEntry> buildMockedArchiveParts() {
        FileEntry fileEntry1 = buildMockedArchivePart("test-archive.part.3");
        FileEntry fileEntry2 = buildMockedArchivePart("test-archive.part.2");
        FileEntry fileEntry3 = buildMockedArchivePart("test-archive.part.1");
        return List.of(fileEntry1, fileEntry2, fileEntry3);
    }

    private ImmutableFileEntry buildMockedArchivePart(String partName) {
        return ImmutableFileEntry.builder()
                                 .name(partName)
                                 .id(UUID.randomUUID()
                                         .toString())
                                 .size(BigInteger.valueOf(10))
                                 .build();
    }

}
