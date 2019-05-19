package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.FileOutputStream;
import java.io.IOException;

import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;

public class DefaultFileUploadProcessor implements FileUploadProcessor<FileOutputStream, FileOutputStream> {

    @Override
    public int getProcessingBufferSize() {
        return FileContentProcessor.DEFAULT_BUFFER_SIZE;
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
