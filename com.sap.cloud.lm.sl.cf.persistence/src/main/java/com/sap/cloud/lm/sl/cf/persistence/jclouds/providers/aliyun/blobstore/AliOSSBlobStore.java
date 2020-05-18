package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore;

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
import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.AliOSSApi;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobAccess;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.ContainerAccess;
import org.jclouds.blobstore.domain.MultipartPart;
import org.jclouds.blobstore.domain.MultipartUpload;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.domain.Tier;
import org.jclouds.blobstore.domain.internal.PageSetImpl;
import org.jclouds.blobstore.domain.internal.StorageMetadataImpl;
import org.jclouds.blobstore.internal.BaseBlobStore;
import org.jclouds.blobstore.options.CreateContainerOptions;
import org.jclouds.blobstore.options.GetOptions;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.blobstore.util.BlobUtils;
import org.jclouds.domain.Location;
import org.jclouds.io.ContentMetadata;
import org.jclouds.io.Payload;
import org.jclouds.io.PayloadSlicer;
import org.jclouds.location.suppliers.LocationsSupplier;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class AliOSSBlobStore extends BaseBlobStore {

    private final AliOSSApi aliOSSApi;

    @Inject
    protected AliOSSBlobStore(AliOSSApi aliOSSApi, BlobStoreContext context, BlobUtils blobUtils, Supplier<Location> defaultLocation,
                              LocationsSupplier locations, PayloadSlicer slicer) {
        super(context, blobUtils, defaultLocation, locations, slicer);
        this.aliOSSApi = aliOSSApi;
    }

    @Override
    public boolean containerExists(String container) {
        return doOssOperation(oss -> oss.doesBucketExist(container));
    }

    @Override
    public PageSet<? extends StorageMetadata> list(String container, ListContainerOptions options) {
        return doOssOperation(oss -> {
            ListObjectsRequest request = toListObjectRequest(container, options);
            ObjectListing objectListing = oss.listObjects(request);
            List<StorageMetadata> storageMetadataList = objectListing.getObjectSummaries()
                                                                     .stream()
                                                                     .map(ossObjectSummary -> {
                StorageType storageType = ossObjectSummary.getKey()
                                                          .endsWith("/") ? StorageType.FOLDER : StorageType.BLOB;
                ObjectMetadata metadata = oss.getObjectMetadata(container, ossObjectSummary.getKey());
                URI url = getPresignedUriForObject(oss, ossObjectSummary);
                return new StorageMetadataImpl(storageType, ossObjectSummary.getKey(), ossObjectSummary.getKey(), defaultLocation.get(),
                                               url, ossObjectSummary.getETag(), ossObjectSummary.getLastModified(),
                                               ossObjectSummary.getLastModified(), metadata.getUserMetadata(), ossObjectSummary.getSize(),
                                               Tier.STANDARD);
            }).collect(Collectors.toList());
            return new PageSetImpl<>(storageMetadataList, objectListing.getNextMarker());
        });
    }

    @Override
    public boolean blobExists(String container, String name) {
        return doOssOperation(oss -> oss.doesObjectExist(container, name));
    }

    @Override
    public String putBlob(String container, Blob blob) {
        return doOssOperation(oss -> {
            try {
                ObjectMetadata objectMetadata = createObjectMetadataFromBlob(blob);
                PutObjectRequest request = new PutObjectRequest(container, blob.getMetadata()
                                                                               .getProviderId(), blob.getPayload()
                                                                                                     .openStream(), objectMetadata);
                PutObjectResult result = oss.putObject(request);
                return result.getETag();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public String putBlob(String container, Blob blob, PutOptions options) {
        return putBlob(container, blob);
    }

    @Override
    public Blob getBlob(String container, String name, GetOptions options) {
        return doOssOperation(oss -> {
            GetObjectRequest req = toGetObjectRequest(container, name, options);
            OSSObject object = oss.getObject(req);
            return convertToBlob(object);
        }, false);

    }

    @Override
    public void removeBlob(String container, String name) {
        doOssOperation(oss -> {
            oss.deleteObject(container, name);
            return null;
        });
    }

    private <R> R doOssOperation(Function<OSS, R> function) {
        return doOssOperation(function, true);
    }

    private <R> R doOssOperation(Function<OSS, R> function, boolean shutdownClient) {
        OSS ossClient = aliOSSApi.getOSSClient();
        R result = function.apply(ossClient);
        if (shutdownClient) {
            ossClient.shutdown();
        }
        return result;
    }

    private GetObjectRequest toGetObjectRequest(String container, String name, GetOptions options) {
        GetObjectRequest request = new GetObjectRequest(container, name);
        if (options.getIfModifiedSince() != null) {
            request.setModifiedSinceConstraint(options.getIfModifiedSince());
        }
        if (!options.getRanges()
                    .isEmpty()) {
            String[] ranges = options.getRanges().get(0).split("-");
            long start = Integer.parseInt(ranges[0]);
            long end = Integer.parseInt(ranges[1]);
            request.setRange(start, end);
        }
        return request;
    }

    private URI getPresignedUriForObject(OSS oss, OSSObjectSummary ossObjectSummary) {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR, time.get(Calendar.HOUR) + 1);
        try {
            return oss.generatePresignedUrl(ossObjectSummary.getBucketName(), ossObjectSummary.getKey(), time.getTime())
                .toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private ListObjectsRequest toListObjectRequest(String container, ListContainerOptions options) {
        ListObjectsRequest request = new ListObjectsRequest(container);
        if (options.getMaxResults() != null) {
            request.setMaxKeys(options.getMaxResults());
        }
        if (options.getMarker() != null) {
            request.setMarker(options.getMarker());
        }
        return request;
    }

    private ObjectMetadata createObjectMetadataFromBlob(Blob blob) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        ContentMetadata blobContentMetadata = blob.getMetadata()
                                                  .getContentMetadata();
        if (blobContentMetadata.getCacheControl() != null) {
            objectMetadata.setCacheControl(blobContentMetadata.getCacheControl());
        }
        if (blobContentMetadata.getContentDisposition() != null) {
            objectMetadata.setContentDisposition(blobContentMetadata.getContentDisposition());
        }
        if (blobContentMetadata.getContentEncoding() != null) {
            objectMetadata.setContentEncoding(blobContentMetadata.getContentEncoding());
        }
        if (blobContentMetadata.getContentLength() != null) {
            objectMetadata.setContentLength(blobContentMetadata.getContentLength());
        }
        if (blobContentMetadata.getContentType() != null) {
            objectMetadata.setContentType(blobContentMetadata.getContentType());
        }
        if (blobContentMetadata.getExpires() != null) {
            objectMetadata.setExpirationTime(blobContentMetadata.getExpires());
        }
        Date lastModified = blob.getMetadata()
                                .getLastModified();
        if (lastModified != null) {
            objectMetadata.setLastModified(lastModified);
        }
        if (blob.getAllHeaders() != null) {
            blob.getAllHeaders()
                .asMap()
                .forEach(objectMetadata::setHeader);
        }
        Map<String, String> userMetadata = blob.getMetadata()
                                               .getUserMetadata();
        if (userMetadata != null) {
            objectMetadata.setUserMetadata(userMetadata);
        }
        return objectMetadata;
    }

    private Blob convertToBlob(OSSObject object) {
        BlobBuilder builder = blobBuilder(object.getKey()).payload(object.getObjectContent());
        Map<String, String> userMetadata = object.getObjectMetadata()
                                                 .getUserMetadata();
        if (userMetadata != null) {
            builder.userMetadata(userMetadata);
        }
        return builder.build();
    }

    // *********************************************
    // *** UNSUPPORTED OR NOT YET SUPPORTED APIS ***
    // *********************************************

    @Override
    protected boolean deleteAndVerifyContainerGone(String container) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PageSet<? extends StorageMetadata> list() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean createContainerInLocation(Location location, String container) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean createContainerInLocation(Location location, String container, CreateContainerOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContainerAccess getContainerAccess(String container) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContainerAccess(String container, ContainerAccess access) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlobMetadata blobMetadata(String container, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlobAccess getBlobAccess(String container, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBlobAccess(String container, String name, BlobAccess access) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultipartUpload initiateMultipartUpload(String container, BlobMetadata blob, PutOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void abortMultipartUpload(MultipartUpload mpu) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String completeMultipartUpload(MultipartUpload mpu, List<MultipartPart> parts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MultipartPart uploadMultipartPart(MultipartUpload mpu, int partNumber, Payload payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MultipartPart> listMultipartUpload(MultipartUpload mpu) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MultipartUpload> listMultipartUploads(String container) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMinimumMultipartPartSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMaximumMultipartPartSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMaximumNumberOfParts() {
        throw new UnsupportedOperationException();
    }
}
