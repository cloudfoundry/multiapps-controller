package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreConstants;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreFilter;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
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

public class AwsS3ObjectStoreFileStorage extends ObjectStoreFileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3ObjectStoreFileStorage.class);
    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final String bucketName;

    public AwsS3ObjectStoreFileStorage(Map<String, Object> credentials) {
        this.bucketName = (String) credentials.get(CredentialKeys.BUCKET);
        this.s3Client = createS3Client(credentials);
    }

    protected S3Client createS3Client(Map<String, Object> credentials) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create((String) credentials.get(CredentialKeys.ACCESS_KEY_ID),
                                                                        (String) credentials.get(CredentialKeys.SECRET_ACCESS_KEY));
        S3ClientBuilder builder = S3Client.builder()
                                          .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                                          .overrideConfiguration(buildClientOverrideConfig())
                                          .httpClientBuilder(UrlConnectionHttpClient.builder()
                                                                                    .socketTimeout(
                                                                                        ObjectStoreConstants.AWS_OBJECT_STORE_SOCKET_TIMEOUT_CONFIG_IN_MINUTES)
                                                                                    .connectionTimeout(
                                                                                        ObjectStoreConstants.AWS_OBJECT_STORE_CONNECTION_TIMEOUT_CONFIG_IN_SECONDS));
        builder.endpointOverride(URI.create("https://" + credentials.get(CredentialKeys.HOST)));
        builder.region(software.amazon.awssdk.regions.Region.of((String) credentials.get(CredentialKeys.REGION)));
        return builder.build();
    }

    protected ClientOverrideConfiguration buildClientOverrideConfig() {
        StandardRetryStrategy retryStrategy = StandardRetryStrategy.builder()
                                                                   .maxAttempts(ObjectStoreConstants.OBJECT_STORE_MAX_ATTEMPTS_CONFIG)
                                                                   .backoffStrategy(BackoffStrategy.exponentialDelayHalfJitter(
                                                                       ObjectStoreConstants.OBJECT_STORE_INITIAL_RETRY_DELAY_CONFIG_IN_MILLIS,
                                                                       ObjectStoreConstants.OBJECT_STORE_MAX_RETRY_DELAY_CONFIG_IN_SECONDS))
                                                                   .build();
        return ClientOverrideConfiguration.builder()
                                          .retryStrategy(retryStrategy)
                                          .apiCallTimeout(ObjectStoreConstants.AWS_OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES)
                                          .apiCallAttemptTimeout(ObjectStoreConstants.OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES)
                                          .build();
    }

    @Override
    public void addFile(FileEntry fileEntry, InputStream content) throws FileStorageException {
        long fileSize = fileEntry.getSize()
                                 .longValue();
        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(fileEntry.getId())
                                                   .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                                   .contentDisposition(fileEntry.getName())
                                                   .metadata(ObjectStoreMapper.createFileEntryMetadata(fileEntry))
                                                   .build();
        try {
            s3Client.putObject(request, RequestBody.fromInputStream(content, fileSize));
            LOGGER.debug(MessageFormat.format(Messages.STORED_FILE_0_WITH_SIZE_1, fileEntry.getId(), fileSize));
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.S3_UPLOAD_FAILED_FILE_0_SIZE_1, fileEntry.getName(), fileSize, e));
            throw new FileStorageException(MessageFormat.format(Messages.UPLOAD_OF_FILE_WITH_NAMESPACE_FAILED, fileEntry.getName(),
                                                                fileEntry.getNamespace()), e);
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) {
        Set<String> existingKeys = new HashSet<>(listAllObjectKeys());
        return fileEntries.stream()
                          .filter(fileEntry -> !existingKeys.contains(fileEntry.getId()))
                          .toList();
    }

    private List<String> listAllObjectKeys() {
        List<String> keys = new ArrayList<>();
        forEachPage(page -> page.contents()
                                .stream()
                                .map(S3Object::key)
                                .forEach(keys::add));
        return keys;
    }

    private void forEachPage(Consumer<ListObjectsV2Response> pageConsumer) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                                                           .bucket(bucketName)
                                                           .build();
        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            pageConsumer.accept(response);
            request = request.toBuilder()
                             .continuationToken(response.nextContinuationToken())
                             .build();
        } while (Boolean.TRUE.equals(response.isTruncated()));
    }

    @Override
    protected boolean existsInObjectStore(FileEntry fileEntry) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                                                 .bucket(bucketName)
                                                 .key(fileEntry.getId())
                                                 .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public void deleteFile(String id, String space) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                                                 .bucket(bucketName)
                                                 .key(id)
                                                 .build());
    }

    @Override
    public void deleteFilesBySpaceIds(List<String> spaceIds) {
        deleteByFilterAndCount((key, metadata) -> ObjectStoreFilter.filterBySpaceIds(metadata, spaceIds));
    }

    @Override
    public void deleteFilesBySpaceAndNamespace(String space, String namespace) {
        deleteByFilterAndCount((key, metadata) -> ObjectStoreFilter.filterBySpaceAndNamespace(metadata, space, namespace));
    }

    @Override
    public int deleteFilesModifiedBefore(LocalDateTime modificationTime) {
        return deleteByFilterAndCount((key, metadata) -> ObjectStoreFilter.filterByModificationTime(metadata, key, modificationTime));
    }

    private int deleteByFilterAndCount(BiPredicate<String, Map<String, String>> predicate) {
        List<String> toDelete = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            forEachPage(page -> toDelete.addAll(collectMatchingKeys(page, predicate, executor)));
        }
        batchDelete(toDelete);
        return toDelete.size();
    }

    private List<String> collectMatchingKeys(ListObjectsV2Response page, BiPredicate<String, Map<String, String>> predicate,
                                             ExecutorService executor) {
        return page.contents()
                   .stream()
                   .map(obj -> CompletableFuture.supplyAsync(() -> fetchMetadataAndFilter(obj.key(), predicate), executor))
                   .toList()
                   .stream()
                   .map(CompletableFuture::join)
                   .filter(Objects::nonNull)
                   .toList();
    }

    private String fetchMetadataAndFilter(String key, BiPredicate<String, Map<String, String>> predicate) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                                                                               .bucket(bucketName)
                                                                               .key(key)
                                                                               .build());
            Map<String, String> metadata = response.metadata() != null ? response.metadata() : Map.of();
            return predicate.test(key, metadata) ? key : null;
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    private void batchDelete(List<String> keys) {
        for (int i = 0; i < keys.size(); i += DELETE_BATCH_SIZE) {
            List<ObjectIdentifier> identifiers = keys.subList(i, Math.min(i + DELETE_BATCH_SIZE, keys.size()))
                                                     .stream()
                                                     .map(k -> ObjectIdentifier.builder()
                                                                               .key(k)
                                                                               .build())
                                                     .toList();
            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                                                                                        .bucket(bucketName)
                                                                                        .delete(Delete.builder()
                                                                                                      .objects(identifiers)
                                                                                                      .build())
                                                                                        .build());
            for (S3Error error : response.errors()) {
                LOGGER.warn(MessageFormat.format(Messages.FAILED_TO_DELETE_FILE_0_IN_OBJECT_STORE_REASON_1, error.key(),
                                                 error.message()));
            }
        }
    }

    @Override
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(id)
                                                   .build();
        try (ResponseInputStream<GetObjectResponse> stream = getObjectStream(request, id, space)) {
            return fileContentProcessor.process(stream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public InputStream openInputStream(String space, String id) throws FileStorageException {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(id)
                                                   .build();
        return getObjectStream(request, id, space);
    }

    private ResponseInputStream<GetObjectResponse> getObjectStream(GetObjectRequest request, String id,
                                                                   String space) throws FileStorageException {
        try {
            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, id, space));
        }
    }

    @Override
    public void testConnection() {
        s3Client.listObjectsV2(ListObjectsV2Request.builder()
                                                   .bucket(bucketName)
                                                   .maxKeys(1)
                                                   .build());
    }

    @Override
    public void deleteFilesByIds(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        batchDelete(fileIds);
    }

    @Override
    public <T> T processArchiveEntryContent(FileContentToProcess fileContentToProcess, FileContentProcessor<T> fileContentProcessor)
        throws FileStorageException {
        String id = fileContentToProcess.getGuid();
        String space = fileContentToProcess.getSpaceGuid();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(id)
                                                   .range("bytes=" + fileContentToProcess.getStartOffset() + "-"
                                                              + fileContentToProcess.getEndOffset())
                                                   .build();
        try (ResponseInputStream<GetObjectResponse> stream = getObjectStream(request, id, space)) {
            return fileContentProcessor.process(stream);
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        s3Client.close();
    }

    private static final class CredentialKeys {
        static final String ACCESS_KEY_ID = "access_key_id";
        static final String SECRET_ACCESS_KEY = "secret_access_key";
        static final String BUCKET = "bucket";
        static final String HOST = "host";
        static final String REGION = "region";
    }
}
