package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.ExternalSqlFileQueryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FileServiceTest extends DatabaseFileServiceTest {

    @Mock
    private FileStorage fileStorage;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        super.setUp();
        Mockito.doAnswer(invocationOnMock -> IOUtils.consume((InputStream)invocationOnMock.getArgument(1)))
               .when(fileStorage)
               .addFile(Mockito.any(), Mockito.any());
    }

    @Test
    void addFileUploadFileErrorTest() throws Exception {
        Mockito.doThrow(new FileStorageException("expected exception"))
               .when(fileStorage)
               .addFile(Mockito.any(), Mockito.any());

        InputStream resourceStream = getResource(PIC_RESOURCE_NAME);
        String space = SPACE_1;
        String namespace = NAMESPACE_1;
        try {
            fileService.addFile(ImmutableFileEntry.builder()
                                                  .space(space)
                                                  .namespace(namespace)
                                                  .name(PIC_STORAGE_NAME)
                                                  .size(BigInteger.valueOf(PIC_SIZE))
                                                  .build(),
                                resourceStream);
            fail("addFile should fail with exception");
        } catch (FileStorageException e) {
            Mockito.verify(fileStorage)
                   .addFile(Mockito.any(), Mockito.any());
            List<FileEntry> listFiles = fileService.listFiles(space, namespace);
            assertEquals(0, listFiles.size());
        }
    }

    @Test
    void consumeFileContentTest() throws Exception {
        fileService.consumeFileContent(SPACE_1, "1111-2222-3333-4444", Mockito.mock(FileContentConsumer.class));
        Mockito.verify(fileStorage)
               .processFileContent(Mockito.eq(SPACE_1), Mockito.eq("1111-2222-3333-4444"), Mockito.any());
    }

    @Test
    void deleteBySpaceAndNamespaceTest() throws Exception {
        super.deleteBySpaceAndNamespaceTest();
        Mockito.verify(fileStorage)
               .deleteFilesBySpaceAndNamespace(Mockito.eq(SPACE_1), Mockito.eq(NAMESPACE_1));
    }

    @Test
    void deleteBySpaceAndNamespaceWithTwoNamespacesTest() throws Exception {
        super.deleteBySpaceAndNamespaceWithTwoNamespacesTest();
        Mockito.verify(fileStorage)
               .deleteFilesBySpaceAndNamespace(Mockito.eq(SPACE_1), Mockito.eq(NAMESPACE_1));
    }

    @Test
    void deleteBySpaceTest() throws Exception {
        super.deleteBySpaceTest();
        Mockito.verify(fileStorage)
               .deleteFilesBySpaceIds(Mockito.eq(List.of(SPACE_1)));
    }

    @Test
    void deleteFileTest() throws Exception {
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);

        boolean deleteFile = fileService.deleteFile(SPACE_2, fileEntry.getId());
        assertFalse(deleteFile);
        Mockito.verify(fileStorage)
               .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_2));

        deleteFile = fileService.deleteFile(SPACE_1, fileEntry.getId());
        assertTrue(deleteFile);
        Mockito.verify(fileStorage)
               .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_1));
    }

    @Test
    void deleteFileErrorTest() throws Exception {
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);

        Mockito.doThrow(new FileStorageException("expected exception"))
               .when(fileStorage)
               .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_1));

        try {
            fileService.deleteFile(SPACE_1, fileEntry.getId());
            fail("deleteFile should fail with an exception!");
        } catch (FileStorageException e) {
            Mockito.verify(fileStorage)
                   .deleteFile(Mockito.eq(fileEntry.getId()), Mockito.eq(SPACE_1));
            FileEntry fileStillExists = fileService.getFile(SPACE_1, fileEntry.getId());
            assertNotNull(fileStillExists);
        }
    }

    @Test
    void deleteByModificationTimeTest() throws Exception {
        super.deleteByModificationTimeTest();
        Mockito.verify(fileStorage)
               .deleteFilesModifiedBefore(Mockito.any());
    }

    @Test
    void deleteFilesEntriesWithoutContentTest() throws Exception {
        FileEntry noContent = addTestFile(SPACE_1, NAMESPACE_1);
        FileEntry noContent2 = addTestFile(SPACE_2, NAMESPACE_1);
        addTestFile(SPACE_1, NAMESPACE_2);
        addTestFile(SPACE_2, NAMESPACE_2);
        Mockito.when(fileStorage.getFileEntriesWithoutContent(Mockito.anyList()))
               .thenReturn(List.of(noContent, noContent2));
        int deleteWithoutContent = fileService.deleteFilesEntriesWithoutContent();
        assertEquals(2, deleteWithoutContent);
        assertNull(fileService.getFile(SPACE_1, noContent.getId()));
        assertNull(fileService.getFile(SPACE_2, noContent2.getId()));
    }

    @Test
    void deleteFilesByIdsTest() throws Exception {
        FileEntry fileEntry = addTestFile("foo", "bar");
        int deletedEntries = fileService.deleteFilesByIds(List.of(fileEntry.getId()));
        assertEquals(1, deletedEntries);
    }

    @Test
    void listFilesCreatedAfterAndBeforeWithoutOperationIdTest() throws Exception {
        addFile("foo", "bar", PIC_STORAGE_NAME, PIC_RESOURCE_NAME, null);
        List<FileEntry> fileEntries = fileService.listFilesCreatedAfterAndBeforeWithoutOperationId(LocalDateTime.now()
                                                                                                                .minusDays(1),
                                                                                                   LocalDateTime.now()
                                                                                                                .plusDays(1));
        assertEquals(1, fileEntries.size());
    }

    @Test
    void updateFilesOperationIdTest() throws Exception {
        FileEntry fileEntry = addFile("foo", "bar", PIC_STORAGE_NAME, PIC_RESOURCE_NAME, null);
        int updated = fileService.updateFilesOperationId(List.of(fileEntry.getId()), "123");
        FileEntry updatedFileEntry = fileService.getFile("foo", fileEntry.getId());
        assertEquals(1, updated);
        assertEquals("123", updatedFileEntry.getOperationId());
    }

    @Test
    void listFilesBySpaceAndOperationIdTest() throws Exception {
        addFile("foo", "bar", PIC_STORAGE_NAME, PIC_RESOURCE_NAME, "123");
        List<FileEntry> fileEntries = fileService.listFilesBySpaceAndOperationId("foo", "123");
        assertEquals(1, fileEntries.size());
    }

    @Override
    protected FileEntry addFile(String space, String namespace, String fileName, String resourceName, String operationId) throws Exception {
        FileEntry fileEntry = super.addFile(space, namespace, fileName, resourceName, operationId);
        FileEntry fileWithoutDigest = ImmutableFileEntry.copyOf(fileEntry)
                                                        .withDigest(null)
                                                        .withDigestAlgorithm(null);
        Mockito.verify(fileStorage)
               .addFile(Mockito.eq(fileWithoutDigest), Mockito.any());
        return fileEntry;
    }

    @Override
    protected FileService createFileService(DataSourceWithDialect dataSource) {
        ExternalSqlFileQueryProvider externalSqlFileQueryProvider = new ExternalSqlFileQueryProvider(FileService.DEFAULT_TABLE_NAME,
                                                                                                     dataSource.getDataSourceDialect()) {
            @Override
            protected String getSelectFilesWithoutOperationCreatedAfterTime1AndBeforeTime2Query() {
                return String.format("SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE MODIFIED > ? AND MODIFIED < ? AND OPERATION_ID IS NULL",
                                     FileService.DEFAULT_TABLE_NAME);
            }
        };
        return new FileService(dataSource, externalSqlFileQueryProvider, fileStorage);
    }

    @Override
    protected void verifyFileIsStored(FileEntry fileEntry) throws Exception {
        FileEntry fileWithoutDigest = ImmutableFileEntry.copyOf(fileEntry)
                                                        .withDigest(null)
                                                        .withDigestAlgorithm(null);
        Mockito.verify(fileStorage)
               .addFile(Mockito.eq(fileWithoutDigest), Mockito.any());
    }
}
