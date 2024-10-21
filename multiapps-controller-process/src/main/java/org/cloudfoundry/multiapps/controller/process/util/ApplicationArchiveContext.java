package org.cloudfoundry.multiapps.controller.process.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.process.stream.ArchiveEntryWithStreamPositions;

public class ApplicationArchiveContext {

    private final String moduleFileName;
    private final long maxSizeInBytes;
    private final List<ArchiveEntryWithStreamPositions> archiveEntryWithStreamPositions;
    private final String spaceId;
    private final String appArchiveId;
    private long currentSizeInBytes;
    private DigestCalculator applicationDigestCalculator;
    private Set<String> alreadyUploadedFiles;

    public ApplicationArchiveContext(String moduleFileName, long maxSizeInBytes,
                                     List<ArchiveEntryWithStreamPositions> archiveEntryWithStreamPositions, String spaceId,
                                     String appArchiveId) {
        this.moduleFileName = moduleFileName;
        this.maxSizeInBytes = maxSizeInBytes;
        this.archiveEntryWithStreamPositions = archiveEntryWithStreamPositions;
        this.spaceId = spaceId;
        this.appArchiveId = appArchiveId;
        createDigestCalculator(Constants.DIGEST_ALGORITHM);
    }

    private void createDigestCalculator(String algorithm) {
        try {
            this.applicationDigestCalculator = new DigestCalculator(MessageDigest.getInstance(algorithm));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public long getCurrentSizeInBytes() {
        return currentSizeInBytes;
    }

    public void calculateCurrentSizeInBytes(long sizeInBytes) {
        currentSizeInBytes += sizeInBytes;
    }

    public String getModuleFileName() {
        return moduleFileName;
    }

    public long getMaxSizeInBytes() {
        return maxSizeInBytes;
    }

    public DigestCalculator getApplicationDigestCalculator() {
        return applicationDigestCalculator;
    }

    public Set<String> getAlreadyUploadedFiles() {
        if (alreadyUploadedFiles == null) {
            return new HashSet<>();
        }
        return alreadyUploadedFiles;
    }

    public void setAlreadyUploadedFiles(Set<String> alreadyUploadedFiles) {
        this.alreadyUploadedFiles = alreadyUploadedFiles;
    }

    public List<ArchiveEntryWithStreamPositions> getArchiveEntryWithStreamPositions() {
        return archiveEntryWithStreamPositions;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getAppArchiveId() {
        return appArchiveId;
    }
}
