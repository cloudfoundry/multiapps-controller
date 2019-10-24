package com.sap.cloud.lm.sl.cf.process.util;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;

import com.sap.cloud.lm.sl.cf.persistence.services.FileService;

public class ApplicationArchiveContext {
    private final ZipInputStream zipInputStream;
    private final String moduleFileName;
    private final long maxSizeInBytes;
    private long currentSizeInBytes;
    private DigestCalculator applicationDigestCalculator;
    private Set<String> alreadyUploadedFiles;

    public ApplicationArchiveContext(InputStream inputStream, String moduleFileName, long maxSizeInBytes) {
        this.zipInputStream = new ZipInputStream(inputStream);
        this.moduleFileName = moduleFileName;
        this.maxSizeInBytes = maxSizeInBytes;
        createDigestCalculator(FileService.DIGEST_METHOD);
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

    public ZipInputStream getZipInputStream() {
        return zipInputStream;
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

}
