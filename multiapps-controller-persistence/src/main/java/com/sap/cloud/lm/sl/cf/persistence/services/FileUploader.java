package com.sap.cloud.lm.sl.cf.persistence.services;

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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;

public class FileUploader {

    public static final String DIGEST_METHOD = "MD5";
    private static final String EXTENSION = "tmp";
    private static final String PREFIX = "fileUpload";
    private static final Logger logger = LoggerFactory.getLogger(FileUploader.class);

    private FileUploader() {
    }

    /**
     * Uploads file.
     * 
     * @param is input stream
     * @param fileUploadProcessor file upload processor
     * @return uploaded file
     * @throws FileStorageException
     */
    @SuppressWarnings({ "unchecked" })
    public static FileInfo uploadFile(InputStream is, @SuppressWarnings("rawtypes") FileUploadProcessor fileUploadProcessor)
        throws FileStorageException {
        BigInteger size = BigInteger.valueOf(0);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(DIGEST_METHOD);
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException(e);
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile(PREFIX, EXTENSION);
        } catch (IOException e) {
            throw new FileStorageException(e);
        }

        // store the passed input to the file system
        OutputStream outputFileStream = null;
        OutputStream outputWrapperStream = null;
        try {
            outputFileStream = new FileOutputStream(tempFile);
            outputWrapperStream = fileUploadProcessor.createOutputStreamWrapper(outputFileStream);
            int read = 0;
            byte[] buffer = new byte[getProcessingBufferSize(fileUploadProcessor)];
            while ((read = is.read(buffer, 0, getProcessingBufferSize(fileUploadProcessor))) > -1) {
                fileUploadProcessor.writeFileChunk(outputWrapperStream, buffer, read);
                digest.update(buffer, 0, read); // original file digest
                size = size.add(BigInteger.valueOf(read)); // original file size
            }
        } catch (IOException e) {
            if (tempFile != null) {
                FileUploader.deleteFile(tempFile);
            }
            throw new FileStorageException(e);
        } finally {
            IOUtils.closeQuietly(outputWrapperStream);
            IOUtils.closeQuietly(outputFileStream);
        }

        return new FileInfo(tempFile, size, getDigestString(digest.digest()), DIGEST_METHOD);
    }

    private static int getProcessingBufferSize(@SuppressWarnings("rawtypes") FileUploadProcessor fileUploadProcessor) {
        int processingBufferSize = fileUploadProcessor.getProcessingBufferSize();
        if (processingBufferSize <= 0) {
            return FileContentProcessor.DEFAULT_BUFFER_SIZE;
        }
        return processingBufferSize;
    }

    private static String getDigestString(byte[] digest) {
        return DatatypeConverter.printHexBinary(digest);
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
            logger.warn(MessageFormat.format(Messages.FAILED_TO_DELETE_FILE, filePath), e);
        }
    }

}
