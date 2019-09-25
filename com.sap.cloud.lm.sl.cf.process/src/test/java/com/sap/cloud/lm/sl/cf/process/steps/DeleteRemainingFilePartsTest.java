package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;

public class DeleteRemainingFilePartsTest extends SyncFlowableStepTest<DeleteRemainingFileParts> {

    private static final String DIGEST_ALGORITHM = "MD5";
    private static final String NAMESPACE = "namespace";
    private static final String DIGEST = "2252290BC44BEAD16AA1BF89948472E8";

    @BeforeEach
    public void setUp() {
        StepsUtil.setAsJsonBinaries(context, Constants.VAR_FILE_ENTRIES, createFakeFileEntries());
    }

    @Test
    public void testFileEntriesAreRemoved() throws FileStorageException {
        step.execute(context);
        verify(fileService, times(3)).deleteFile(any(String.class), any(String.class));
        assertStepFinishedSuccessfully();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThrowingException() throws FileStorageException {
        when(fileService.deleteFile(any(String.class), any(String.class))).thenThrow(FileStorageException.class);
        step.execute(context);
        assertStepFinishedSuccessfully();
    }

    private List<FileEntry> createFakeFileEntries() {
        return Arrays.asList(createFileEntry("id1", "file.part.0", "local"), createFileEntry("id2", "file.part.1", "local"),
                             createFileEntry("id3", "file.part.2", "local"));
    }

    private FileEntry createFileEntry(String id, String name, String space) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setId(id);
        fileEntry.setSpace(space);
        fileEntry.setName(name);
        fileEntry.setDigest(DIGEST_ALGORITHM);
        fileEntry.setModified(new Date());
        fileEntry.setNamespace(NAMESPACE);
        fileEntry.setSize(BigInteger.TEN);
        fileEntry.setDigest(DIGEST);
        return fileEntry;
    }

    @Override
    protected DeleteRemainingFileParts createStep() {
        return new DeleteRemainingFileParts();
    }

}
