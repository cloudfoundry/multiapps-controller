package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileInfo;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.BlobSqlFileQueryProvider;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.SqlFileQueryProvider;

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
    public <T> T processFileContent(String space, String id, FileContentProcessor<T> fileContentProcessor) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getProcessFileWithContentQuery(space, id, fileContentProcessor));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    @Override
    public int deleteBySpaceAndNamespace(String space, String namespace) throws FileStorageException {
        return deleteFileAttributesBySpaceAndNamespace(space, namespace);
    }

    @Override
    public int deleteBySpaceIds(List<String> spaceIds) throws FileStorageException {
        return deleteFileAttributesBySpaceIds(spaceIds);
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
