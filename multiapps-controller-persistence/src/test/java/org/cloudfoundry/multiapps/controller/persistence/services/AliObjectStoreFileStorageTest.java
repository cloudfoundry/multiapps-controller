package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AliObjectStoreFileStorageTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_SPACE_ID = UUID.randomUUID()
                                                    .toString();
    private static final String TEST_SPACE_ID_2 = UUID.randomUUID()
                                                      .toString();
    private static final String TEST_ID = UUID.randomUUID()
                                              .toString();
    private static final String TEST_ID_2 = UUID.randomUUID()
                                                .toString();
    private static final String NAMESPACE = "namespace";
    private static final LocalDateTime FILE_TIMESTAMP = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);

    @Mock
    private OSS ossClient;

    @Mock
    private UploadDurationTracker uploadDurationTracker;

    private AliObjectStoreFileStorage fileStorage;

    private final InputStream inputStream = new ByteArrayInputStream(new byte[] {});

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        fileStorage = new AliObjectStoreFileStorage(Map.of("bucket", BUCKET_NAME, "endpoint", "https://oss-eu-central-1.aliyuncs.com",
                                                           "access_key_id", "key", "secret_access_key", "secret"),
                                                    uploadDurationTracker) {
            @Override
            protected OSS createOssClient(Map<String, Object> credentials) {
                return ossClient;
            }
        };

        when(ossClient.deleteObjects(any(DeleteObjectsRequest.class))).thenAnswer(invocation -> {
            DeleteObjectsRequest request = invocation.getArgument(0);
            return new DeleteObjectsResult(new ArrayList<>(request.getKeys()));
        });
    }

    @Test
    void testAddFileCallsPutObject() throws FileStorageException {
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        fileStorage.addFile(fileEntry, inputStream);

        verify(ossClient, times(1)).putObject(any(PutObjectRequest.class));
    }

    @Test
    void testAddFileSetsUserMetadata() throws FileStorageException {
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);

        fileStorage.addFile(fileEntry, inputStream);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(ossClient).putObject(captor.capture());

        Map<String, String> userMetadata = captor.getValue()
                                                 .getMetadata()
                                                 .getUserMetadata();
        assertEquals(TEST_SPACE_ID, userMetadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase()));
    }

    @Test
    void testExistsInObjectStoreReturnsTrue() {
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);
        when(ossClient.doesObjectExist(BUCKET_NAME, TEST_ID)).thenReturn(true);

        assertTrue(fileStorage.existsInObjectStore(fileEntry));
        verify(ossClient, times(1)).doesObjectExist(BUCKET_NAME, TEST_ID);
    }

    @Test
    void testExistsInObjectStoreReturnsFalse() {
        FileEntry fileEntry = createFileEntry(TEST_SPACE_ID, TEST_ID);
        when(ossClient.doesObjectExist(BUCKET_NAME, TEST_ID)).thenReturn(false);

        assertFalse(fileStorage.existsInObjectStore(fileEntry));
    }

    @Test
    void testDeleteFileCallsDeleteObject() {
        fileStorage.deleteFile(TEST_ID, TEST_SPACE_ID);

        verify(ossClient, times(1)).deleteObject(BUCKET_NAME, TEST_ID);
    }

    @Test
    void testDeleteFilesByIdsCallsDeleteObjects() {
        fileStorage.deleteFilesByIds(List.of(TEST_ID, TEST_ID_2));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(ossClient, times(1)).deleteObjects(captor.capture());
        assertTrue(captor.getValue()
                         .getKeys()
                         .containsAll(List.of(TEST_ID, TEST_ID_2)));
    }

    @Test
    void testDeleteFilesByIdsDoesNothingWhenEmpty() {
        fileStorage.deleteFilesByIds(List.of());

        verify(ossClient, never()).deleteObjects(any());
    }

    @Test
    void testDeleteFilesByIdsBatchesRequestsEachByOssLimit() {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < 2500; i++) {
            ids.add("id-" + i);
        }

        fileStorage.deleteFilesByIds(ids);

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(ossClient, times(3)).deleteObjects(captor.capture());

        List<DeleteObjectsRequest> requests = captor.getAllValues();
        assertEquals(1000, requests.get(0)
                                   .getKeys()
                                   .size());
        assertEquals(1000, requests.get(1)
                                   .getKeys()
                                   .size());
        assertEquals(500, requests.get(2)
                                  .getKeys()
                                  .size());

        requests.forEach(request -> assertTrue(request.getKeys()
                                                      .size() <= 1000));
    }

    @Test
    void testDeleteCountReflectsWhatObjectStoreConfirmed() {
        when(ossClient.deleteObjects(any(DeleteObjectsRequest.class)))
            .thenReturn(new DeleteObjectsResult(List.of(TEST_ID)));

        mockListObjects(List.of(
            summaryWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE),
            summaryWithMetadata(TEST_ID_2, TEST_SPACE_ID, NAMESPACE)
        ));

        int deleted = fileStorage.deleteFilesModifiedBefore(LocalDateTime.of(2099, Month.JANUARY, 1, 0, 0));

        assertEquals(1, deleted);
    }

    @Test
    void testDeleteFilesBySpaceIdDeletesMatchingObjects() {
        mockListObjects(List.of(
            summaryWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE),
            summaryWithMetadata(TEST_ID_2, TEST_SPACE_ID_2, NAMESPACE)
        ));

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(ossClient, times(1)).deleteObjects(captor.capture());
        assertTrue(captor.getValue()
                         .getKeys()
                         .contains(TEST_ID));
        assertFalse(captor.getValue()
                          .getKeys()
                          .contains(TEST_ID_2));
    }

    @Test
    void testDeleteFilesBySpaceAndNamespaceDeletesMatchingObjects() {
        String otherNamespace = "other-namespace";
        mockListObjects(List.of(
            summaryWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE),
            summaryWithMetadata(TEST_ID_2, TEST_SPACE_ID, otherNamespace)
        ));

        fileStorage.deleteFilesBySpaceAndNamespace(TEST_SPACE_ID, NAMESPACE);

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(ossClient, times(1)).deleteObjects(captor.capture());
        assertEquals(1, captor.getValue()
                              .getKeys()
                              .size());
        assertTrue(captor.getValue()
                         .getKeys()
                         .contains(TEST_ID));
    }

    @Test
    void testDeleteFilesModifiedBeforeDeletesOldFiles() {
        long nowMillis = FILE_TIMESTAMP.atZone(ZoneId.systemDefault())
                                       .toInstant()
                                       .toEpochMilli();
        long oldMillis = nowMillis - 60_000;
        long newMillis = nowMillis + 60_000;

        mockListObjects(List.of(
            summaryWithModifiedTime(TEST_ID, oldMillis),
            summaryWithModifiedTime(TEST_ID_2, newMillis)
        ));

        int deleted = fileStorage.deleteFilesModifiedBefore(FILE_TIMESTAMP);

        assertEquals(1, deleted);
        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(ossClient, times(1)).deleteObjects(captor.capture());
        assertTrue(captor.getValue()
                         .getKeys()
                         .contains(TEST_ID));
        assertFalse(captor.getValue()
                          .getKeys()
                          .contains(TEST_ID_2));
    }

    @Test
    void testProcessFileContentReturnsContent() throws Exception {
        OSSObject ossObject = new OSSObject();
        ossObject.setObjectContent(new ByteArrayInputStream("hello".getBytes()));
        when(ossClient.getObject(any(GetObjectRequest.class))).thenReturn(ossObject);

        String result = fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, stream -> new String(stream.readAllBytes()));

        assertEquals("hello", result);
    }

    @Test
    void testProcessFileContentThrowsWhenObjectIsMissing() {
        when(ossClient.getObject(any(GetObjectRequest.class))).thenReturn(null);

        assertThrows(FileStorageException.class,
                     () -> fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, stream -> null));
    }

    @Test
    void testProcessArchiveEntryContentSetsRange() throws Exception {
        OSSObject ossObject = new OSSObject();
        ossObject.setObjectContent(new ByteArrayInputStream("archive-data".getBytes()));
        when(ossClient.getObject(any(GetObjectRequest.class))).thenReturn(ossObject);

        FileContentToProcess contentToProcess = buildFileContentToProcess(TEST_SPACE_ID, TEST_ID, 0L, 11L);
        assertNotNull(fileStorage.processArchiveEntryContent(contentToProcess, stream -> stream.readAllBytes()));

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(ossClient).getObject(captor.capture());

        assertNotNull(captor.getValue());
    }

    @Test
    void testOpenInputStreamReturnsStream() throws FileStorageException {
        OSSObject ossObject = new OSSObject();
        ossObject.setObjectContent(new ByteArrayInputStream("data".getBytes()));
        when(ossClient.getObject(any(GetObjectRequest.class))).thenReturn(ossObject);

        assertNotNull(fileStorage.openInputStream(TEST_SPACE_ID, TEST_ID));
    }

    @Test
    void testTestConnectionCallsDoesBucketExist() {
        when(ossClient.doesBucketExist(BUCKET_NAME)).thenReturn(true);

        fileStorage.testConnection();

        verify(ossClient, times(1)).doesBucketExist(BUCKET_NAME);
    }

    @Test
    void testConnectionRethrowsClientExceptionOnNetworkFailure() {
        ClientException networkFailure = new ClientException("connect failed",
                                                             new UnknownHostException("oss-eu-central-1.aliyuncs.com"));

        when(ossClient.doesBucketExist(BUCKET_NAME)).thenThrow(networkFailure);

        assertThrows(ClientException.class, () -> fileStorage.testConnection());
    }

    @Test
    void testProcessFileContentRethrowsOssExceptionFromGetObject() {
        when(ossClient.getObject(any(GetObjectRequest.class))).thenThrow(new OSSException("access denied"));

        assertThrows(OSSException.class,
                     () -> fileStorage.processFileContent(TEST_SPACE_ID, TEST_ID, stream -> null));
    }

    @Test
    void testFilteredDeleteRethrowsClientExceptionFromListObjects() {
        when(ossClient.listObjects(any(ListObjectsRequest.class)))
            .thenThrow(new ClientException("read timed out", new SocketTimeoutException("Read timed out")));

        assertThrows(ClientException.class, () -> fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID)));
    }

    @Test
    void testGetFileEntriesWithoutContentReturnsMissing() {
        mockListObjects(List.of(summaryWithKey(TEST_ID)));

        FileEntry existing = createFileEntry(TEST_SPACE_ID, TEST_ID);
        FileEntry missing = createFileEntry(TEST_SPACE_ID, TEST_ID_2);

        List<FileEntry> result = fileStorage.getFileEntriesWithoutContent(List.of(existing, missing));

        assertEquals(1, result.size());
        assertEquals(TEST_ID_2, result.getFirst()
                                      .getId());
    }

    @Test
    void testDestroyShutdownsOssClient() {
        fileStorage.destroy();

        verify(ossClient, times(1)).shutdown();
    }

    @Test
    void testListPaginatesUntilNotTruncated() {
        OSSObjectSummary summary1 = summaryWithMetadata(TEST_ID, TEST_SPACE_ID, NAMESPACE);
        OSSObjectSummary summary2 = summaryWithMetadata(TEST_ID_2, TEST_SPACE_ID, NAMESPACE);

        ObjectListing page1 = new ObjectListing();
        page1.setTruncated(true);
        page1.getObjectSummaries()
             .add(summary1);
        page1.setNextMarker("marker-1");

        ObjectListing page2 = new ObjectListing();
        page2.setTruncated(false);
        page2.getObjectSummaries()
             .add(summary2);

        when(ossClient.listObjects(any(ListObjectsRequest.class)))
            .thenReturn(page1)
            .thenReturn(page2);

        fileStorage.deleteFilesBySpaceIds(List.of(TEST_SPACE_ID));

        verify(ossClient, times(2)).listObjects(any(ListObjectsRequest.class));
        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(ossClient).deleteObjects(captor.capture());
        assertTrue(captor.getValue()
                         .getKeys()
                         .containsAll(List.of(TEST_ID, TEST_ID_2)));
    }

    @Test
    void testEndpointDerivedFromHostWhenEndpointAbsent() {
        AliObjectStoreFileStorage storageWithHost = new AliObjectStoreFileStorage(
            Map.of("bucket", BUCKET_NAME, "host", "oss-eu-central-1.aliyuncs.com", "access_key_id", "k", "secret_access_key", "s"),
            uploadDurationTracker) {
            @Override
            protected OSS createOssClient(Map<String, Object> credentials) {
                return ossClient;
            }
        };

        when(ossClient.doesBucketExist(BUCKET_NAME)).thenReturn(true);

        storageWithHost.testConnection();

        verify(ossClient, atLeastOnce()).doesBucketExist(BUCKET_NAME);
    }

    @Test
    void testMissingEndpointAndHostThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new AliObjectStoreFileStorage(
                Map.of("bucket", BUCKET_NAME, "access_key_id", "k", "secret_access_key", "s"),
                uploadDurationTracker));
    }

    private FileEntry createFileEntry(String space, String id) {
        return ImmutableFileEntry.builder()
                                 .id(id)
                                 .space(space)
                                 .namespace(NAMESPACE)
                                 .size(BigInteger.TEN)
                                 .modified(FILE_TIMESTAMP)
                                 .name("file.bin")
                                 .build();
    }

    private FileContentToProcess buildFileContentToProcess(String space, String id, long start, long end) {
        return new FileContentToProcess() {
            @Override
            public String getSpaceGuid() {
                return space;
            }

            @Override
            public String getGuid() {
                return id;
            }

            @Override
            public long getStartOffset() {
                return start;
            }

            @Override
            public long getEndOffset() {
                return end;
            }
        };
    }

    private OSSObjectSummary summaryWithKey(String key) {
        OSSObjectSummary summary = new OSSObjectSummary();
        summary.setKey(key);
        summary.setBucketName(BUCKET_NAME);

        return summary;
    }

    private OSSObjectSummary summaryWithMetadata(String key, String space, String namespace) {
        OSSObjectSummary s = summaryWithKey(key);
        Map<String, String> meta = new HashMap<>();
        meta.put(Constants.FILE_ENTRY_SPACE.toLowerCase(), space);

        if (namespace != null) {
            meta.put(Constants.FILE_ENTRY_NAMESPACE.toLowerCase(), namespace);
        }

        meta.put(Constants.FILE_ENTRY_MODIFIED.toLowerCase(),
                 String.valueOf(FILE_TIMESTAMP.atZone(ZoneId.systemDefault())
                                              .toInstant()
                                              .toEpochMilli()));

        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setUserMetadata(meta);
        when(ossClient.getObjectMetadata(BUCKET_NAME, key)).thenReturn(objMeta);

        return s;
    }

    private OSSObjectSummary summaryWithModifiedTime(String key, long epochMillis) {
        OSSObjectSummary s = summaryWithKey(key);
        Map<String, String> meta = new HashMap<>();
        meta.put(Constants.FILE_ENTRY_MODIFIED.toLowerCase(), String.valueOf(epochMillis));

        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setUserMetadata(meta);
        when(ossClient.getObjectMetadata(BUCKET_NAME, key)).thenReturn(objMeta);

        return s;
    }

    private void mockListObjects(List<OSSObjectSummary> summaries) {
        ObjectListing listing = new ObjectListing();
        listing.setTruncated(false);
        listing.getObjectSummaries()
               .addAll(summaries);

        when(ossClient.listObjects(any(ListObjectsRequest.class))).thenReturn(listing);
    }
}
