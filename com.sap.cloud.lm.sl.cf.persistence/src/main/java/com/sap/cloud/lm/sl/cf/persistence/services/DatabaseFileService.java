package com.sap.cloud.lm.sl.cf.persistence.services;

import java.sql.SQLException;
import java.util.Date;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.query.SqlQuery;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.BlobSqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.stream.AnalyzingInputStream;

public class DatabaseFileService extends FileService {

    public DatabaseFileService(DataSourceWithDialect dataSourceWithDialect) {
        this(DEFAULT_TABLE_NAME, dataSourceWithDialect);
    }

    public DatabaseFileService(String tableName, DataSourceWithDialect dataSourceWithDialect) {
        this(dataSourceWithDialect, new BlobSqlFileQueryProvider(tableName, dataSourceWithDialect.getDataSourceDialect()));
    }

    protected DatabaseFileService(DataSourceWithDialect dataSourceWithDialect, SqlFileQueryProvider sqlFileQueryProvider) {
        super(dataSourceWithDialect, sqlFileQueryProvider, null);
    }

    @Override
    public void processFileContent(String space, String id, FileContentProcessor fileContentProcessor) throws FileStorageException {
        try {
            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getProcessFileWithContentQuery(space, id, fileContentProcessor));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    public int deleteBySpaceAndNamespace(String space, String namespace) throws FileStorageException {
        return deleteFileAttributesBySpaceAndNamespace(space, namespace);
    }

    @Override
    public int deleteBySpace(String space) throws FileStorageException {
        return deleteFileAttributesBySpace(space);
    }

    @Override
    public int deleteModifiedBefore(Date modificationTime) throws FileStorageException {
        return deleteFileAttributesModifiedBefore(modificationTime);
    }

    @Override
    public boolean deleteFile(String space, String id) throws FileStorageException {
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
    protected FileEntry storeFile(FileEntry fileEntry, AnalyzingInputStream inputStream) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getStoreFileSqlQuery(fileEntry, inputStream));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    private SqlQuery<FileEntry> getStoreFileSqlQuery(FileEntry fileEntry, AnalyzingInputStream inputStream) {
        return connection -> {
            getSqlFileQueryProvider().getInsertFileQuery(fileEntry, inputStream)
                                     .execute(connection);
            FileEntry updatedFileEntry = updateFileEntry(fileEntry, inputStream);
            getSqlFileQueryProvider().getUpdateFileAttributesQuery(updatedFileEntry)
                                     .execute(connection);
            return updatedFileEntry;
        };
    }

}
