package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class GcpObjectStoreFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpObjectStoreFileStorage.class);
    private final String bucketName;
    private final Storage storage;
    private static final long RETRY_WAIT_TIME = 5000L;

    public GcpObjectStoreFileStorage(String bucketName, Storage storage) {
        this.bucketName = bucketName;
        this.storage = storage;
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        BlobId blobId = BlobId.of(bucketName, fileEntry.getId());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                    .setContentDisposition(fileEntry.getName())
                                    .setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                    .setMetadata(ObjectStoreUtil.createFileEntryMetadata(fileEntry))
                                    .build();

        putBlobWithRetries(blobInfo, content, 3);
    }

    private void putBlobWithRetries(BlobInfo blobInfo, InputStream content, int retries) throws FileStorageException {
        for (int i = 1; i <= retries; i++) {
            try {
                storage.createFrom(blobInfo, content);
                return;
            } catch (IOException e) {
                LOGGER.warn(MessageFormat.format(Messages.ATTEMPT_TO_UPLOAD_BLOB_FAILED, i, retries, e.getMessage()), e);
                if (i == retries) {
                    throw new FileStorageException(e);
                }
            }
            MiscUtil.sleep(i * getRetryWaitTime());
        }
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
    public void deleteFile(String id, String space) {
        storage.delete(bucketName, id);
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) {
        removeBlobsByFilter(blob -> ObjectStoreUtil.filterBySpaceIds(blob.getMetadata(), spaceIds));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        removeBlobsByFilter(blob -> ObjectStoreUtil.filterBySpaceAndNamespace(blob.getMetadata(), space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) {
        return removeBlobsByFilter(
            blob -> ObjectStoreUtil.filterByModificationTime(blob.getMetadata(), blob.getName(), modificationTime));
    }

    private InputStream getBlobPayloadWithOffset(FileEntry fileEntry, long startOffset, long endOffset)
        throws FileStorageException {
        try {
            return getBlobWithRetriesWithOffset(fileEntry, 3, startOffset, endOffset);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public <T> T processFileContent(String space, String id,
                                    FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreUtil.createFileEntry(space, id);
        try (InputStream inputStream = openBlobStreamWithRetries(fileEntry, 3)) {
            return fileContentProcessor.process(inputStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private InputStream openBlobStreamWithRetries(FileEntry fileEntry, int maxAttempts) throws FileStorageException {
        Blob blob = getBlobWithRetries(fileEntry, maxAttempts);
        if (blob == null) {
            throw new FileStorageException(
                MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST,
                                     fileEntry.getId(), fileEntry.getSpace()));
        }
        return Channels.newInputStream(blob.reader());
    }

    private Blob getBlobWithRetries(FileEntry fileEntry, int retries) {
        for (int i = 1; i <= retries; i++) {
            try {
                return storage.get(bucketName, fileEntry.getId());
            } catch (StorageException e) {
                LOGGER.warn(MessageFormat.format(Messages.ATTEMPT_TO_DOWNLOAD_MISSING_BLOB, i, retries, fileEntry.getId()));
                if (i == retries) {
                    break;
                }
                MiscUtil.sleep(i * getRetryWaitTime());
            }
        }
        return null;
    }

    protected long getRetryWaitTime() {
        return RETRY_WAIT_TIME;
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreUtil.createFileEntry(space, id);
        return getBlobStream(fileEntry);
    }

    private InputStream getBlobStream(FileEntry fileEntry) throws FileStorageException {
        Blob blob = getBlobWithRetries(fileEntry, 3);
        if (blob == null) {
            throw new FileStorageException(
                MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST,
                                     fileEntry.getId(), fileEntry.getSpace()));
        }
        return Channels.newInputStream(blob.reader());
    }

    @Override
    public void testConnection() {
        storage.get(bucketName, "test");
    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) throws FileStorageException {
        removeBlobsByFilter(blob -> fileIds.contains(blob.getName()));
    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        FileEntry fileEntry = ObjectStoreUtil.createFileEntry(fileContentToProcess.getSpaceGuid(), fileContentToProcess.getGuid());
        InputStream res = getBlobPayloadWithOffset(fileEntry, fileContentToProcess.getStartOffset(),
                                                   fileContentToProcess.getEndOffset());
        return processContent(fileContentProcessor, res);
    }

    private <T> T processContent(FileContentProcessor<T> fileContentProcessor, InputStream res)
        throws FileStorageException {
        try {
            return fileContentProcessor.process(res);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    public Set<Blob> getAllEntries() {
        return storage.list(bucketName)
                      .streamAll()
                      .collect(Collectors.toSet());
    }

    protected int removeBlobsByFilter(Predicate<? super Blob> filter) {
        List<BlobId> blobIds = getEntryNames(filter).stream()
                                                    .map(entry -> BlobId.of(bucketName, entry))
                                                    .toList();

        if (!blobIds.isEmpty()) {
            storage.delete(blobIds);
        }
        return blobIds.size();
    }

    protected Set<String> getEntryNames(Predicate<? super Blob> filter) {
        return storage.list(bucketName)
                      .streamAll()
                      .filter(filter)
                      .map(Blob::getName)
                      .collect(Collectors.toSet());
    }

    private InputStream getBlobWithRetriesWithOffset(FileEntry fileEntry, int retries, long startOffset, long endOffset)
        throws IOException, FileStorageException {
        Blob blob = getBlobWithRetries(fileEntry, retries);
        if (blob == null) {
            throw new FileStorageException(
                MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST,
                                     fileEntry.getId(), fileEntry.getSpace()));
        }
        BlobId blobId = BlobId.of(bucketName, fileEntry.getId());
        ReadChannel reader = storage.reader(blobId);
        reader.seek(startOffset);
        reader.limit(endOffset + 1);

        return Channels.newInputStream(reader);
    }
}
