package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultFileUploadProcessor implements FileUploadProcessor<FileOutputStream, FileOutputStream> {

    private boolean shouldScanFile = true; // default

    public DefaultFileUploadProcessor(BigInteger maxUploadSize) {
        this(true);
    }

    public DefaultFileUploadProcessor(boolean shouldScanFile) {
        this.shouldScanFile = shouldScanFile;
    }

    @Override
    public int getProcessingBufferSize() {
        return FileContentProcessor.DEFAULT_BUFFER_SIZE;
    }

    @Override
    public boolean shouldScanFile() {
        return this.shouldScanFile;
    }

    @Override
    public FileOutputStream createOutputStreamWrapper(FileOutputStream outputStream) throws IOException {
        return outputStream;
    }

    @Override
    public void writeFileChunk(FileOutputStream outputStreamWrapper, byte[] data, int readToIndex) throws IOException {
        outputStreamWrapper.write(data, 0, readToIndex);
    }

}
