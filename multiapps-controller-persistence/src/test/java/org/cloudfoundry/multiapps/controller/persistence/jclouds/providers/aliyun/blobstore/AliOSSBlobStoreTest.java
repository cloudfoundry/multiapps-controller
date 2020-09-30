package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.AliOSSApi;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.internal.BlobBuilderImpl;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.blobstore.util.BlobUtils;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.io.PayloadSlicer;
import org.jclouds.io.internal.BasePayloadSlicer;
import org.jclouds.location.suppliers.LocationsSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;

class AliOSSBlobStoreTest {

    private static final String CONTAINER = "test-bucket";
    private static final String FILENAME = "test-object";
    private static final String REGION = "oss-eu-central-1";
    private static final String PAYLOAD = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt la.";
    private static final Location LOCATION = new LocationBuilder().id(REGION)
                                                                  .scope(LocationScope.REGION)
                                                                  .description(REGION)
                                                                  .build();

    private AliOSSBlobStore aliOSSBlobStore;
    private OSS ossClient;
    private BlobStoreContext context;
    private BlobUtils blobUtils;

    @BeforeEach
    void init() {
        AliOSSApi aliOSSApi = Mockito.mock(AliOSSApi.class);
        ossClient = Mockito.spy(OSS.class);
        Mockito.when(aliOSSApi.getOSSClient())
               .thenReturn(ossClient);

        context = Mockito.mock(BlobStoreContext.class);
        blobUtils = Mockito.mock(BlobUtils.class);
        PayloadSlicer slicer = new BasePayloadSlicer();
        LocationsSupplier locations = () -> Sets.newHashSet(LOCATION);
        Supplier<Location> defaultLocation = () -> LOCATION;
        aliOSSBlobStore = new AliOSSBlobStore(aliOSSApi, context, blobUtils, defaultLocation, locations, slicer);
    }

    @Test
    void testBlobExists() {
        Mockito.when(ossClient.doesObjectExist(CONTAINER, FILENAME))
               .thenReturn(true)
               .thenReturn(false);
        assertTrue(aliOSSBlobStore.blobExists(CONTAINER, FILENAME));
        assertFalse(aliOSSBlobStore.blobExists(CONTAINER, FILENAME));
    }

    @Test
    void testPutBlob() {
        Mockito.when(ossClient.putObject(any()))
               .thenReturn(new PutObjectResult());
        Blob blob = new BlobBuilderImpl().name(FILENAME)
                                         .payload(PAYLOAD)
                                         .userMetadata(getUserMetadata())
                                         .build();
        aliOSSBlobStore.putBlob(CONTAINER, blob);
        Mockito.verify(ossClient)
               .putObject(any(PutObjectRequest.class));
    }

    @Test
    void testGetBlob() throws Exception {
        OSSObject ossObject = new OSSObject();
        ossObject.setKey(FILENAME);
        ossObject.setObjectContent(new ByteArrayInputStream(PAYLOAD.getBytes()));
        Mockito.when(blobUtils.blobBuilder())
               .thenReturn(new BlobBuilderImpl());
        Mockito.when(ossClient.getObject(any(GetObjectRequest.class)))
               .thenReturn(ossObject);
        Blob blob = aliOSSBlobStore.getBlob(CONTAINER, FILENAME);
        String actualPayload = IOUtils.toString(blob.getPayload()
                                                    .openStream(),
                                                StandardCharsets.UTF_8);
        assertEquals(PAYLOAD, actualPayload);
    }

    @Test
    void testRemoveBlob() {
        aliOSSBlobStore.removeBlob(CONTAINER, FILENAME);
        Mockito.verify(ossClient)
               .deleteObject(CONTAINER, FILENAME);
    }

    @Test
    void testList() throws Exception {
        ObjectListing objectListing = new ObjectListing();
        objectListing.setBucketName(CONTAINER);
        objectListing.setObjectSummaries(getObjectSummaries(3));
        Mockito.when(ossClient.listObjects(any(ListObjectsRequest.class)))
               .thenReturn(objectListing);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setUserMetadata(getUserMetadata());

        Mockito.when(ossClient.getObjectMetadata(any(String.class), any(String.class)))
               .thenReturn(objectMetadata);
        Mockito.when(ossClient.generatePresignedUrl(any(), any(), any()))
               .thenReturn(new URL("https://oss-eu-central-1.aliyuncs.com"));
        aliOSSBlobStore.list(CONTAINER, new ListContainerOptions().withDetails())
                       .forEach(storageMetadata -> {
                           assertTrue(storageMetadata.getName()
                                                     .startsWith(FILENAME));
                           assertEquals(PAYLOAD.length(), storageMetadata.getSize());
                           assertTrue(storageMetadata.getETag()
                                                     .startsWith(FILENAME));
                           assertEquals(getUserMetadata(), storageMetadata.getUserMetadata());
                       });
    }

    private Map<String, String> getUserMetadata() {
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put(Constants.FILE_ENTRY_NAME, FILENAME);
        userMetadata.put(Constants.FILE_ENTRY_SPACE, "915a046b-4c7d-44b3-b179-b445c33e5f63");
        userMetadata.put(Constants.FILE_ENTRY_MODIFIED, "1589547494002");
        userMetadata.put(Constants.FILE_ENTRY_NAMESPACE, "b68257d4-4374-424a-9fad-7662628e3bac");
        return userMetadata;
    }

    private List<OSSObjectSummary> getObjectSummaries(int count) {
        List<OSSObjectSummary> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OSSObjectSummary ossObjectSummary = new OSSObjectSummary();
            ossObjectSummary.setBucketName(CONTAINER);
            ossObjectSummary.setETag(FILENAME + "-etag-" + i);
            ossObjectSummary.setKey(FILENAME + i);
            ossObjectSummary.setLastModified(new Date());
            ossObjectSummary.setSize(PAYLOAD.length());
            list.add(ossObjectSummary);
        }
        return list;
    }
}
