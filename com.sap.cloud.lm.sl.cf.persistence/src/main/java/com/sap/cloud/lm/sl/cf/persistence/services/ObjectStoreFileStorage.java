package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class ObjectStoreFileStorage implements FileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFileStorage.class);

    private static final int RETRY_BASE_WAIT_TIME_IN_MILLIS = 5000;

    private BlobStore blobStore;
    private String container;

    public ObjectStoreFileStorage(BlobStore blobStore, String container) {
        this.blobStore = blobStore;
        this.container = container;
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream fileStream) throws FileStorageException {
        String entryName = fileEntry.getId();
        long fileSize = fileEntry.getSize()
            .longValue();
        Blob blob = blobStore.blobBuilder(entryName)
            .payload(fileStream)
            .contentDisposition(fileEntry.getName())
            .contentType(MediaType.OCTET_STREAM.toString())
            .contentLength(fileSize)
            .userMetadata(createFileEntryMetadata(fileEntry))
            .build();
        try {
            putBlobWithRetries(blob, 3);
            LOGGER.debug(MessageFormat.format(Messages.STORED_FILE_0_WITH_SIZE_1_SUCCESSFULLY_2, fileEntry.getId(), fileSize));
        } catch (ContainerNotFoundException e) {
            throw new FileStorageException(
                MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(), fileEntry.getNamespace()));
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) throws FileStorageException {
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
    public void deleteFile(String id, String space) throws FileStorageException {
        blobStore.removeBlob(container, id);
    }

    @Override
    public void deleteFilesBySpace(String space) throws FileStorageException {
        removeBlobsByFilter(blob -> filterBySpace(blob, space));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) throws FileStorageException {
        removeBlobsByFilter(blob -> filterBySpaceAndNamespace(blob, space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(Date modificationTime) throws FileStorageException {
        return removeBlobsByFilter(blob -> filterByModificationTime(blob, modificationTime));
    }

    @Override
    public void processFileContent(FileDownloadProcessor fileDownloadProcessor) throws FileStorageException {
        FileEntry fileEntry = fileDownloadProcessor.getFileEntry();
        InputStream fileContentStream = null;
        try {
            Blob blob = blobStore.getBlob(container, fileEntry.getId());
            Payload payload = blob.getPayload();
            fileContentStream = payload.openStream();
            fileDownloadProcessor.processContent(fileContentStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        } finally {
            closeQuietly(fileContentStream);
        }
    }

    private void putBlobWithRetries(Blob blob, int retries) {
        for (int i = 1; i <= retries; i++) {
            try {
                blobStore.putBlob(container, blob);
                return;
            } catch (HttpResponseException e) {
                LOGGER.warn(MessageFormat.format(Messages.BLOB_STORE_PUT_BLOB_FAILED, e.getMessage()), e);
                if (i == retries) {
                    throw e;
                }
            }
            CommonUtil.sleep(i * RETRY_BASE_WAIT_TIME_IN_MILLIS);
        }
    }

    private Map<String, String> createFileEntryMetadata(FileEntry fileEntry) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(FileService.FileServiceColumnNames.SPACE.toLowerCase(), fileEntry.getSpace());
        metadata.put(FileService.FileServiceColumnNames.FILE_NAME.toLowerCase(), fileEntry.getName());
        metadata.put(FileService.FileServiceColumnNames.MODIFIED.toLowerCase(), Long.toString(fileEntry.getModified()
            .getTime()));
        if (fileEntry.getNamespace() != null) {
            metadata.put(FileService.FileServiceColumnNames.NAMESPACE.toLowerCase(), fileEntry.getNamespace());
        }
        return metadata;
    }

    private int removeBlobsByFilter(Predicate<? super StorageMetadata> filter) {
        Set<String> entriesToDelete = blobStore.list(container, new ListContainerOptions().withDetails())
            .stream()
            .filter(filter)
            .map(StorageMetadata::getName)
            .collect(Collectors.toSet());

        if (!entriesToDelete.isEmpty()) {
            blobStore.removeBlobs(container, entriesToDelete);
        }
        return entriesToDelete.size();
    }

    private boolean filterByModificationTime(StorageMetadata blobMetadata, Date modificationTime) {
        Map<String, String> userMetadata = blobMetadata.getUserMetadata();
        // Clean up any blobStore entries that don't have any metadata as we can't check their creation date
        if (CollectionUtils.isEmpty(userMetadata)) {
            return true;
        }
        String longString = userMetadata.get(FileService.FileServiceColumnNames.MODIFIED.toLowerCase());
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
        String spaceParameter = userMetadata.get(FileService.FileServiceColumnNames.SPACE.toLowerCase());
        return space.equals(spaceParameter);

    }

    private boolean filterBySpaceAndNamespace(StorageMetadata blobMetadata, String space, String namespace) {
        Map<String, String> userMetadata = blobMetadata.getUserMetadata();
        if (CollectionUtils.isEmpty(userMetadata)) {
            return false;
        }
        String spaceParameter = userMetadata.get(FileService.FileServiceColumnNames.SPACE.toLowerCase());
        String namespaceParameter = userMetadata.get(FileService.FileServiceColumnNames.NAMESPACE.toLowerCase());
        return space.equals(spaceParameter) && namespace.equals(namespaceParameter);

    }

    private void closeQuietly(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
