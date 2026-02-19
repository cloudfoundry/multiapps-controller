package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreConstants;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreFilter;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreMapper;

public class AzureObjectStoreFileStorage implements FileStorage {

    private static final String SAS_TOKEN = "sas_token";
    private static final String CONTAINER_NAME = "container_name";
    private static final String CONTAINER_URI = "container_uri";
    private final HttpClient httpClient;
    private final BlobContainerClient containerClient;

    public AzureObjectStoreFileStorage(Map<String, Object> credentials) {
        this.containerClient = createContainerClient(credentials);
        this.httpClient = new OkHttpAsyncHttpClientBuilder().build();
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        BlobClient blobClient = containerClient.getBlobClient(fileEntry.getId());
        try {
            BlobParallelUploadOptions blobParallelUploadOptions = new BlobParallelUploadOptions(content);
            blobParallelUploadOptions.setMetadata(ObjectStoreMapper.createFileEntryMetadata(fileEntry));

            blobClient.uploadWithResponse(blobParallelUploadOptions, ObjectStoreConstants.OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES,
                                          null);
        } catch (BlobStorageException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) throws FileStorageException {
        Set<String> existingFiles = getAllEntriesNames();
        return fileEntries.stream()
                          .filter(fileEntry -> !existingFiles.contains(fileEntry.getId()))
                          .toList();
    }

    @Override
    public void deleteFile(String id, String space) throws FileStorageException {
        BlobClient blobClient = containerClient.getBlobClient(id);
        try {
            blobClient.deleteIfExists();
        } catch (BlobStorageException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) throws FileStorageException {
        removeBlobsByFilter(blob -> ObjectStoreFilter.filterBySpaceIds(blob.getMetadata(), spaceIds));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        removeBlobsByFilter(blob -> ObjectStoreFilter.filterBySpaceAndNamespace(blob.getMetadata(), space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) throws FileStorageException {
        return removeBlobsByFilter(
            blob -> ObjectStoreFilter.filterByModificationTime(blob.getMetadata(), blob.getName(), modificationTime));
    }

    @Override
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(space, id);
        try (InputStream inputStream = openBlobInputStream(fileEntry)) {
            return fileContentProcessor.process(inputStream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    private InputStream openBlobInputStream(FileEntry fileEntry) throws FileStorageException {
        BlobClient blobClient = containerClient.getBlobClient(fileEntry.getId());
        try {
            return blobClient.openInputStream();
        } catch (BlobStorageException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(space, id);
        return openBlobInputStream(fileEntry);
    }

    @Override
    public void testConnection() {
        containerClient.getBlobClient("test");
    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) throws FileStorageException {
        removeBlobsByFilter(blob -> fileIds.contains(blob.getName()));
    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(fileContentToProcess.getSpaceGuid(), fileContentToProcess.getGuid());
        BlobClient blobClient = containerClient.getBlobClient(fileEntry.getId());
        long contentSize = fileContentToProcess.getEndOffset() - fileContentToProcess.getStartOffset();
        BlobRange blobRange = new BlobRange(fileContentToProcess.getStartOffset(), contentSize);

        try {
            return fileContentProcessor.process(blobClient.openInputStream(blobRange, null));
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    protected BlobContainerClient createContainerClient(Map<String, Object> credentials) {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder().endpoint(getContainerUriEndpoint(credentials))
                                                                        .retryOptions(createRetryOptions())
                                                                        .httpClient(httpClient)
                                                                        .sasToken((String) credentials.get(SAS_TOKEN))
                                                                        .buildClient();

        return serviceClient.getBlobContainerClient((String) credentials.get(CONTAINER_NAME));
    }

    public String getContainerUriEndpoint(Map<String, Object> credentials) {
        if (!credentials.containsKey(CONTAINER_URI)) {
            return null;
        }
        try {
            URL containerUri = new URL((String) credentials.get(CONTAINER_URI));
            return new URL(containerUri.getProtocol(), containerUri.getHost(), containerUri.getPort(), "").toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(Messages.CANNOT_PARSE_CONTAINER_URI_OF_OBJECT_STORE, e);
        }
    }

    private RetryOptions createRetryOptions() {
        ExponentialBackoffOptions exponentialBackoffOptions = new ExponentialBackoffOptions().setBaseDelay(
                                                                                                 ObjectStoreConstants.OBJECT_STORE_INITIAL_RETRY_DELAY_CONFIG_IN_MILLIS)
                                                                                             .setMaxDelay(
                                                                                                 ObjectStoreConstants.OBJECT_STORE_MAX_RETRY_DELAY_CONFIG_IN_SECONDS)
                                                                                             .setMaxRetries(
                                                                                                 ObjectStoreConstants.OBJECT_STORE_MAX_ATTEMPTS_CONFIG);

        return new RetryOptions(exponentialBackoffOptions);
    }

    private int removeBlobsByFilter(Predicate<? super BlobItem> filter) {
        Set<String> blobNames = getEntryNames(filter);
        List<Boolean> deletedBlobsResults = new ArrayList<>();

        if (blobNames.isEmpty()) {
            return 0;
        }
        for (String blobName : blobNames) {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            deletedBlobsResults.add(blobClient.deleteIfExists());
        }

        deletedBlobsResults.removeIf(Boolean.FALSE::equals);

        return deletedBlobsResults.size();
    }

    protected Set<String> getEntryNames(Predicate<? super BlobItem> filter) {
        ListBlobsOptions listBlobsOptions = new ListBlobsOptions();
        BlobListDetails blobListDetails = new BlobListDetails();
        blobListDetails.setRetrieveMetadata(true);
        listBlobsOptions.setDetails(blobListDetails);

        return containerClient.listBlobs(listBlobsOptions, ObjectStoreConstants.OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES)
                              .stream()
                              .filter(filter)
                              .map(BlobItem::getName)
                              .collect(Collectors.toSet());
    }

    public Set<String> getAllEntriesNames() {
        return containerClient.listBlobs()
                              .stream()
                              .map(BlobItem::getName)
                              .collect(Collectors.toSet());
    }
}
