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

            @Override
            public List<FileEntry> getExistingFileEntries(List<FileEntry> fileEntries) {
                return fileEntries.stream()
                                  .filter(fileEntry -> storage.get(CONTAINER, fileEntry.getId()) != null)
                                  .toList();
            }
        };
        spaceId = UUID.randomUUID()
                      .toString();
        namespace = UUID.randomUUID()
                        .toString();
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

    @Test
    void getExistingFileEntriesWhenAllEntriesExist() {
        Storage mockedStorage = mock(Storage.class);
        GcpObjectStoreFileStorage gcpFileStorage = gcpFileStorageWithMockedStorage(mockedStorage);
        FileEntry firstEntry = createFileEntryWithRandomId();
        FileEntry secondEntry = createFileEntryWithRandomId();
        Blob firstBlob = blobWithName(firstEntry.getId());
        Blob secondBlob = blobWithName(secondEntry.getId());
        when(mockedStorage.get(anyList())).thenReturn(List.of(firstBlob, secondBlob));

        List<FileEntry> result = gcpFileStorage.getExistingFileEntries(List.of(firstEntry, secondEntry));

        assertEquals(2, result.size());
        List<String> returnedIds = result.stream()
                                         .map(FileEntry::getId)
                                         .toList();
        assertTrue(returnedIds.contains(firstEntry.getId()));
        assertTrue(returnedIds.contains(secondEntry.getId()));
    }

    @Test
    void getExistingFileEntriesWhenNoEntriesExist() {
        Storage mockedStorage = mock(Storage.class);
        GcpObjectStoreFileStorage gcpFileStorage = gcpFileStorageWithMockedStorage(mockedStorage);
        FileEntry firstEntry = createFileEntryWithRandomId();
        FileEntry secondEntry = createFileEntryWithRandomId();
        when(mockedStorage.get(anyList())).thenReturn(Arrays.asList(null, null));

        List<FileEntry> result = gcpFileStorage.getExistingFileEntries(List.of(firstEntry, secondEntry));

        assertTrue(result.isEmpty());
    }

    @Test
    void getExistingFileEntriesWhenSomeEntriesExist() {
        Storage mockedStorage = mock(Storage.class);
        GcpObjectStoreFileStorage gcpFileStorage = gcpFileStorageWithMockedStorage(mockedStorage);
        FileEntry existingEntry = createFileEntryWithRandomId();
        FileEntry nonExistingEntry = createFileEntryWithRandomId();
        Blob existingBlob = blobWithName(existingEntry.getId());
        when(mockedStorage.get(anyList())).thenReturn(Arrays.asList(existingBlob, null));

        List<FileEntry> result = gcpFileStorage.getExistingFileEntries(List.of(existingEntry, nonExistingEntry));

        assertEquals(1, result.size());
        assertEquals(existingEntry.getId(), result.getFirst()
                                                  .getId());
    }

    @Test
    void getExistingFileEntriesPassesCorrectBlobIdsToStorage() {
        Storage mockedStorage = mock(Storage.class);
        GcpObjectStoreFileStorage gcpFileStorage = gcpFileStorageWithMockedStorage(mockedStorage);
        FileEntry entry = createFileEntryWithRandomId();
        when(mockedStorage.get(anyList())).thenReturn(List.of());

        gcpFileStorage.getExistingFileEntries(List.of(entry));

        verify(mockedStorage).get(List.of(BlobId.of(CONTAINER, entry.getId())));
    }

    private GcpObjectStoreFileStorage gcpFileStorageWithMockedStorage(Storage mockedStorage) {
        return new GcpObjectStoreFileStorage(Map.of("bucket", CONTAINER)) {
            @Override
            protected Storage createObjectStoreStorage(Map<String, Object> credentials) {
                return mockedStorage;
            }
        };
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
