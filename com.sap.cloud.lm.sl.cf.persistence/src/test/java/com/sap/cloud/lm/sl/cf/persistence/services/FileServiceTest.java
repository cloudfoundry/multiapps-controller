package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableFileEntry;

public class FileServiceTest extends DatabaseFileServiceTest {

    @Mock
    private FileStorage fileStorage;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        super.setUp();
        Mockito.doAnswer(consumeStream())
               .when(fileStorage)
               .addFile(Mockito.any(), Mockito.any());

    }

    private Answer<Void> consumeStream() {
        return invocation -> {
            InputStream stream = invocation.getArgument(1);
            IOUtils.copy(stream, new NullOutputStream());
            return null;
        };
    }

    @Test
    public void addFileUploadFileErrorTest() throws Exception {
        Mockito.doThrow(new FileStorageException("expected exception"))
               .when(fileStorage)
               .addFile(Mockito.any(), Mockito.any());

        InputStream resourceStream = getResource(PIC_RESOURCE_NAME);
        String space = SPACE_1;
        String namespace = NAMESPACE_1;
        try {
            fileService.addFile(space, namespace, PIC_STORAGE_NAME, resourceStream);
            fail("addFile should fail with exception");
        } catch (FileStorageException e) {
            Mockito.verify(fileStorage, Mockito.times(1))
                   .addFile(Mockito.any(), Mockito.any());
            List<FileEntry> listFiles = fileService.listFiles(space, namespace);
            assertEquals(0, listFiles.size());
        }
    }

    @Test
    public void processFileContentTest() throws Exception {
        FileContentProcessor fileContentProcessor = Mockito.mock(FileContentProcessor.class);
        fileService.processFileContent(SPACE_1, "1111-2222-3333-4444", fileContentProcessor);
        Mockito.verify(fileStorage, Mockito.times(1))
               .processFileContent(SPACE_1, "1111-2222-3333-4444", fileContentProcessor);
    }

    @Test
    public void deleteBySpaceAndNamespaceTest() throws Exception {
        super.deleteBySpaceAndNamespaceTest();
        Mockito.verify(fileStorage, Mockito.times(1))
               .deleteFilesBySpaceAndNamespace(Mockito.eq(SPACE_1), Mockito.eq(NAMESPACE_1));
    }

    @Test
    public void deleteBySpaceAndNamespaceWithTwoNamespacesTest() throws Exception {
        super.deleteBySpaceAndNamespaceWithTwoNamespacesTest();
        Mockito.verify(fileStorage, Mockito.times(1))
               .deleteFilesBySpaceAndNamespace(Mockito.eq(SPACE_1), Mockito.eq(NAMESPACE_1));
    }

    @Test
    public void deleteBySpaceTest() throws Exception {
        super.deleteBySpaceTest();
        Mockito.verify(fileStorage, Mockito.times(1))
               .deleteFilesBySpace(Mockito.eq(SPACE_1));
    }

    @Test
    public void deleteFileTest() throws Exception {
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);

        boolean deleteFile = fileService.deleteFile(SPACE_2, fileEntry.getId());
        assertFalse(deleteFile);
        Mockito.verify(fileStorage, Mockito.times(1))
               .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_2));

        deleteFile = fileService.deleteFile(SPACE_1, fileEntry.getId());
        assertTrue(deleteFile);
        Mockito.verify(fileStorage, Mockito.times(1))
               .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_1));
    }

    @Test
    public void deleteFileErrorTest() throws Exception {
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);

        Mockito.doThrow(new FileStorageException("expected exception"))
               .when(fileStorage)
               .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_1));

        try {
            fileService.deleteFile(SPACE_1, fileEntry.getId());
            fail("deleteFile should fail with an exception!");
        } catch (FileStorageException e) {
            Mockito.verify(fileStorage, Mockito.times(1))
                   .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_1));
            FileEntry fileStillExists = fileService.getFile(SPACE_1, fileEntry.getId());
            assertNotNull(fileStillExists);
        }
    }

    @Test
    public void deleteByModificationTimeTest() throws Exception {
        super.deleteByModificationTimeTest();
        Mockito.verify(fileStorage, Mockito.times(1))
               .deleteFilesModifiedBefore(Mockito.any());
    }

    @Test
    public void deleteFilesEntriesWithoutContentTest() throws Exception {
        FileEntry noContent = addTestFile(SPACE_1, NAMESPACE_1);
        FileEntry noContent2 = addTestFile(SPACE_2, NAMESPACE_1);
        addTestFile(SPACE_1, NAMESPACE_2);
        addTestFile(SPACE_2, NAMESPACE_2);
        Mockito.when(fileStorage.getFileEntriesWithoutContent(Mockito.anyList()))
               .thenReturn(Arrays.asList(noContent, noContent2));
        int deleteWithoutContent = fileService.deleteFilesEntriesWithoutContent();
        assertEquals(2, deleteWithoutContent);
        assertNull(fileService.getFile(SPACE_1, noContent.getId()));
        assertNull(fileService.getFile(SPACE_2, noContent2.getId()));
    }

    @Override
    protected FileEntry addFile(String space, String namespace, String fileName, String resourceName) throws Exception {
        FileEntry fileEntry = super.addFile(space, namespace, fileName, resourceName);
        FileEntry expectedFileEntry = removeSizeAndDigest(fileEntry);
        Mockito.verify(fileStorage)
               .addFile(Mockito.eq(expectedFileEntry), Mockito.any());
        return fileEntry;
    }

    private ImmutableFileEntry removeSizeAndDigest(FileEntry fileEntry) {
        return ImmutableFileEntry.builder()
                                 .from(fileEntry)
                                 .size(null)
                                 .digest(null)
                                 .digestAlgorithm(null)
                                 .build();
    }

    @Override
    protected FileService createFileService(DataSourceWithDialect dataSource) {
        return new FileService(dataSource, fileStorage);
    }

    @Override
    protected void verifyFileIsStored(FileEntry fileEntry) throws Exception {
        Mockito.verify(fileStorage)
               .addFile(Mockito.eq(removeSizeAndDigest(fileEntry)), Mockito.any());
    }
}
