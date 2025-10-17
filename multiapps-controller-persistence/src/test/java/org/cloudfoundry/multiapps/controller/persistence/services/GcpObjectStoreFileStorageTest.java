package org.cloudfoundry.multiapps.controller.persistence.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GcpObjectStoreFileStorageTest extends ObjectStoreFileStorageTest {

    private Storage storage;

    @Override
    @BeforeEach
    public void setUp() {
        storage = LocalStorageHelper.getOptions()
                                    .getService();
        fileStorage = new GcpObjectStoreFileStorage(CONTAINER, storage) {
            @Override
            protected long getRetryWaitTime() {
                return 0;
            }

            @Override
            protected int removeBlobsByFilter(Predicate<? super Blob> filter) {
                Set<String> entries = getEntryNames(filter);
                for (String entry : entries) {
                    storage.delete(CONTAINER, entry);
                }

                return entries.size();
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
    protected void assertBlobInNull(String blobWithNoMetadataId) {
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

}
