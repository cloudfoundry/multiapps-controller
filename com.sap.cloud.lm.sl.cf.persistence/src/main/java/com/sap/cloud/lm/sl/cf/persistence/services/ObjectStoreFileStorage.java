package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.http.HttpResponseException;
import org.jclouds.io.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.google.common.net.MediaType;
import com.sap.cloud.lm.sl.cf.persistence.Constants;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class ObjectStoreFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorage.class);

    private static final long RETRY_BASE_WAIT_TIME_IN_MILLIS = 5000L;

    private final BlobStore blobStore;
    private final String container;

    public ObjectStoreFileStorage(BlobStore blobStore, String container) {
        this.blobStore = blobStore;
        this.container = container;
    }

    @Override
    public void addFile(FileEntry fileEntry, File file) throws FileStorageException {
        String entryName = fileEntry.getId();
        long fileSize = fileEntry.getSize()
                                 .longValue();
        Blob blob = blobStore.blobBuilder(entryName)
                             .payload(file)
                             .contentDisposition(fileEntry.getName())
                             .contentType(MediaType.OCTET_STREAM.toString())
                             .userMetadata(createFileEntryMetadata(fileEntry))
                             .build();
        try {
            putBlobWithRetries(blob, 3);
            LOGGER.debug(MessageFormat.format(Messages.STORED_FILE_0_WITH_SIZE_1_SUCCESSFULLY_2, fileEntry.getId(), fileSize));
        } catch (ContainerNotFoundException e) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(),
                                                                fileEntry.getNamespace()));
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) {
        Set<String> existingFiles = blobStore.list(container)
                                             .stream()
                                             .map(StorageMetadata::getName)
                                             .collect(Collectors.toSet());

        return fileEntries.stream()
                          .filter(fileEntry -> {
                              String id = fileEntry.getId();
                              return !existingFiles.contains(id);
                          })
                          .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String id, String space) {
        blobStore.removeBlob(container, id);
    }

    @Override
    public void deleteFilesBySpace(String space) {
        removeBlobsByFilter(blob -> filterBySpace(blob, space));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        removeBlobsByFilter(blob -> filterBySpaceAndNamespace(blob, space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(Date modificationTime) {
        return removeBlobsByFilter(blob -> filterByModificationTime(blob, modificationTime));
    }

    @Override
    public void processFileContent(String space, String id, FileContentProcessor fileContentProcessor) throws FileStorageException {
        FileEntry fileEntry = createFileEntry(space, id);
        try {
            Blob blob = getBlobWithRetries(fileEntry, 3);
            if (blob == null) {
                throw new FileStorageException(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, fileEntry.getId(),
                                                                    fileEntry.getSpace()));
            }
            Payload payload = blob.getPayload();
            processContent(fileContentProcessor, payload);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private FileEntry createFileEntry(String space, String id) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setSpace(space);
        fileEntry.setId(id);
        return fileEntry;
    }

    private void processContent(FileContentProcessor fileContentProcessor, Payload payload) throws FileStorageException {
        try (InputStream fileContentStream = payload.openStream()) {
            fileContentProcessor.processFileContent(fileContentStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private void putBlobWithRetries(Blob blob, int retries) {
        for (int i = 1; i <= retries; i++) {
            try {
                blobStore.putBlob(container, blob);
                return;
            } catch (HttpResponseException e) {
                LOGGER.warn(MessageFormat.format(Messages.ATTEMPT_TO_UPLOAD_BLOB_FAILED, i, retries, e.getMessage()), e);
                if (i == retries) {
                    throw e;
                }
            }
            CommonUtil.sleep(i * getRetryWaitTime());
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
            CommonUtil.sleep(i * getRetryWaitTime());
        }
        return null;
    }

    protected long getRetryWaitTime() {
        return RETRY_BASE_WAIT_TIME_IN_MILLIS;
    }

    private Map<String, String> createFileEntryMetadata(FileEntry fileEntry) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(Constants.FILE_ENTRY_SPACE.toLowerCase(), fileEntry.getSpace());
        metadata.put(Constants.FILE_ENTRY_NAME.toLowerCase(), fileEntry.getName());
        metadata.put(Constants.FILE_ENTRY_MODIFIED.toLowerCase(), Long.toString(fileEntry.getModified()
                                                                                         .getTime()));
        if (fileEntry.getNamespace() != null) {
            metadata.put(Constants.FILE_ENTRY_NAMESPACE.toLowerCase(), fileEntry.getNamespace());
        }
        return metadata;
    }

    private int removeBlobsByFilter(Predicate<? super StorageMetadata> filter) {
        Set<String> entries = getEntryNames(filter);
        if (!entries.isEmpty()) {
            blobStore.removeBlobs(container, entries);
        }
        return entries.size();
    }

    private Set<String> getEntryNames(Predicate<? super StorageMetadata> filter) {
        return blobStore.list(container, new ListContainerOptions().withDetails())
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(filter)
                        .map(StorageMetadata::getName)
                        .collect(Collectors.toSet());
    }

    private boolean filterByModificationTime(StorageMetadata blobMetadata, Date modificationTime) {
        Map<String, String> userMetadata = blobMetadata.getUserMetadata();
        // Clean up any blobStore entries that don't have any metadata as we can't check their creation date
        if (CollectionUtils.isEmpty(userMetadata)) {
            return true;
        }
        String longString = userMetadata.get(Constants.FILE_ENTRY_MODIFIED.toLowerCase());
        try {
            long dateLong = Long.parseLong(longString);
            Date date = new Date(dateLong);
            return date.before(modificationTime);
        } catch (NumberFormatException e) {
            // Clean up any blobStore entries that have invalid timestamp
            return true;
        }
    }

    private boolean filterBySpace(StorageMetadata blobMetadata, String space) {
        Map<String, String> userMetadata = blobMetadata.getUserMetadata();
        if (CollectionUtils.isEmpty(userMetadata)) {
            return false;
        }
        String spaceParameter = userMetadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase());
        return space.equals(spaceParameter);
    }

    private boolean filterBySpaceAndNamespace(StorageMetadata blobMetadata, String space, String namespace) {
        Map<String, String> userMetadata = blobMetadata.getUserMetadata();
        if (CollectionUtils.isEmpty(userMetadata)) {
            return false;
        }
        String spaceParameter = userMetadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase());
        String namespaceParameter = userMetadata.get(Constants.FILE_ENTRY_NAMESPACE.toLowerCase());
        return space.equals(spaceParameter) && namespace.equals(namespaceParameter);
    }

}
