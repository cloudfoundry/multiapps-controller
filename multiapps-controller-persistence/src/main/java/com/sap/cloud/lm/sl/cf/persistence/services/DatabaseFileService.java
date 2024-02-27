package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;

public class DatabaseFileService extends FileService {

    public DatabaseFileService(DataSourceWithDialect dataSourceWithDialect) {
        super(dataSourceWithDialect, null);
    }

    public DatabaseFileService(String tableName, DataSourceWithDialect dataSourceWithDialect) {
        super(tableName, dataSourceWithDialect, null);
    }

    @Override
    public void processFileContent(final FileDownloadProcessor fileDownloadProcessor) throws FileStorageException {
        try {
            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getProcessFileWithContentQuery(fileDownloadProcessor));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    public int deleteBySpaceAndNamespace(final String space, final String namespace) throws FileStorageException {
        return deleteFileAttributesBySpaceAndNamespace(space, namespace);
    }

    @Override
    public int deleteBySpace(final String space) throws FileStorageException {
        return deleteFileAttributesBySpace(space);
    }

    @Override
    public int deleteModifiedBefore(Date modificationTime) throws FileStorageException {
        return deleteFileAttributesModifiedBefore(modificationTime);
    }

    @Override
    public boolean deleteFile(final String space, final String id) throws FileStorageException {
        return deleteFileAttribute(space, id);
    }

    @Override
    public int deleteFilesEntriesWithoutContent() throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteFilesWithoutContentQuery());
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    protected void storeFile(FileEntry fileEntry, FileInfo fileinfo) throws FileStorageException {
        try (InputStream fileStream = fileinfo.getInputStream()) {
            storeFileWithContent(fileEntry, fileStream);
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    private boolean storeFileWithContent(FileEntry fileEntry, InputStream fileStream) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileQuery(fileEntry, fileStream));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

}
