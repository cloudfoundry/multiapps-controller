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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class AwsS3ObjectStoreFileStorage extends ObjectStoreFileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3ObjectStoreFileStorage.class);
    private static final int DELETE_BATCH_SIZE = 1000;
    private static final String ACCESS_KEY_ID = "access_key_id";
    private static final String SECRET_ACCESS_KEY = "secret_access_key";
    private static final String BUCKET = "bucket";
    private static final String HOST = "host";
    private static final String REGION = "region";

    private final S3Client s3Client;
    private final String bucketName;

    public AwsS3ObjectStoreFileStorage(Map<String, Object> credentials) {
        this.bucketName = (String) credentials.get(BUCKET);
        this.s3Client = createS3Client(credentials);
    }

    protected S3Client createS3Client(Map<String, Object> credentials) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create((String) credentials.get(ACCESS_KEY_ID),
                                                                        (String) credentials.get(SECRET_ACCESS_KEY));
        S3ClientBuilder builder = S3Client.builder()
                                          .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                                          .overrideConfiguration(buildClientOverrideConfig())
                                          .httpClientBuilder(UrlConnectionHttpClient.builder());
        builder.endpointOverride(URI.create("https://" + credentials.get(HOST)));
        builder.region(software.amazon.awssdk.regions.Region.of((String) credentials.get(REGION)));
        return builder.build();
    }

    private ClientOverrideConfiguration buildClientOverrideConfig() {
        StandardRetryStrategy retryStrategy = StandardRetryStrategy.builder()
                                                                   .maxAttempts(ObjectStoreConstants.OBJECT_STORE_MAX_ATTEMPTS_CONFIG)
                                                                   .backoffStrategy(BackoffStrategy.exponentialDelayHalfJitter(
                                                                       ObjectStoreConstants.OBJECT_STORE_INITIAL_RETRY_DELAY_CONFIG_IN_MILLIS,
                                                                       ObjectStoreConstants.OBJECT_STORE_MAX_RETRY_DELAY_CONFIG_IN_SECONDS))
                                                                   .build();
        return ClientOverrideConfiguration.builder()
                                          .retryStrategy(retryStrategy)
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
            throw new FileStorageException(MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(),
                                                                fileEntry.getNamespace()), e);
        }
    }

    @Override
    public List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) {
        Set<String> existingKeys = new HashSet<>(listAllObjectKeys());
        return fileEntries.stream()
                          .filter(fileEntry -> !existingKeys.contains(fileEntry.getId()))
                          .collect(Collectors.toList());
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

    // TODO: refactor these methods to get the response with metadata as well
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

    @Override
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(id)
                                                   .build();
        try (ResponseInputStream<GetObjectResponse> stream = getObjectStream(request, id, space)) {
            return fileContentProcessor.process(stream);
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException(e);
        }
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
        } catch (FileStorageException e) {
            throw e;
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
        Set<String> fileIdSet = new HashSet<>(fileIds);
        List<String> toDelete = listAllObjectKeys().stream()
                                                   .filter(fileIdSet::contains)
                                                   .collect(Collectors.toList());
        batchDelete(toDelete);
    }

    @Override
    public void destroy() {
        super.destroy();
        s3Client.close();
    }

    private ResponseInputStream<GetObjectResponse> getObjectStream(GetObjectRequest request, String id,
                                                                   String space) throws FileStorageException {
        try {
            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw new FileStorageException(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, id, space));
        }
    }

    private List<String> listAllObjectKeys() {
        List<String> keys = new ArrayList<>();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                                                           .bucket(bucketName)
                                                           .build();
        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            for (S3Object obj : response.contents()) {
                keys.add(obj.key());
            }
            request = request.toBuilder()
                             .continuationToken(response.nextContinuationToken())
                             .build();
        } while (Boolean.TRUE.equals(response.isTruncated()));
        return keys;
    }

    private int deleteByFilterAndCount(BiPredicate<String, Map<String, String>> predicate) {
        List<String> allKeys = listAllObjectKeys();
        List<String> toDelete = new ArrayList<>();
        for (String key : allKeys) {
            Map<String, String> metadata = fetchMetadata(key);
            if (predicate.test(key, metadata)) {
                toDelete.add(key);
            }
        }
        batchDelete(toDelete);
        return toDelete.size();
    }

    private Map<String, String> fetchMetadata(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                                                                               .bucket(bucketName)
                                                                               .key(key)
                                                                               .build());
            Map<String, String> metadata = response.metadata();
            return metadata != null ? metadata : Map.of();
        } catch (NoSuchKeyException e) {
            return Map.of();
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
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                                                       .bucket(bucketName)
                                                       .delete(Delete.builder()
                                                                     .objects(identifiers)
                                                                     .build())
                                                       .build());
        }
    }
}
