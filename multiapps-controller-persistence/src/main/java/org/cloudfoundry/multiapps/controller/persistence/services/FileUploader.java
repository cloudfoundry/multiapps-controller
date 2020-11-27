package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

import javax.xml.bind.DatatypeConverter;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUploader {

    private static final String EXTENSION = "tmp";
    private static final String PREFIX = "fileUpload";
    private static final int BUFFER_SIZE = 4 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploader.class);

    private FileUploader() {
    }

    /**
     * Uploads file.
     * 
     * @param is input stream
     * @return uploaded file
     * @throws FileStorageException if the file cannot be uploaded
     */
    public static FileInfo uploadFile(InputStream is) throws FileStorageException {
        BigInteger size = BigInteger.valueOf(0);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(Constants.DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException(e);
        }

        File tempFile;
        try {
            tempFile = File.createTempFile(PREFIX, EXTENSION);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }

        // store the passed input to the file system
        try (OutputStream outputFileStream = new FileOutputStream(tempFile)) {
            int read = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((read = is.read(buffer)) > -1) {
                outputFileStream.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size = size.add(BigInteger.valueOf(read));
            }
        } catch (Exception e) {
            deleteFile(tempFile);
            throw new FileStorageException(e);
        }

        return ImmutableFileInfo.builder()
                                .file(tempFile)
                                .size(size)
                                .digest(DatatypeConverter.printHexBinary(digest.digest()))
                                .digestAlgorithm(Constants.DIGEST_ALGORITHM)
                                .build();
    }

    public static void removeFile(FileInfo uploadedFile) {
        File file = uploadedFile.getFile();
        deleteFile(file);
    }

    private static void deleteFile(File file) {
        Path filePath = file.toPath();
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            LOGGER.warn(MessageFormat.format(Messages.FAILED_TO_DELETE_FILE, filePath), e);
        }
    }

}
