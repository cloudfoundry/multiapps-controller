package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker;
import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadTimeoutMatcher;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreConstants;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreFilter;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class AliObjectStoreFileStorage extends ObjectStoreFileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliObjectStoreFileStorage.class);
    private static final String HTTPS_SCHEME = "https://";
    private static final int DELETE_BATCH_SIZE = DeleteObjectsRequest.DELETE_OBJECTS_ONETIME_LIMIT;

    private final OSS ossClient;
    private final String bucketName;
    private final String endpoint;
    private final UploadDurationTracker uploadDurationTracker;

    public AliObjectStoreFileStorage(Map<String, Object> credentials, UploadDurationTracker uploadDurationTracker) {
        this.bucketName = (String) credentials.get(CredentialKeys.BUCKET);
        this.endpoint = resolveEndpoint(credentials);
        this.ossClient = createOssClient(credentials);
        this.uploadDurationTracker = uploadDurationTracker;
    }

    protected OSS createOssClient(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get(CredentialKeys.ACCESS_KEY_ID);
        String secretAccessKey = (String) credentials.get(CredentialKeys.SECRET_ACCESS_KEY);
        String region = ((String) credentials.get(CredentialKeys.REGION)).substring(
            4); // so we can remove the "oss-" part, because it does not work with V4 signing

        ClientBuilderConfiguration config = new ClientBuilderConfiguration();
        config.setProtocol(Protocol.HTTPS);
        config.setSignatureVersion(SignVersion.V4);
        config.setConnectionTimeout((int) ObjectStoreConstants.ALI_OBJECT_STORE_CONNECTION_TIMEOUT_CONFIG_IN_SECONDS.toMillis());
        config.setSocketTimeout((int) ObjectStoreConstants.ALI_OBJECT_STORE_SOCKET_TIMEOUT_CONFIG_IN_MINUTES.toMillis());
        config.setRequestTimeoutEnabled(true);
        config.setRequestTimeout((int) ObjectStoreConstants.ALI_OBJECT_STORE_REQUEST_TIMEOUT_CONFIG_IN_MINUTES.toMillis());
        config.setMaxErrorRetry(ObjectStoreConstants.ALI_OBJECT_STORE_MAX_ERROR_RETRY_CONFIG);

        DefaultCredentialProvider credentialProvider = new DefaultCredentialProvider(accessKeyId, secretAccessKey);

        LOGGER.info(MessageFormat.format(Messages.INITIALIZING_ALI_OBJECT_STORE_CLIENT_FOR_BUCKET_0_ENDPOINT_1_REGION_2, bucketName,
                                         endpoint, region));

        return OSSClientBuilder.create()
                               .endpoint(endpoint)
                               .credentialsProvider(credentialProvider)
                               .clientConfiguration(config)
                               .region(region)
                               .build();
    }

    private String resolveEndpoint(Map<String, Object> credentials) {
        String endpoint = (String) credentials.get(CredentialKeys.ENDPOINT);
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint;
        }

        String host = (String) credentials.get(CredentialKeys.HOST);
        if (host != null && !host.isBlank()) {
            return HTTPS_SCHEME + host;
        }

        throw new IllegalArgumentException(Messages.MISSING_ENDPOINT_OR_HOST_FOR_ALI_OBJECT_STORE);
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        Instant startTime = Instant.now();
        long fileSize = fileEntry.getSize()
                                 .longValue();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileSize);
        metadata.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        metadata.setContentDisposition(fileEntry.getName());
        metadata.setUserMetadata(ObjectStoreMapper.createFileEntryMetadata(fileEntry));

        PutObjectRequest request = new PutObjectRequest(bucketName, fileEntry.getId(), content, metadata);
        try {
            ossClient.putObject(request);
            LOGGER.debug(MessageFormat.format(Messages.STORED_FILE_0_WITH_SIZE_1, fileEntry.getId(), fileSize));

            uploadDurationTracker.recordObjectStoreUpload(getElapsedTimeInMillis(startTime), false);
            LOGGER.info(MessageFormat.format(Messages.TIME_ELAPSED_FOR_ALI_OS_UPLOAD_0_IN_MILLIS, getElapsedTimeInMillis(startTime)));
        } catch (OSSException | ClientException e) {
            uploadDurationTracker.recordObjectStoreUpload(getElapsedTimeInMillis(startTime),
                                                          UploadTimeoutMatcher.isUploadTimeoutException(e));
            LOGGER.error(MessageFormat.format(Messages.ALI_OBJECT_STORE_OPERATION_0_FAILED_FOR_KEY_1, "putObject", fileEntry.getId(),
                                              describe(e)));
            throw new FileStorageException(
                MessageFormat.format(Messages.UPLOAD_OF_FILE_WITH_NAMESPACE_FAILED, fileEntry.getName(), fileEntry.getNamespace()), e);
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) {
        Set<String> existingKeys = listAllObjectKeys();

        return fileEntries.stream()
                          .filter(fileEntry -> !existingKeys.contains(fileEntry.getId()))
                          .toList();
    }

    @Override
    protected boolean existsInObjectStore(FileEntry fileEntry) {
        return ossClient.doesObjectExist(bucketName, fileEntry.getId());
    }

    @Override
    public void deleteFile(String id, String space) {
        try {
            ossClient.deleteObject(bucketName, id);
            LOGGER.debug(MessageFormat.format(Messages.DELETED_FILE_WITH_ID_0_AND_SPACE_1, id, space));
        } catch (OSSException | ClientException e) {
            LOGGER.error(MessageFormat.format(Messages.ALI_OBJECT_STORE_OPERATION_0_FAILED_FOR_KEY_1, "deleteObject", id, describe(e)));
            throw e;
        }
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) {
        int deletedFiles = deleteByFilterAndCount((key, metadata) -> ObjectStoreFilter.filterBySpaceIds(metadata, spaceIds));

        LOGGER.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITH_SPACEIDS_1, deletedFiles, spaceIds));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        int deletedFiles = deleteByFilterAndCount(
            (key, metadata) -> ObjectStoreFilter.filterBySpaceAndNamespace(metadata, space, namespace));

        LOGGER.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITH_SPACE_1_AND_NAMESPACE_2, deletedFiles, space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) {
        int deletedFiles = deleteByFilterAndCount((key, metadata) -> ObjectStoreFilter.filterByModificationTime(metadata, key,
                                                                                                                modificationTime));

        LOGGER.debug(MessageFormat.format(Messages.DELETED_0_FILES_MODIFIED_BEFORE_1, deletedFiles, modificationTime));
        return deletedFiles;
    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }

        deleteMultipleFiles(fileIds);
    }

    @Override
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        GetObjectRequest request = new GetObjectRequest(bucketName, id);

        try (InputStream inputStream = getObjectStream(request, id, space)) {
            return fileContentProcessor.process(inputStream);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        GetObjectRequest request = new GetObjectRequest(bucketName, id);

        return getObjectStream(request, id, space);
    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        String id = fileContentToProcess.getGuid();
        String space = fileContentToProcess.getSpaceGuid();

        GetObjectRequest request = new GetObjectRequest(bucketName, id);
        request.setRange(fileContentToProcess.getStartOffset(), fileContentToProcess.getEndOffset());

        try (InputStream inputStream = getObjectStream(request, id, space)) {
            return fileContentProcessor.process(inputStream);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public void testConnection() {
        try {
            ossClient.doesBucketExist(bucketName);
        } catch (OSSException | ClientException e) {
            LOGGER.error(MessageFormat.format(Messages.ALI_OBJECT_STORE_CONNECTION_TEST_FAILED_FOR_BUCKET_0_ENDPOINT_1, bucketName,
                                              endpoint, describe(e)));
            throw e;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        ossClient.shutdown();
    }

    private InputStream getObjectStream(GetObjectRequest request, String id, String space) throws FileStorageException {
        OSSObject ossObject;
        try {
            ossObject = ossClient.getObject(request);
        } catch (OSSException | ClientException e) {
            LOGGER.error(MessageFormat.format(Messages.ALI_OBJECT_STORE_OPERATION_0_FAILED_FOR_KEY_1, "getObject", id, describe(e)));
            throw e;
        }

        if (ossObject == null) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, id, space));
        }

        return ossObject.getObjectContent();
    }

    private int deleteByFilterAndCount(BiPredicate<String, Map<String, String>> predicate) {
        List<String> toDelete = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            forEachPage(page -> toDelete.addAll(collectMatchingKeys(page, predicate, executor)));
        }

        return deleteMultipleFiles(toDelete);
    }

    private List<String> collectMatchingKeys(List<OSSObjectSummary> summaries, BiPredicate<String, Map<String, String>> predicate,
                                             ExecutorService executor) {
        return summaries.stream()
                        .map(objectSummary -> CompletableFuture.supplyAsync(() -> fetchMetadataAndFilter(objectSummary.getKey(), predicate),
                                                                            executor))
                        .toList()
                        .stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList();
    }

    private String fetchMetadataAndFilter(String key, BiPredicate<String, Map<String, String>> predicate) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucketName, key);
            Map<String, String> userMetadata = metadata.getUserMetadata() != null ? metadata.getUserMetadata() : Map.of();

            return predicate.test(key, userMetadata) ? key : null;
        } catch (OSSException | ClientException e) {
            LOGGER.warn(
                MessageFormat.format(Messages.ALI_OBJECT_STORE_OPERATION_0_FAILED_FOR_KEY_1, "getObjectMetadata", key, describe(e)));
            return null;
        }
    }

    private int deleteMultipleFiles(List<String> keys) {
        if (keys.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        // AliCloud allows DeleteObjectsRequest to have only no more than 1000 keys
        for (int i = 0; i < keys.size(); i += DELETE_BATCH_SIZE) {
            List<String> batchToDelete = keys.subList(i, Math.min(i + DELETE_BATCH_SIZE, keys.size()));
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName).withKeys(batchToDelete);

            DeleteObjectsResult deletedResult = ossClient.deleteObjects(deleteRequest);
            deletedCount += countActuallyDeletedFiles(batchToDelete, deletedResult);
        }
        return deletedCount;
    }

    private int countActuallyDeletedFiles(List<String> requestedKeys, DeleteObjectsResult deletedResult) {
        List<String> deletedKeys = deletedResult.getDeletedObjects();
        if (deletedKeys == null) {
            return 0;
        }

        if (deletedKeys.size() < requestedKeys.size()) {
            Set<String> confirmed = new HashSet<>(deletedKeys);
            requestedKeys.stream()
                         .filter(key -> !confirmed.contains(key))
                         .forEach(key -> LOGGER.warn(MessageFormat.format(
                             Messages.FAILED_TO_DELETE_FILE_0_IN_OBJECT_STORE_REASON_1, key,
                             Messages.OBJECT_STORE_DID_NOT_CONFIRM_DELETION)));
        }

        return deletedKeys.size();
    }

    private void forEachPage(Consumer<List<OSSObjectSummary>> pageConsumer) {
        ListObjectsRequest request = new ListObjectsRequest(bucketName);
        ObjectListing listing;

        try {
            do {
                listing = ossClient.listObjects(request);
                pageConsumer.accept(listing.getObjectSummaries());
                request.setMarker(listing.getNextMarker());
            } while (listing.isTruncated());
        } catch (OSSException | ClientException e) {
            LOGGER.error(MessageFormat.format(Messages.ALI_OBJECT_STORE_LISTING_FAILED_FOR_BUCKET_0, bucketName, describe(e)));
            throw e;
        }
    }

    private Set<String> listAllObjectKeys() {
        Set<String> keys = new HashSet<>();
        forEachPage(summaries -> summaries.stream()
                                          .map(OSSObjectSummary::getKey)
                                          .forEach(keys::add));
        return keys;
    }

    private long getElapsedTimeInMillis(Instant startTime) {
        return Duration.between(startTime, Instant.now())
                       .toMillis();
    }

    private static String describe(Throwable throwable) {
        if (throwable instanceof OSSException ossException) {
            return MessageFormat.format(Messages.ALI_SERVICE_ERROR_CODE_0_REQUEST_ID_1_MESSAGE_2,
                                        ossException.getErrorCode(), ossException.getRequestId(), ossException.getErrorMessage());
        }

        if (throwable instanceof ClientException clientException) {
            Throwable rootCause = clientException.getCause() != null ? clientException.getCause() : clientException;

            return MessageFormat.format(Messages.ALI_NETWORK_ERROR_CAUSE_0_MESSAGE_1,
                                        rootCause.getClass()
                                                 .getSimpleName(),
                                        rootCause.getMessage());
        }

        return String.valueOf(throwable.getMessage());
    }

    private static final class CredentialKeys {
        static final String ACCESS_KEY_ID = "access_key_id";
        static final String SECRET_ACCESS_KEY = "secret_access_key";
        static final String BUCKET = "bucket";
        static final String ENDPOINT = "endpoint";
        static final String HOST = "host";
        static final String REGION = "region";
    }
}
