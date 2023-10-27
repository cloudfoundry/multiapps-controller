package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.googlecloudstorage.algorithm;

import static com.google.common.base.Preconditions.checkArgument;

import javax.inject.Named;

import org.jclouds.blobstore.reference.BlobStoreConstants;
import org.jclouds.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

public class MultipartUploadSlicingAlgorithmForGcp {

    private final long minimumPartSize;
    private final long maximumPartSize;
    private final int maximumNumberOfParts;

    @Named(BlobStoreConstants.BLOBSTORE_LOGGER)
    protected Logger logger = Logger.NULL;

    @VisibleForTesting
    public static final long DEFAULT_PART_SIZE = 33554432; // 32MB

    @VisibleForTesting
    static final int DEFAULT_MAGNITUDE_BASE = 100;

    @Inject(optional = true)
    @Named("jclouds.mpu.parts.size")
    @VisibleForTesting
    long defaultPartSize = DEFAULT_PART_SIZE;

    @Inject(optional = true)
    @Named("jclouds.mpu.parts.magnitude")
    @VisibleForTesting
    int magnitudeBase = DEFAULT_MAGNITUDE_BASE;

    // calculated only once, but not from the constructor
    private volatile int parts; // required number of parts with chunkSize
    private volatile long chunkSize;
    private volatile long remaining; // number of bytes remained for the last part

    // sequentially updated values
    private volatile int part;
    private volatile long chunkOffset;
    private volatile long copied;

    public MultipartUploadSlicingAlgorithmForGcp(long minimumPartSize, long maximumPartSize, int maximumNumberOfParts) {
        checkArgument(minimumPartSize > 0);
        this.minimumPartSize = minimumPartSize;
        checkArgument(maximumPartSize > 0);
        this.maximumPartSize = maximumPartSize;
        checkArgument(maximumNumberOfParts > 0);
        this.maximumNumberOfParts = maximumNumberOfParts;
    }

    public long calculateChunkSize(long length) {
        long unitPartSize = defaultPartSize; // first try with default part size
        int parts = (int) (length / unitPartSize);
        long partSize = unitPartSize;
        int magnitude = parts / magnitudeBase;
        if (magnitude > 0) {
            partSize = magnitude * unitPartSize;
            if (partSize > maximumPartSize) {
                partSize = maximumPartSize;
                unitPartSize = maximumPartSize;
            }
            parts = (int) (length / partSize);
            if (parts * partSize < length) {
                partSize = (magnitude + 1) * unitPartSize;
                if (partSize > maximumPartSize) {
                    partSize = maximumPartSize;
                    unitPartSize = maximumPartSize;
                }
                parts = (int) (length / partSize);
            }
        }
        if (partSize < minimumPartSize) {
            partSize = minimumPartSize;
            unitPartSize = minimumPartSize;
            parts = (int) (length / unitPartSize);
        }
        if (partSize > maximumPartSize) {
            partSize = maximumPartSize;
            unitPartSize = maximumPartSize;
            parts = (int) (length / unitPartSize);
        }
        if (parts > maximumNumberOfParts) {
            partSize = length / maximumNumberOfParts;
            unitPartSize = partSize;
            parts = maximumNumberOfParts;
        }
        long remainder = length % unitPartSize;
        if (remainder == 0 && parts > 0) {
            parts -= 1;
        }
        // This condition fixes the following BUG: https://issues.apache.org/jira/browse/JCLOUDS-1606
        if (remainder > 0 && parts == maximumNumberOfParts) {
            parts -= 1;
            partSize = length / parts;
        }
        this.chunkSize = partSize;
        this.parts = parts;
        this.remaining = length - partSize * parts;
        logger.debug(" %d bytes partitioned in %d parts of part size: %d, remaining: %d%s", length, parts, chunkSize, remaining,
                     remaining > maximumPartSize ? " overflow!" : "");
        return this.chunkSize;
    }

    public long getCopied() {
        return copied;
    }

    public void setCopied(long copied) {
        this.copied = copied;
    }

    public int getParts() {
        return parts;
    }

    protected int getNextPart() {
        return ++part;
    }

    public void addCopied(long copied) {
        this.copied += copied;
    }

    protected long getNextChunkOffset() {
        long next = chunkOffset;
        chunkOffset += getChunkSize();
        return next;
    }

    @VisibleForTesting
    protected long getChunkSize() {
        return chunkSize;
    }

    public long getRemaining() {
        return remaining;
    }

}
