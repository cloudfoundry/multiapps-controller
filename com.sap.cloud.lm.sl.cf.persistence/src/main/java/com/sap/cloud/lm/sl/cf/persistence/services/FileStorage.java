package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.util.Date;
import java.util.List;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;

public interface FileStorage {

    void addFile(FileEntry fileEntry, File file) throws FileStorageException;

    List<FileEntry> getFileEntriesWithoutContent(List<FileEntry> fileEntries) throws FileStorageException;

    void deleteFile(String id, String space) throws FileStorageException;

    void deleteFilesBySpace(String space) throws FileStorageException;

    void deleteFilesBySpaceAndNamespace(String space, String namespace);

    int deleteFilesModifiedBefore(Date modificationTime) throws FileStorageException;

    void processFileContent(String space, String id, FileContentProcessor fileContentProcessor) throws FileStorageException;

}
