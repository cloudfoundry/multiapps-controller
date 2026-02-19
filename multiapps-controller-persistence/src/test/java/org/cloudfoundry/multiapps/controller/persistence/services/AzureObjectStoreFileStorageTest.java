package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobInputStream;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureObjectStoreFileStorageTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private PagedIterable pagedIterable;

    @Mock
    private FileContentProcessor fileContentProcessor;

    @Mock
    private BlobInputStream blobInputStream;

    private AzureObjectStoreFileStorage fileStorage;
    private InputStream inputStream = new ByteArrayInputStream(new byte[] {});
    private final String TEST_SPACE_ID = UUID.randomUUID()
                                             .toString();
    private final String TEST_SPACE_ID_2 = UUID.randomUUID()
                                               .toString();
    private final String TEST_ID = UUID.randomUUID()
                                       .toString();
    private final String TEST_ID_2 = UUID.randomUUID()
                                         .toString();

    private final String NAMESPACE = "namespace";
    private final String NAMESPACE_2 = "namespace_2";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        fileStorage = new AzureObjectStoreFileStorage(Map.of()) {

            @Override
            protected BlobContainerClient createContainerClient(Map<String, Object> credentials) {
                return blobContainerClient;
            }
        };

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
    }

    @Test
    void testAddFileWithSuccessfulUpload() throws FileStorageException {
        when(blobClient.uploadWithResponse(any(), any(), any())).thenReturn(null);
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        fileStorage.addFile(fileEntry, inputStream);

        verify(blobClient).uploadWithResponse(any(), any(), any());
    }

    @Test
    void testAddFileWithFailedUpload() {
        doThrow(BlobStorageException.class).when(blobClient)
                                           .uploadWithResponse(any(), any(), any());
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        assertThrows(FileStorageException.class, () -> fileStorage.addFile(fileEntry, inputStream));
    }

    @Test
    void testGetFileEntriesWithoutContentWithoutMatches() throws FileStorageException {
        setupDeleteMethods(createBlobItem(TEST_ID, TEST_SPACE_ID, NAMESPACE, LocalDateTime.now()),
                           createBlobItem(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE, LocalDateTime.now()));
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        List<FileEntry> fileEntries = fileStorage.getFileEntriesWithoutContent(List.of(fileEntry));

        assertEquals(0, fileEntries.size());
        verify(blobContainerClient).listBlobs();
    }

    @Test
    void testGetFileEntriesWithoutContent() throws FileStorageException {
        setupDeleteMethods(createSecondTestBlobItem());

        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);
        List<FileEntry> fileEntries = fileStorage.getFileEntriesWithoutContent(List.of(fileEntry));

        assertEquals(TEST_ID, fileEntries.get(0)
                                         .getId());
        assertEquals(TEST_SPACE_ID, fileEntries.get(0)
                                               .getSpace());
        assertEquals(1, fileEntries.size());
        verify(blobContainerClient).listBlobs();
    }

    @Test
    void testDeleteFile() throws FileStorageException {
        when(blobClient.deleteIfExists()).thenReturn(false);

        fileStorage.deleteFile(TEST_ID, TEST_SPACE_ID);
        verify(blobClient).deleteIfExists();
    }

    @Test
    void testTestConnection() {
        fileStorage.testConnection();
        verify(blobContainerClient).getBlobClient("test");
    }

    @Test
    void testGetContainerUriEndpointWithEmptyCredentials() {
        assertNull(fileStorage.getContainerUriEndpoint(Map.of()));
    }

    @Test
    void testGetContainerUriEndpointWithInvalidContainerUri() {
        assertThrows(IllegalStateException.class, () -> fileStorage.getContainerUriEndpoint(Map.of("container_uri", "")));
    }

    @Test
    void testGetContainerUriEndpointWithValidContainerUri() {
        assertEquals("https://google.com", fileStorage.getContainerUriEndpoint(Map.of("container_uri", "https://google.com")));
    }

    @Test
    void testDeleteFileWithException() {
        doThrow(new BlobStorageException("", null, null)).when(blobClient)
                                                         .deleteIfExists();

        assertThrows(FileStorageException.class, () -> fileStorage.deleteFile(TEST_ID, TEST_SPACE_ID));
        verify(blobClient).deleteIfExists();
    }

    @Test
    void testProcessFileContent() throws FileStorageException, IOException {
        when(blobClient.openInputStream()).thenReturn(blobInputStream);
        fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, fileContentProcessor);

        verify(fileContentProcessor).process(blobInputStream);
    }

    @Test
    void testProcessFileContentWithException() {
        doThrow(new BlobStorageException("", null, null)).when(blobClient)
                                                         .openInputStream();

        assertThrows(FileStorageException.class, () -> fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, fileContentProcessor));
    }

    @Test
    void testDeleteFilesBySpaceIdsWithAllMatchingItems() throws FileStorageException {
        setupDeleteMethods(createFirstTestBlobItem(), createSecondTestBlobItem());

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID, TEST_SPACE_ID_2));

        verify(blobClient, times(2)).deleteIfExists();
    }

    @Test
    void testDeleteFilesBySpaceIdsWithOneMatchingItem() throws FileStorageException {
        setupDeleteMethods(createFirstTestBlobItem(), createSecondTestBlobItem());

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID));

        verify(blobClient).deleteIfExists();
    }

    @Test
    void testDeleteFilesBySpaceIdsWithoutMatchingItem() throws FileStorageException {
        setupDeleteMethods();

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID, TEST_SPACE_ID_2));

        verify(blobClient, times(0)).deleteIfExists();
    }

    @Test
    void testDeleteFilesBySpaceAndNamespaceWithOneMatch() {
        setupDeleteMethods(createFirstTestBlobItem(), createSecondTestBlobItem());
        when(blobContainerClient.listBlobs()).thenReturn(pagedIterable);
        when(blobClient.deleteIfExists()).thenReturn(true);

        fileStorage.deleteFilesBySpaceAndNamespace(TEST_SPACE_ID, NAMESPACE);

        verify(blobClient, times(1)).deleteIfExists();
    }

    @Test
    void testDeleteFilesModifiedBefore() throws FileStorageException {
        long currentMillis = System.currentTimeMillis();
        long oldFilesTtl = 1000 * 60 * 10; // 10min
        setupDeleteMethods(createFirstTestBlobItem(), createSecondTestBlobItem());

        int deletedFiles = fileStorage.deleteFilesModifiedBefore(LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis - oldFilesTtl),
                                                                                         ZoneId.systemDefault()));

        assertEquals(2, deletedFiles);
    }

    @Test
    void testOpenInputStream() throws FileStorageException {
        when(blobClient.openInputStream()).thenReturn(null);

        fileStorage.openInputStream(TEST_SPACE_ID_2, TEST_ID);

        verify(blobContainerClient).getBlobClient(TEST_ID);
        verify(blobClient).openInputStream();
    }

    @Test
    void testOpenInputStreamWithException() {
        doThrow(new BlobStorageException(null, null, null)).when(blobClient)
                                                           .openInputStream();
        assertThrows(FileStorageException.class, () -> fileStorage.openInputStream(TEST_SPACE_ID_2, TEST_ID));

        verify(blobContainerClient).getBlobClient(TEST_ID);
        verify(blobClient).openInputStream();
    }

    @Test
    void testDeleteFilesByIds() throws FileStorageException {
        setupDeleteMethods(createFirstTestBlobItem(), createSecondTestBlobItem());

        fileStorage.deleteFilesByIds(List.of(TEST_ID));

        verify(blobClient).deleteIfExists();
    }

    private void setupDeleteMethods(BlobItem... blobItems) {
        when(pagedIterable.stream()).thenReturn(Stream.of(blobItems));
        when(blobContainerClient.listBlobs(any(), any())).thenReturn(pagedIterable);
        when(blobContainerClient.listBlobs()).thenReturn(pagedIterable);
        when(blobClient.deleteIfExists()).thenReturn(true);
    }

    public static FileEntry createFileEntry(String space, String id) {
        return ImmutableFileEntry.builder()
                                 .space(space)
                                 .size(BigInteger.TEN)
                                 .modified(LocalDateTime.now())
                                 .id(id)
                                 .build();
    }

    private BlobItem createFirstTestBlobItem() {
        long currentMillis = System.currentTimeMillis();
        long pastMoment = currentMillis - 1000 * 60 * 15; // before 15min
        return createBlobItem(TEST_ID, TEST_SPACE_ID, NAMESPACE,
                              LocalDateTime.ofInstant(Instant.ofEpochMilli(pastMoment), ZoneId.systemDefault()));
    }

    private BlobItem createSecondTestBlobItem() {
        long currentMillis = System.currentTimeMillis();
        long pastMoment = currentMillis - 1000 * 60 * 15; // before 15min
        return createBlobItem(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE_2,
                              LocalDateTime.ofInstant(Instant.ofEpochMilli(pastMoment), ZoneId.systemDefault()));
    }

    private BlobItem createBlobItem(String name, String spaceId, String namespace, LocalDateTime modificationTime) {
        BlobItem blobItem = new BlobItem();
        blobItem.setName(name);
        blobItem.setMetadata(Map.of("space", spaceId, "namespace", namespace, "modified", modificationTime.toString()));
        return blobItem;
    }
}
