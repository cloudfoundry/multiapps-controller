package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;

/*
 * Provides functionality for uploading a file to the server and reading uploaded file contents
 *
 */
public class DatabaseFileService extends AbstractFileService {

    public DatabaseFileService(DataSourceWithDialect dataSourceWithDialect) {
        this(DEFAULT_TABLE_NAME, dataSourceWithDialect);
    }

    protected DatabaseFileService(String tableName, DataSourceWithDialect dataSourceWithDialect) {
        super(tableName, dataSourceWithDialect);
    }

    @Override
    protected boolean storeFile(final FileEntry fileEntry, final InputStream inputStream) throws FileStorageException {
        try {
            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileQuery(fileEntry, inputStream));
            return true;
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void processFileContent(final FileDownloadProcessor fileDownloadProcessor) throws FileStorageException {
        deleteFilesWithoutContent();
        try {
            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getProcessFileContentQuery(fileDownloadProcessor));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteBySpace(String space) throws FileStorageException {
    }

    @Override
    protected void deleteFileContent(String space, String id) throws FileStorageException {
        /*
         * The implementation of this method is empty because the content of the file is being deleted by the abstract file service using a
         * DELETE query for the whole file record
         */
    }

    @Override
    public List<FileEntry> listFiles(String space, String namespace) throws FileStorageException {
        deleteFilesWithoutContent();
        return super.listFiles(space, namespace);
    }

    @Override
    public FileEntry getFile(String space, String id) throws FileStorageException {
        deleteFilesWithoutContent();
        return super.getFile(space, id);
    }

    protected int deleteFilesWithoutContent() throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteFilesWithoutContentQuery());
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

}
