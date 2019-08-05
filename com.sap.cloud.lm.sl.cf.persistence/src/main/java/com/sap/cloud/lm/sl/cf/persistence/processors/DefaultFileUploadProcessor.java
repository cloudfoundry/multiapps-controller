package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.IOException;
import java.io.OutputStream;

import com.sap.cloud.lm.sl.cf.persistence.Constants;

public class DefaultFileUploadProcessor implements FileUploadProcessor {

    @Override
    public int getProcessingBufferSize() {
        return Constants.DEFAULT_BUFFER_SIZE;
    }

    @Override
    public void writeFileChunk(OutputStream outputStream, byte[] data, int readToIndex) throws IOException {
        outputStream.write(data, 0, readToIndex);
    }

}
