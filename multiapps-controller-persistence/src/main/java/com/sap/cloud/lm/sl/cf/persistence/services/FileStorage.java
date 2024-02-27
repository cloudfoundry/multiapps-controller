package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;

public interface FileStorage {

    void addFile(FileEntry fileEntry, InputStream fileStream) throws FileStorageException;

    List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) throws FileStorageException;

    void deleteFile(String id, String space) throws FileStorageException;

    void deleteFilesBySpace(String space) throws FileStorageException;

    void deleteFilesBySpaceAndNamespace(String space, String namespace) throws FileStorageException;

    int deleteFilesModifiedBefore(Date modificationTime) throws FileStorageException;

    void processFileContent(FileDownloadProcessor fileDownloadProcessor) throws FileStorageException;

}
