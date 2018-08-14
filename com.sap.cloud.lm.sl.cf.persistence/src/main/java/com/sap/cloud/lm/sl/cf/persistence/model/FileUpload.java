package com.sap.cloud.lm.sl.cf.persistence.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;

import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

public class FileUpload {
    private final BigInteger size;
    private final String digest;
    private final String digestAlgorithm;
    private final File file;

    public FileUpload(File file, BigInteger size, String digest, String digestAlgorithm) {
        super();
        this.file = file;
        this.size = size;
        this.digest = digest;
        this.digestAlgorithm = digestAlgorithm;
    }

    public BigInteger getSize() {
        return size;
    }

    public String getDigest() {
        return digest;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public InputStream getInputStream() throws FileStorageException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new FileStorageException(e);
        }
    }

    public File getFile() {
        return file;
    }

}
