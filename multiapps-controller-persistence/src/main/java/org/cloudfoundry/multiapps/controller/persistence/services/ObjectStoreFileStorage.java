package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreFilter;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreMapper;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.GetOptions;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.http.HttpResponseException;
import org.jclouds.io.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class ObjectStoreFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorage.class);
    private static final int MAX_RETRIES_COUNT = 3;
    private static final long RETRY_BASE_WAIT_TIME_IN_MILLIS = 5000L;

    private final BlobStore blobStore;
    private final String container;

    public ObjectStoreFileStorage(BlobStore blobStore, String container) {
        this.blobStore = blobStore;
        this.container = container;
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        String entryName = fileEntry.getId();
        long fileSize = fileEntry.getSize()
                                 .longValue();
        Blob blob = blobStore.blobBuilder(entryName)
                             .payload(content)
                             .contentDisposition(fileEntry.getName())
                             .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                             .contentLength(fileSize)
                             .userMetadata(ObjectStoreMapper.createFileEntryMetadata(fileEntry))
                             .build();
        try {
            putBlobWithRetries(blob, 3);
            LOGGER.debug(MessageFormat.format(Messages.STORED_FILE_0_WITH_SIZE_1, fileEntry.getId(), fileSize));
        } catch (ContainerNotFoundException e) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(),
                                                                fileEntry.getNamespace()));
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) {
        Set<String> existingFiles = getAllEntries(new ListContainerOptions()).stream()
                                                                             .map(StorageMetadata::getName)
                                                                             .collect(Collectors.toSet());
        return fileEntries.stream()
                          .filter(fileEntry -> !existingFiles.contains(fileEntry.getId()))
                          .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String id, String space) {
        blobStore.removeBlob(container, id);
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) {
        removeBlobsByFilter(blob -> ObjectStoreFilter.filterBySpaceIds(blob.getUserMetadata(), spaceIds));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        removeBlobsByFilter(blob -> ObjectStoreFilter.filterBySpaceAndNamespace(blob.getUserMetadata(), space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) {
        return removeBlobsByFilter(
            blob -> ObjectStoreFilter.filterByModificationTime(blob.getUserMetadata(), blob.getName(), modificationTime));
    }

    @Override
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(space, id);
        try {
            Payload payload = getBlobPayload(fileEntry);
            return processContent(fileContentProcessor, payload);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(fileContentToProcess.getSpaceGuid(), fileContentToProcess.getGuid());
        try {
            Payload payload = getBlobPayloadWithOffset(fileEntry, fileContentToProcess.getStartOffset(),
                                                       fileContentToProcess.getEndOffset());
            return processContent(fileContentProcessor, payload);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private Payload getBlobPayloadWithOffset(FileEntry fileEntry, long startOffset, long endOffset) throws FileStorageException {
        Blob blob = getBlobWithRetriesWithOffset(fileEntry, MAX_RETRIES_COUNT, startOffset, endOffset);
        if (blob == null) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, fileEntry.getId(),
                                                                fileEntry.getSpace()));
        }
        return blob.getPayload();
    }

    private Blob getBlobWithRetriesWithOffset(FileEntry fileEntry, int retries, long startOffset, long endOffset) {
        GetOptions getOptions = new GetOptions().range(startOffset, endOffset);
        for (int i = 1; i <= retries; i++) {
            Blob blob = blobStore.getBlob(container, fileEntry.getId(), getOptions);
            if (blob != null) {
                return blob;
            }
            LOGGER.warn(MessageFormat.format(Messages.ATTEMPT_TO_DOWNLOAD_MISSING_BLOB, i, retries, fileEntry.getId()));
            if (i == retries) {
                break;
            }
            MiscUtil.sleep(i * getRetryWaitTime());
        }
        return null;
    }

    private Payload getBlobPayload(FileEntry fileEntry) throws FileStorageException {
        Blob blob = getBlobWithRetries(fileEntry, 3);
        if (blob == null) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, fileEntry.getId(),
                                                                fileEntry.getSpace()));
        }
        return blob.getPayload();
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(space, id);
        Payload payload = getBlobPayload(fileEntry);
        return openPayloadInputStream(payload);
    }

    private InputStream openPayloadInputStream(Payload payload) throws FileStorageException {
        try {
            return payload.openStream();
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public void testConnection() {
        blobStore.blobExists(container, "test");
    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) {
        removeBlobsByFilter(blob -> fileIds.contains(blob.getName()));
    }

    private <T> T processContent(FileContentProcessor<T> fileContentProcessor, Payload payload) throws FileStorageException {
        try (InputStream fileContentStream = payload.openStream()) {
            return fileContentProcessor.process(fileContentStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private void putBlobWithRetries(Blob blob, int retries) {
        for (int i = 1; i <= retries; i++) {
            try {
                blobStore.putBlob(container, blob, PutOptions.Builder.multipart());
                return;
            } catch (HttpResponseException e) {
                LOGGER.warn(MessageFormat.format(Messages.ATTEMPT_TO_UPLOAD_BLOB_FAILED, i, retries, e.getMessage()), e);
                if (i == retries) {
                    throw e;
                }
            }
            MiscUtil.sleep(i * getRetryWaitTime());
        }
    }

    private Blob getBlobWithRetries(FileEntry fileEntry, int retries) {
        for (int i = 1; i <= retries; i++) {
            Blob blob = blobStore.getBlob(container, fileEntry.getId());
            if (blob != null) {
                return blob;
            }
            LOGGER.warn(MessageFormat.format(Messages.ATTEMPT_TO_DOWNLOAD_MISSING_BLOB, i, retries, fileEntry.getId()));
            if (i == retries) {
                break;
            }
            MiscUtil.sleep(i * getRetryWaitTime());
        }
        return null;
    }

    protected long getRetryWaitTime() {
        return RETRY_BASE_WAIT_TIME_IN_MILLIS;
    }

    private int removeBlobsByFilter(Predicate<? super StorageMetadata> filter) {
        Set<String> entries = getEntryNames(filter);
        if (!entries.isEmpty()) {
            blobStore.removeBlobs(container, entries);
        }
        return entries.size();
    }

    private Set<String> getEntryNames(Predicate<? super StorageMetadata> filter) {
        return getAllEntries(new ListContainerOptions().withDetails()).stream()
                                                                      .filter(Objects::nonNull)
                                                                      .filter(filter)
                                                                      .map(StorageMetadata::getName)
                                                                      .collect(Collectors.toSet());
    }

    private Set<StorageMetadata> getAllEntries(ListContainerOptions options) {
        Set<StorageMetadata> entries = new HashSet<>();
        PageSet<? extends StorageMetadata> responseResult = blobStore.list(container, options);
        entries.addAll(responseResult);
        while (responseResult.getNextMarker() != null) {
            responseResult = blobStore.list(container, options.afterMarker(responseResult.getNextMarker()));
            entries.addAll(responseResult);
        }
        return entries;
    }
}
