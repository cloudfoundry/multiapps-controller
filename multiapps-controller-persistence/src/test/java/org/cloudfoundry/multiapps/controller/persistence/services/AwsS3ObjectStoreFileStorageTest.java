package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.ObjectStoreConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.s3.S3Client;
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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AwsS3ObjectStoreFileStorageTest {

    private static final LocalDateTime FILE_TIMESTAMP = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);

    private static final String BUCKET_NAME = "test-bucket";

    @Mock
    private S3Client s3Client;

    @Mock
    private FileContentProcessor<String> fileContentProcessor;

    private AwsS3ObjectStoreFileStorage fileStorage;
    private final InputStream inputStream = new ByteArrayInputStream(new byte[] {});
    private static final String TEST_SPACE_ID = UUID.randomUUID()
                                                     .toString();
    private static final String TEST_SPACE_ID_2 = UUID.randomUUID()
                                                       .toString();
    private static final String TEST_ID = UUID.randomUUID()
                                               .toString();
    private static final String TEST_ID_2 = UUID.randomUUID()
                                                 .toString();
    private static final String NAMESPACE = "namespace";
    private static final String NAMESPACE_2 = "namespace_2";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        fileStorage = new AwsS3ObjectStoreFileStorage(Map.of("bucket", BUCKET_NAME)) {

            @Override
            protected S3Client createS3Client(Map<String, Object> credentials) {
                return s3Client;
            }
        };
    }

    @Test
    void testAddFileWithSuccessfulUpload() throws FileStorageException {
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        fileStorage.addFile(fileEntry, inputStream);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        PutObjectRequest request = captor.getValue();
        assertEquals(BUCKET_NAME, request.bucket());
        assertEquals(TEST_ID, request.key());
    }

    @Test
    void testAddFileWithFailedUpload() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
            .thenThrow(S3Exception.builder()
                                  .message("upload failed")
                                  .build());
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        assertThrows(FileStorageException.class, () -> fileStorage.addFile(fileEntry, inputStream));
    }

    @Test
    void testGetFileEntriesWithoutContent() {
        setupListObjects(TEST_ID_2);
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        List<FileEntry> result = fileStorage.getFileEntriesWithoutContent(List.of(fileEntry));

        assertEquals(1, result.size());
        assertEquals(TEST_ID, result.getFirst()
                                    .getId());
    }

    @Test
    void testGetFileEntriesWithoutContentWithoutMatches() {
        setupListObjects(TEST_ID, TEST_ID_2);
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        List<FileEntry> result = fileStorage.getFileEntriesWithoutContent(List.of(fileEntry));

        assertEquals(0, result.size());
    }

    @Test
        void testExistsInObjectStoreWhenFileExists() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                                                                                             .build());
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        List<FileEntry> result = fileStorage.getExistingFileEntries(List.of(fileEntry));

        assertEquals(1, result.size());
        assertEquals(TEST_ID, result.getFirst()
                                    .getId());
    }

    @Test
    void testExistsInObjectStoreWhenFileDoesNotExist() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                                                                                            .build());
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        List<FileEntry> result = fileStorage.getExistingFileEntries(List.of(fileEntry));

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteFile() {
        fileStorage.deleteFile(TEST_ID, TEST_SPACE_ID);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue()
                                        .bucket());
        assertEquals(TEST_ID, captor.getValue()
                                    .key());
    }

    @Test
    void testDeleteFilesBySpaceIdsWithAllMatchingItems() {
        setupListObjectsWithKeys(TEST_ID, TEST_ID_2);
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);
        setupHeadObjectWithMetadata(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE_2, FILE_TIMESTAMP);

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID, TEST_SPACE_ID_2));

        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void testDeleteFilesBySpaceIdsWithOneMatchingItem() {
        setupListObjectsWithKeys(TEST_ID, TEST_ID_2);
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);
        setupHeadObjectWithMetadata(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE_2, FILE_TIMESTAMP);

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        assertEquals(1, captor.getValue()
                              .delete()
                              .objects()
                              .size());
    }

    @Test
    void testDeleteFilesBySpaceIdsWithoutMatchingItem() {
        setupListObjectsWithKeys(TEST_ID);
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);

        fileStorage.deleteFilesBySpaceIds(List.of("non-existing-space"));

        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void testDeleteFilesBySpaceAndNamespaceWithOneMatch() {
        setupListObjectsWithKeys(TEST_ID, TEST_ID_2);
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);
        setupHeadObjectWithMetadata(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE_2, FILE_TIMESTAMP);

        fileStorage.deleteFilesBySpaceAndNamespace(TEST_SPACE_ID, NAMESPACE);

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        assertEquals(1, captor.getValue()
                              .delete()
                              .objects()
                              .size());
        assertEquals(TEST_ID, captor.getValue()
                                    .delete()
                                    .objects()
                                    .getFirst()
                                    .key());
    }

    @Test
    void testDeleteFilesModifiedBefore() {
        LocalDateTime oldModified = FILE_TIMESTAMP
                                                 .minusMinutes(15);

        setupListObjectsWithKeys(TEST_ID, TEST_ID_2);
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, oldModified);
        setupHeadObjectWithMetadata(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE_2, oldModified);

        int deletedCount = fileStorage.deleteFilesModifiedBefore(FILE_TIMESTAMP);

        assertEquals(2, deletedCount);
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void testDeleteFilesModifiedBeforeWithNoOldFiles() {
        setupListObjectsWithKeys(TEST_ID);
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);

        LocalDateTime cutoff = FILE_TIMESTAMP
                                            .minusDays(1);
        int deletedCount = fileStorage.deleteFilesModifiedBefore(cutoff);

        assertEquals(0, deletedCount);
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testProcessFileContent() throws FileStorageException, IOException {
        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);
        when(fileContentProcessor.process(any(InputStream.class))).thenReturn("result");

        String result = fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, fileContentProcessor);

        assertEquals("result", result);
        verify(fileContentProcessor).process(mockStream);
    }

    @Test
    void testProcessFileContentWithNoSuchKeyException() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                                                                                          .build());

        assertThrows(FileStorageException.class, () -> fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, fileContentProcessor));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testProcessFileContentWithProcessorException() throws IOException {
        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);
        when(fileContentProcessor.process(any(InputStream.class))).thenThrow(new IOException("processing failed"));

        assertThrows(FileStorageException.class, () -> fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, fileContentProcessor));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testOpenInputStream() throws FileStorageException {
        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

        InputStream result = fileStorage.openInputStream(TEST_SPACE_ID, TEST_ID);

        assertNotNull(result);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue()
                                        .bucket());
        assertEquals(TEST_ID, captor.getValue()
                                    .key());
    }

    @Test
    void testOpenInputStreamWithNoSuchKeyException() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                                                                                          .build());

        assertThrows(FileStorageException.class, () -> fileStorage.openInputStream(TEST_SPACE_ID, TEST_ID));
    }

    @Test
    void testTestConnection() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(ListObjectsV2Response.builder()
                                                                                                      .isTruncated(false)
                                                                                                      .build());

        fileStorage.testConnection();

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue()
                                        .bucket());
        assertEquals(1, captor.getValue()
                              .maxKeys());
    }

    @Test
    void testDeleteFilesByIds() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder()
                                                                                                      .build());

        fileStorage.deleteFilesByIds(List.of(TEST_ID));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        assertEquals(1, captor.getValue()
                              .delete()
                              .objects()
                              .size());
        assertEquals(TEST_ID, captor.getValue()
                                    .delete()
                                    .objects()
                                    .getFirst()
                                    .key());
    }

    @Test
    void testDeleteFilesByIdsWithEmptyList() {
        fileStorage.deleteFilesByIds(List.of());

        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void testDeleteFilesByIdsWithAllMatching() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder()
                                                                                                      .build());

        fileStorage.deleteFilesByIds(List.of(TEST_ID, TEST_ID_2));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        assertEquals(2, captor.getValue()
                              .delete()
                              .objects()
                              .size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testProcessArchiveEntryContent() throws FileStorageException, IOException {
        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);
        when(fileContentProcessor.process(any(InputStream.class))).thenReturn("archive-result");

        FileContentToProcess fileContentToProcess = ImmutableFileContentToProcess.builder()
                                                                                 .guid(TEST_ID)
                                                                                 .spaceGuid(TEST_SPACE_ID)
                                                                                 .startOffset(100L)
                                                                                 .endOffset(500L)
                                                                                 .build();

        String result = fileStorage.processArchiveEntryContent(fileContentToProcess, fileContentProcessor);

        assertEquals("archive-result", result);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertEquals(BUCKET_NAME, captor.getValue()
                                        .bucket());
        assertEquals(TEST_ID, captor.getValue()
                                    .key());
        assertEquals("bytes=100-500", captor.getValue()
                                            .range());
    }

    @Test
    void testProcessArchiveEntryContentWithNoSuchKeyException() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                                                                                          .build());
        FileContentToProcess fileContentToProcess = ImmutableFileContentToProcess.builder()
                                                                                 .guid(TEST_ID)
                                                                                 .spaceGuid(TEST_SPACE_ID)
                                                                                 .startOffset(0L)
                                                                                 .endOffset(100L)
                                                                                 .build();

        assertThrows(FileStorageException.class, () -> fileStorage.processArchiveEntryContent(fileContentToProcess, fileContentProcessor));
    }

    @Test
    void getExistingFileEntriesWhenAllEntriesExist() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                                                                                             .build());
        FileEntry firstEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);
        FileEntry secondEntry = createFileEntry(TEST_SPACE_ID_2, TEST_ID_2);

        List<FileEntry> result = fileStorage.getExistingFileEntries(List.of(firstEntry, secondEntry));

        assertEquals(2, result.size());
        List<String> returnedIds = result.stream()
                                         .map(FileEntry::getId)
                                         .toList();
        assertTrue(returnedIds.contains(TEST_ID));
        assertTrue(returnedIds.contains(TEST_ID_2));
    }

    @Test
    void getExistingFileEntriesWhenNoEntriesExist() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                                                                                            .build());
        FileEntry firstEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);
        FileEntry secondEntry = createFileEntry(TEST_SPACE_ID_2, TEST_ID_2);

        List<FileEntry> result = fileStorage.getExistingFileEntries(List.of(firstEntry, secondEntry));

        assertTrue(result.isEmpty());
    }

    @Test
    void getExistingFileEntriesWhenSomeEntriesExist() {
        when(s3Client.headObject(headObjectRequestForKey(TEST_ID))).thenReturn(HeadObjectResponse.builder()
                                                                                                 .build());
        when(s3Client.headObject(headObjectRequestForKey(TEST_ID_2))).thenThrow(NoSuchKeyException.builder()
                                                                                                  .build());
        FileEntry existingEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);
        FileEntry nonExistingEntry = createFileEntry(TEST_SPACE_ID_2, TEST_ID_2);

        List<FileEntry> result = fileStorage.getExistingFileEntries(List.of(existingEntry, nonExistingEntry));

        assertEquals(1, result.size());
        assertEquals(TEST_ID, result.getFirst()
                                    .getId());
    }

    @Test
    void testDeleteFilesBySpaceIdsWithPagination() {
        String thirdId = UUID.randomUUID()
                             .toString();
        ListObjectsV2Response firstPage = ListObjectsV2Response.builder()
                                                               .contents(S3Object.builder()
                                                                                 .key(TEST_ID)
                                                                                 .build())
                                                               .isTruncated(true)
                                                               .nextContinuationToken("token-1")
                                                               .build();
        ListObjectsV2Response secondPage = ListObjectsV2Response.builder()
                                                                .contents(S3Object.builder()
                                                                                  .key(TEST_ID_2)
                                                                                  .build(),
                                                                          S3Object.builder()
                                                                                  .key(thirdId)
                                                                                  .build())
                                                                .isTruncated(false)
                                                                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(firstPage, secondPage);
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder()
                                                                                                      .build());
        setupHeadObjectWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);
        setupHeadObjectWithMetadata(TEST_ID_2, TEST_SPACE_ID, NAMESPACE, FILE_TIMESTAMP);
        setupHeadObjectWithMetadata(thirdId, TEST_SPACE_ID_2, NAMESPACE_2, FILE_TIMESTAMP);

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        assertEquals(2, captor.getValue()
                              .delete()
                              .objects()
                              .size());
        verify(s3Client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void testDeleteFilesBySpaceIdsWhenHeadObjectReturnsNoSuchKey() {
        setupListObjectsWithKeys(TEST_ID);
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                                                                                            .build());

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID));

        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void getExistingFileEntriesWithEmptyList() {
        List<FileEntry> result = fileStorage.getExistingFileEntries(List.of());

        assertTrue(result.isEmpty());
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void testBuildClientOverrideConfigSetsApiCallTimeout() {
        ClientOverrideConfiguration config = fileStorage.buildClientOverrideConfig();

        assertTrue(config.apiCallTimeout()
                         .isPresent());
        assertEquals(ObjectStoreConstants.AWS_OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES, config.apiCallTimeout()
                                                                                                  .get());
    }

    @Test
    void testBuildClientOverrideConfigSetsApiCallAttemptTimeout() {
        ClientOverrideConfiguration config = fileStorage.buildClientOverrideConfig();

        assertTrue(config.apiCallAttemptTimeout()
                         .isPresent());
        assertEquals(ObjectStoreConstants.OBJECT_STORE_TOTAL_TIMEOUT_CONFIG_IN_MINUTES, config.apiCallAttemptTimeout()
                                                                                          .get());
    }

    @Test
    void testBuildClientOverrideConfigSetsRetryStrategy() {
        ClientOverrideConfiguration config = fileStorage.buildClientOverrideConfig();

        assertTrue(config.retryStrategy()
                         .isPresent());
        assertInstanceOf(StandardRetryStrategy.class, config.retryStrategy()
                                                            .get());
        assertEquals(ObjectStoreConstants.OBJECT_STORE_MAX_ATTEMPTS_CONFIG, config.retryStrategy()
                                                                                .get()
                                                                                .maxAttempts());
    }

    @Test
    void testCreateS3ClientWithValidCredentials() {
        Map<String, Object> credentials = Map.of("access_key_id", "test-key",
                                                 "secret_access_key", "test-secret",
                                                 "bucket", "test-bucket",
                                                 "host", "s3.amazonaws.com",
                                                 "region", "eu-central-1");
        AwsS3ObjectStoreFileStorage storage = new AwsS3ObjectStoreFileStorage(credentials);

        assertNotNull(storage);
        storage.destroy();
    }

    private HeadObjectRequest headObjectRequestForKey(String key) {
        return HeadObjectRequest.builder()
                                .bucket(BUCKET_NAME)
                                .key(key)
                                .build();
    }

    private void setupListObjects(String... keys) {
        ListObjectsV2Response.Builder responseBuilder = ListObjectsV2Response.builder()
                                                                             .isTruncated(false);
        List<S3Object> s3Objects = Arrays.stream(keys)
                                         .map(key -> S3Object.builder()
                                                             .key(key)
                                                             .build())
                                         .toList();
        responseBuilder.contents(s3Objects);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(responseBuilder.build());
    }

    private void setupListObjectsWithKeys(String... keys) {
        setupListObjects(keys);
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder()
                                                                                                      .build());
    }

    private void setupHeadObjectWithMetadata(String key, String space, String namespace, LocalDateTime modified) {
        long modifiedMillis = modified.atZone(ZoneId.systemDefault())
                                      .toInstant()
                                      .toEpochMilli();
        HeadObjectResponse response = HeadObjectResponse.builder()
                                                        .metadata(Map.of("space", space,
                                                                         "namespace", namespace,
                                                                         "modified", Long.toString(modifiedMillis)))
                                                        .build();
        when(s3Client.headObject(headObjectRequestForKey(key))).thenReturn(response);
    }

    private static FileEntry createFileEntry(String space, String id) {
        return ImmutableFileEntry.builder()
                                 .space(space)
                                 .size(BigInteger.TEN)
                                 .modified(FILE_TIMESTAMP)
                                 .id(id)
                                 .build();
    }
}
