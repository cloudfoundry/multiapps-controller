package org.cloudfoundry.multiapps.controller.persistence.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GcpObjectStoreFileStorageTest extends JCloudsObjectStoreFileStorageTest {

    private Storage storage;
    private Storage mockedStorage;
    private GcpObjectStoreFileStorage mockedGcpFileStorage;

    @Override
    @BeforeEach
    public void setUp() {
        storage = LocalStorageHelper.getOptions()
                                    .getService();
        fileStorage = new GcpObjectStoreFileStorage(Map.of("bucket", CONTAINER)) {

            @Override
            protected Storage createObjectStoreStorage(Map<String, Object> credentials) {
                return storage;
            }
        };
        spaceId = UUID.randomUUID()
                      .toString();
        namespace = UUID.randomUUID()
                        .toString();
        mockedStorage = mock(Storage.class);
        mockedGcpFileStorage = new GcpObjectStoreFileStorage(Map.of("bucket", CONTAINER)) {
            @Override
            protected Storage createObjectStoreStorage(Map<String, Object> credentials) {
                return mockedStorage;
            }
        };
    }

    @Override
    @AfterEach
    public void tearDown() {
        if (storage != null) {
            try {
                storage.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void assertBlobDoesNotExist(String blobWithNoMetadataId) {
        assertNull(storage.get(blobWithNoMetadataId));
    }

    @Override
    public String addBlobWithNoMetadata() throws Exception {
        Path path = Paths.get(TEST_FILE_LOCATION);
        String id = UUID.randomUUID()
                        .toString();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(CONTAINER, id))
                                    .setContentDisposition(path.getFileName()
                                                               .toString())
                                    .setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                    .build();
        storage.create(blobInfo, Files.newInputStream(path));
        return id;
    }

    @Override
    public void assertFileExists(boolean exceptedFileExist, FileEntry actualFile) {
        Blob blob = storage.get(CONTAINER, actualFile.getId());
        boolean blobExists = blob != null;

        assertEquals(exceptedFileExist, blobExists);
    }

    @Override
    @Test
    void getExistingFileEntriesAllExist() {
        FileEntry firstEntry = createFileEntryWithRandomId();
        FileEntry secondEntry = createFileEntryWithRandomId();
        mockStorageGetToReturn(List.of(blobWithName(firstEntry.getId()), blobWithName(secondEntry.getId())));

        List<FileEntry> result = mockedGcpFileStorage.getExistingFileEntries(List.of(firstEntry, secondEntry));

        assertEquals(2, result.size());
        List<String> returnedIds = result.stream()
                                         .map(FileEntry::getId)
                                         .toList();
        assertTrue(returnedIds.contains(firstEntry.getId()));
        assertTrue(returnedIds.contains(secondEntry.getId()));
    }

    @Override
    @Test
    void getExistingFileEntriesNoneExist() {
        FileEntry firstEntry = createFileEntryWithRandomId();
        FileEntry secondEntry = createFileEntryWithRandomId();
        mockStorageGetToReturn(Arrays.asList(null, null));

        List<FileEntry> result = mockedGcpFileStorage.getExistingFileEntries(List.of(firstEntry, secondEntry));

        assertTrue(result.isEmpty());
    }

    @Override
    @Test
    void getExistingFileEntriesSomeExist() {
        FileEntry existingEntry = createFileEntryWithRandomId();
        FileEntry nonExistingEntry = createFileEntryWithRandomId();
        mockStorageGetToReturn(Arrays.asList(blobWithName(existingEntry.getId()), null));

        List<FileEntry> result = mockedGcpFileStorage.getExistingFileEntries(List.of(existingEntry, nonExistingEntry));

        assertEquals(1, result.size());
        assertEquals(existingEntry.getId(), result.getFirst()
                                                  .getId());
    }

    @Test
    void getExistingFileEntriesPassesCorrectBlobIdsToStorage() {
        FileEntry entry = createFileEntryWithRandomId();
        mockStorageGetToReturn(List.of());

        mockedGcpFileStorage.getExistingFileEntries(List.of(entry));

        verify(mockedStorage).get(List.of(BlobId.of(CONTAINER, entry.getId())));
    }

    private void mockStorageGetToReturn(List<Blob> blobs) {
        when(mockedStorage.get(anyList())).thenReturn(blobs);
    }

    private FileEntry createFileEntryWithRandomId() {
        return ImmutableFileEntry.builder()
                                 .id(UUID.randomUUID()
                                         .toString())
                                 .space(spaceId)
                                 .namespace(namespace)
                                 .build();
    }

    private Blob blobWithName(String name) {
        Blob blob = mock(Blob.class);
        when(blob.getName()).thenReturn(name);
        return blob;
    }

}
