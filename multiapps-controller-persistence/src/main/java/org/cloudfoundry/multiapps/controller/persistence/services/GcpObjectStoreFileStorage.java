package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRetryStrategy;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreFilter;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreMapper;
import org.springframework.http.MediaType;

public class GcpObjectStoreFileStorage implements FileStorage {

    private final String bucketName;
    private final Storage storage;
    private static final String BUCKET = "bucket";
    private static final int OBJECT_STORE_MAX_ATTEMPTS_CONFIG = 6;
    private static final double OBJECT_STORE_RETRY_DELAY_MULTIPLIER_CONFIG = 2.0;
    private static final Duration OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES = Duration.ofMinutes(10);
    private static final Duration OBJECT_STORE_MAX_RETRY_DELAY_CONFIG_IN_SECONDS = Duration.ofSeconds(10);
    private static final Duration OBJECT_STORE_INITIAL_RETRY_DELAY_CONFIG_IN_MILLIS = Duration.ofMillis(250);
    private static final String BASE_64_ENCODED_PRIVATE_KEY_DATA = "base64EncodedPrivateKeyData";

    public GcpObjectStoreFileStorage(Map<String, Object> credentials) {
        this.bucketName = (String) credentials.get(BUCKET);
        this.storage = createObjectStoreStorage(credentials);
    }

    protected Storage createObjectStoreStorage(Map<String, Object> credentials) {
        return StorageOptions.http()
                             .setCredentials(getGcpCredentialsSupplier(credentials))
                             .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy())
                             .setRetrySettings(
                                 RetrySettings.newBuilder()
                                              .setMaxAttempts(OBJECT_STORE_MAX_ATTEMPTS_CONFIG)
                                              .setTotalTimeoutDuration(OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES)
                                              .setMaxRetryDelayDuration(OBJECT_STORE_MAX_RETRY_DELAY_CONFIG_IN_SECONDS)
                                              .setInitialRetryDelayDuration(OBJECT_STORE_INITIAL_RETRY_DELAY_CONFIG_IN_MILLIS)
                                              .setRetryDelayMultiplier(OBJECT_STORE_RETRY_DELAY_MULTIPLIER_CONFIG)
                                              .build())
                             .build()
                             .getService();
    }

    private Credentials getGcpCredentialsSupplier(Map<String, Object> credentials) {
        if (!credentials.containsKey(BASE_64_ENCODED_PRIVATE_KEY_DATA)) {
            return null;
        }
        byte[] decodedKey = Base64.getDecoder()
                                  .decode((String) credentials.get(BASE_64_ENCODED_PRIVATE_KEY_DATA));
        try {
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decodedKey));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        BlobId blobId = BlobId.of(bucketName, fileEntry.getId());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                    .setContentDisposition(fileEntry.getName())
                                    .setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                    .setMetadata(ObjectStoreMapper.createFileEntryMetadata(fileEntry))
                                    .build();

        putBlob(blobInfo, content);
    }

    private void putBlob(BlobInfo blobInfo, InputStream content) throws FileStorageException {
        try {
            storage.createFrom(blobInfo, content);
        } catch (IOException | StorageException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) {
        Set<String> existingFiles = getAllEntries().stream()
                                                   .map(Blob::getName)
                                                   .collect(Collectors.toSet());
        return fileEntries.stream()
                          .filter(fileEntry -> !existingFiles.contains(fileEntry.getId()))
                          .toList();
    }

    @Override
    public void deleteFile(String id, String space) {
        deleteFileWithGeneration(id);
    }

    private boolean deleteFileWithGeneration(String id) {
        Blob blob = storage.get(bucketName, id);
        if (blob == null) {
            return false;
        }
        if (blob.getGeneration() == null) {
            return storage.delete(bucketName, id);
        }
        //Without generationMatch the delete requests are not retried because a retry can "accidentally delete a newer object version"
        return storage.delete(bucketName, id, Storage.BlobSourceOption.generationMatch(blob.getGeneration()));
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) {
        removeBlobsByFilter(blob -> ObjectStoreFilter.filterBySpaceIds(blob.getMetadata(), spaceIds));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        removeBlobsByFilter(blob -> ObjectStoreFilter.filterBySpaceAndNamespace(blob.getMetadata(), space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) {
        return removeBlobsByFilter(
            blob -> ObjectStoreFilter.filterByModificationTime(blob.getMetadata(), blob.getName(), modificationTime));
    }

    @Override
    public <T> T processFileContent(String space, String id,
                                    FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(space, id);
        try (InputStream inputStream = openBlobStream(fileEntry)) {
            return fileContentProcessor.process(inputStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private InputStream openBlobStream(FileEntry fileEntry) throws FileStorageException {
        Blob blob = getBlob(fileEntry);
        return Channels.newInputStream(blob.reader());
    }

    private Blob getBlob(FileEntry fileEntry) throws FileStorageException {
        try {
            Blob blob = storage.get(bucketName, fileEntry.getId());
            if (blob == null) {
                throw new FileStorageException(
                    MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST,
                                         fileEntry.getId(), fileEntry.getSpace()));
            }
            return blob;
        } catch (StorageException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(space, id);
        return openBlobStream(fileEntry);
    }

    @Override
    public void testConnection() {
        storage.get(bucketName, "test");
    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) {
        removeBlobsByFilter(blob -> fileIds.contains(blob.getName()));
    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(fileContentToProcess.getSpaceGuid(), fileContentToProcess.getGuid());
        InputStream blobPayload = getBlobPayloadWithOffset(fileEntry, fileContentToProcess.getStartOffset(),
                                                           fileContentToProcess.getEndOffset());
        return processContent(fileContentProcessor, blobPayload);
    }

    private <T> T processContent(FileContentProcessor<T> fileContentProcessor, InputStream inputStream)
        throws FileStorageException {
        try {
            return fileContentProcessor.process(inputStream);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    public Set<Blob> getAllEntries() {
        return storage.list(bucketName)
                      .streamAll()
                      .collect(Collectors.toSet());
    }

    private int removeBlobsByFilter(Predicate<? super Blob> filter) {
        List<BlobId> blobIds = getEntryNames(filter).stream()
                                                    .map(entry -> BlobId.of(bucketName, entry))
                                                    .toList();
        List<Boolean> deletedBlobsResults = new ArrayList<>();
        if (!blobIds.isEmpty()) {
            deletedBlobsResults.addAll(deleteBlobs(blobIds));
        }

        deletedBlobsResults.removeIf(Boolean.FALSE::equals);
        return deletedBlobsResults.size();
    }

    private List<Boolean> deleteBlobs(List<BlobId> blobIds) {
        return blobIds.stream()
                      .map(blobId -> deleteFileWithGeneration(blobId.getName()))
                      .toList();
    }

    protected Set<String> getEntryNames(Predicate<? super Blob> filter) {
        return storage.list(bucketName)
                      .streamAll()
                      .filter(filter)
                      .map(Blob::getName)
                      .collect(Collectors.toSet());
    }

    private InputStream getBlobPayloadWithOffset(FileEntry fileEntry, long startOffset, long endOffset)
        throws FileStorageException {
        try {
            Blob blob = getBlob(fileEntry);
            ReadChannel reader = storage.reader(blob.getBlobId());
            reader.seek(startOffset);
            reader.limit(endOffset + 1);

            return Channels.newInputStream(reader);
        } catch (IOException | StorageException e) {
            throw new FileStorageException(e);
        }
    }
}
