package com.sap.cloud.lm.sl.cf.persistence.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;

import org.immutables.value.Value;

import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

@Value.Immutable
public abstract class FileInfo {

    public abstract BigInteger getSize();

    public abstract String getDigest();

    public abstract String getDigestAlgorithm();

    public InputStream getInputStream() throws FileStorageException {
        try {
            return new FileInputStream(getFile());
        } catch (FileNotFoundException e) {
            throw new FileStorageException(e);
        }
    }

    public abstract File getFile();

}
