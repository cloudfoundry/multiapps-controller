package com.sap.cloud.lm.sl.cf.persistence.processors;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

public class DefaultFileDownloadProcessor implements FileDownloadProcessor {

    private FileContentProcessor fileContentProcessor = null;
    private FileEntry fileEntry = null;

    public DefaultFileDownloadProcessor(String space, String fileId, FileContentProcessor fileContentProcessor) {
        this(createFileEntry(space, fileId), fileContentProcessor);
    }

    public DefaultFileDownloadProcessor(FileEntry fileEntry, FileContentProcessor fileContentProcessor) {
        this.fileContentProcessor = fileContentProcessor;
        this.fileEntry = fileEntry;
    }

    private static FileEntry createFileEntry(String space, String fileId) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setSpace(space);
        fileEntry.setId(fileId);
        return fileEntry;
    }

    @Override
    public void processContent(InputStream is) throws NoSuchAlgorithmException, IOException, FileStorageException {
        this.fileContentProcessor.processFileContent(is);
    }

    @Override
    public FileEntry getFileEntry() {
        return this.fileEntry;
    }

}
