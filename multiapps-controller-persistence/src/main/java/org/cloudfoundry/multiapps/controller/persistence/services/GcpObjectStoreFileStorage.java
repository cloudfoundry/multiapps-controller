package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class GcpObjectStoreFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpObjectStoreFileStorage.class);
    private final String bucketName;
    private final Storage storage;

    public GcpObjectStoreFileStorage(String bucketName, Storage storage) {
        this.bucketName = bucketName;
        this.storage = storage;

    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        String entryName = UUID.randomUUID()
                               .toString();
        LOGGER.error("Trying to create file");
        BlobId blobId = BlobId.of(bucketName, entryName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                    .setContentDisposition(fileEntry.getName())
                                    .setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                    .setMetadata(createFileEntryMetadata(fileEntry))
                                    .build();

        try {
            storage.create(blobInfo, content.readAllBytes());
            LOGGER.error("Created it");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> createFileEntryMetadata(FileEntry fileEntry) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(org.cloudfoundry.multiapps.controller.persistence.Constants.FILE_ENTRY_SPACE.toLowerCase(), fileEntry.getSpace());
        metadata.put(org.cloudfoundry.multiapps.controller.persistence.Constants.FILE_ENTRY_MODIFIED.toLowerCase(),
                     Long.toString(fileEntry.getModified()
                                            .atZone(
                                                ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli()));
        if (fileEntry.getNamespace() != null) {
            metadata.put(org.cloudfoundry.multiapps.controller.persistence.Constants.FILE_ENTRY_NAMESPACE.toLowerCase(),
                         fileEntry.getNamespace());
        }
        return metadata;
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) throws FileStorageException {
        Set<String> existingFiles = getAllEntries().stream()
                                                   .map(Blob::getName)
                                                   .collect(Collectors.toSet());
        return fileEntries.stream()
                          .filter(fileEntry -> !existingFiles.contains(fileEntry.getId()))
                          .toList();
    }

    @Override
    public void deleteFile(String id, String space) throws FileStorageException {
        storage.delete(id);
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) throws FileStorageException {

    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {

    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) throws FileStorageException {
        return 0;
    }

    @Override
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        return null;
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        return null;
    }

    @Override
    public void testConnection() {

    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) throws FileStorageException {

    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        return null;
    }

    public Set<Blob> getAllEntries() {
        Set<Blob> entries = new HashSet<>();
        Storage.BlobListOption[] opts = new Storage.BlobListOption[0];

        for (Blob b : storage.list(bucketName, opts)
                             .iterateAll()) {
            entries.add(b);
        }
        return entries;
    }
}
