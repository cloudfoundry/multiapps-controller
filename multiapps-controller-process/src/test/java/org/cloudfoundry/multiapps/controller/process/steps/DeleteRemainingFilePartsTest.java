package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteRemainingFilePartsTest extends SyncFlowableStepTest<DeleteRemainingFileParts> {

    private static final String DIGEST_ALGORITHM = "MD5";
    private static final String NAMESPACE = "namespace";
    private static final String DIGEST = "2252290BC44BEAD16AA1BF89948472E8";

    @BeforeEach
    void setUp() {
        context.setVariable(Variables.FILE_ENTRIES, createFakeFileEntries());
    }

    @Test
    void testFileEntriesAreRemoved() throws FileStorageException {
        step.execute(execution);
        verify(fileService, times(3)).deleteFile(any(String.class), any(String.class));
        assertStepFinishedSuccessfully();
    }

    @Test
    void testThrowingException() throws FileStorageException {
        when(fileService.deleteFile(any(String.class), any(String.class))).thenThrow(FileStorageException.class);
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    private List<FileEntry> createFakeFileEntries() {
        return List.of(createFileEntry("id1", "file.part.0", "local"), createFileEntry("id2", "file.part.1", "local"),
                       createFileEntry("id3", "file.part.2", "local"));
    }

    private FileEntry createFileEntry(String id, String name, String space) {
        return ImmutableFileEntry.builder()
                                 .id(id)
                                 .name(name)
                                 .namespace(NAMESPACE)
                                 .space(space)
                                 .digest(DIGEST)
                                 .digestAlgorithm(DIGEST_ALGORITHM)
                                 .size(BigInteger.TEN)
                                 .modified(LocalDateTime.now())
                                 .build();
    }

    @Override
    protected DeleteRemainingFileParts createStep() {
        return new DeleteRemainingFileParts();
    }

}
