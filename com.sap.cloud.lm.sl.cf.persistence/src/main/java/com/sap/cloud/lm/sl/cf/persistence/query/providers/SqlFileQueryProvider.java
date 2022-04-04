package com.sap.cloud.lm.sl.cf.persistence.query.providers;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;

import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.query.SqlQuery;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

public class SqlFileQueryProvider {

    private static final String INSERT_FILE_ATTRIBUTES_AND_CONTENT = "INSERT INTO %s (FILE_ID, SPACE, FILE_NAME, NAMESPACE, FILE_SIZE, DIGEST, DIGEST_ALGORITHM, MODIFIED, CONTENT) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_FILE_ATTRIBUTES = "INSERT INTO %s (FILE_ID, SPACE, FILE_NAME, NAMESPACE, FILE_SIZE, DIGEST, DIGEST_ALGORITHM, MODIFIED) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_FILE = "UPDATE %s SET FILE_SIZE=?, DIGEST=?, DIGEST_ALGORITHM=?, MODIFIED=?, CONTENT=? WHERE FILE_ID=?";
    private static final String SELECT_ALL_FILES = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE FROM %s";
    private static final String SELECT_FILES_BY_NAMESPACE_AND_SPACE = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE FROM %s WHERE NAMESPACE=? AND SPACE=?";
    private static final String SELECT_FILES_BY_SPACE = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE FROM %s WHERE SPACE=?";
    private static final String SELECT_FILE_BY_ID_AND_SPACE = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE FROM %s WHERE FILE_ID=? AND SPACE=?";
    private static final String SELECT_FILE_WITH_CONTENT_BY_ID_AND_SPACE = "SELECT FILE_ID, SPACE, CONTENT FROM %s WHERE FILE_ID=? AND SPACE=?";
    private static final String DELETE_FILES_BY_NAMESPACE_AND_SPACE = "DELETE FROM %s WHERE NAMESPACE=? AND SPACE=?";
    private static final String DELETE_CONTENT_BY_NAMESPACE = "DELETE FROM %s WHERE NAMESPACE=?";
    private static final String DELETE_FILES_BY_SPACE = "DELETE FROM %s WHERE SPACE=?";
    private static final String DELETE_FILES_MODIFIED_BEFORE = "DELETE FROM %s WHERE MODIFIED<?";
    private static final String DELETE_FILE_BY_ID_AND_SPACE = "DELETE FROM %s WHERE FILE_ID=? AND SPACE=?";
    private static final String DELETE_FILES_WITHOUT_CONTENT = "DELETE FROM %s WHERE CONTENT IS NULL";

    private final String tableName;
    private final DataSourceDialect dataSourceDialect;
    private final Logger logger;

    public SqlFileQueryProvider(String tableName, DataSourceDialect dataSourceDialect, Logger logger) {
        this.tableName = tableName;
        this.dataSourceDialect = dataSourceDialect;
        this.logger = logger;
    }

    public SqlQuery<Boolean> getStoreFileQuery(FileEntry fileEntry, InputStream content) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(INSERT_FILE_ATTRIBUTES_AND_CONTENT));
                statement.setString(1, fileEntry.getId());
                statement.setString(2, fileEntry.getSpace());
                statement.setString(3, fileEntry.getName());
                setOrNull(statement, 4, fileEntry.getNamespace());
                getDataSourceDialect().setBigInteger(statement, 5, fileEntry.getSize());
                statement.setString(6, fileEntry.getDigest());
                statement.setString(7, fileEntry.getDigestAlgorithm());
                statement.setTimestamp(8, new Timestamp(fileEntry.getModified()
                                                                 .getTime()));
                getDataSourceDialect().setBlobAsBinaryStream(statement, 9, content);
                return statement.executeUpdate() > 0;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Boolean> getStoreFileAttributesQuery(FileEntry fileEntry) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(INSERT_FILE_ATTRIBUTES));
                statement.setString(1, fileEntry.getId());
                statement.setString(2, fileEntry.getSpace());
                statement.setString(3, fileEntry.getName());
                setOrNull(statement, 4, fileEntry.getNamespace());
                getDataSourceDialect().setBigInteger(statement, 5, fileEntry.getSize());
                statement.setString(6, fileEntry.getDigest());
                statement.setString(7, fileEntry.getDigestAlgorithm());
                statement.setTimestamp(8, new Timestamp(fileEntry.getModified()
                                                                 .getTime()));
                return statement.executeUpdate() > 0;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Boolean> getUpdateFileQuery(FileEntry fileEntry, InputStream content) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(UPDATE_FILE));
                getDataSourceDialect().setBigInteger(statement, 1, fileEntry.getSize());
                statement.setString(2, fileEntry.getDigest());
                statement.setString(3, fileEntry.getDigestAlgorithm());
                statement.setTimestamp(4, new Timestamp(fileEntry.getModified()
                                                                 .getTime()));
                getDataSourceDialect().setBlobAsBinaryStream(statement, 5, content);
                statement.setString(6, fileEntry.getId());
                return statement.executeUpdate() > 0;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<List<FileEntry>> getListFilesQuery(final String space, final String namespace) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<FileEntry> files = new ArrayList<>();
                if (namespace != null) {
                    statement = connection.prepareStatement(getQuery(SELECT_FILES_BY_NAMESPACE_AND_SPACE));
                    statement.setString(1, namespace);
                    statement.setString(2, space);
                } else {
                    statement = connection.prepareStatement(getQuery(SELECT_FILES_BY_SPACE));
                    statement.setString(1, space);
                }
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    files.add(getFileEntry(resultSet));
                }
                return files;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<List<FileEntry>> getListAllFilesQuery() {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<FileEntry> files = new ArrayList<>();
                statement = connection.prepareStatement(getQuery(SELECT_ALL_FILES));
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    files.add(getFileEntry(resultSet));
                }
                return files;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<FileEntry> getRetrieveFileQuery(final String space, final String id) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(getQuery(SELECT_FILE_BY_ID_AND_SPACE));
                statement.setString(1, id);
                statement.setString(2, space);
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return getFileEntry(resultSet);
                }
                return null;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Void> getProcessFileWithContentQuery(final FileDownloadProcessor fileDownloadProcessor) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(getQuery(SELECT_FILE_WITH_CONTENT_BY_ID_AND_SPACE));
                statement.setString(1, fileDownloadProcessor.getFileEntry()
                                                            .getId());
                statement.setString(2, fileDownloadProcessor.getFileEntry()
                                                            .getSpace());
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    processFileContent(resultSet, fileDownloadProcessor);
                } else {
                    throw new SQLException(MessageFormat.format(Messages.FILE_NOT_FOUND, fileDownloadProcessor.getFileEntry()
                                                                                                              .getId()));
                }
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
            return null;
        };
    }

    public SqlQuery<Integer> getDeleteBySpaceAndNamespaceQuery(final String space, final String namespace) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_BY_NAMESPACE_AND_SPACE));
                statement.setString(1, namespace);
                statement.setString(2, space);
                int deletedFiles = statement.executeUpdate();
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITH_SPACE_1_AND_NAMESPACE_2, deletedFiles, space, namespace));
                return deletedFiles;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteProcessLogByNamespaceQuery(final String namespace) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_CONTENT_BY_NAMESPACE));
                statement.setString(1, namespace);
                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };

    }

    public SqlQuery<Integer> getDeleteBySpaceQuery(final String space) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_BY_SPACE));
                statement.setString(1, space);
                int deletedFiles = statement.executeUpdate();
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITH_SPACE_1, deletedFiles, space));
                return deletedFiles;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteModifiedBeforeQuery(Date modificationTime) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_MODIFIED_BEFORE));
                statement.setTimestamp(1, new java.sql.Timestamp(modificationTime.getTime()));
                int deletedFiles = statement.executeUpdate();
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_MODIFIED_BEFORE_1, deletedFiles, modificationTime));
                return deletedFiles;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Boolean> getDeleteFileEntryQuery(final String space, final String id) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILE_BY_ID_AND_SPACE));
                statement.setString(1, id);
                statement.setString(2, space);
                int deletedRows = statement.executeUpdate();
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITH_ID_1_AND_SPACE_2, deletedRows, id, space));
                return deletedRows > 0;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteFileEntriesQuery(List<FileEntry> fileEntries) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILE_BY_ID_AND_SPACE));
                addFileEntriesAsBatches(statement, fileEntries);
                int[] batchResults = statement.executeBatch();
                int deletedEntries = 0;
                for (int batchResult : batchResults) {
                    deletedEntries += batchResult;
                }
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITHOUT_CONTENT, deletedEntries));
                return deletedEntries;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteFilesWithoutContentQuery() {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_WITHOUT_CONTENT));
                int deletedFiles = statement.executeUpdate();
                if (deletedFiles > 0) {
                    logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITHOUT_CONTENT, deletedFiles));
                }
                return deletedFiles;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    protected String getQuery(String statementTemplate) {
        return String.format(statementTemplate, tableName);
    }

    protected void setOrNull(PreparedStatement statement, int position, String value) throws SQLException {
        if (value == null) {
            statement.setNull(position, Types.NULL);
        } else {
            statement.setString(position, value);
        }
    }

    private DataSourceDialect getDataSourceDialect() {
        return dataSourceDialect;
    }

    private void processFileContent(ResultSet resultSet, final FileDownloadProcessor fileDownloadProcessor) throws SQLException {
        InputStream fileStream = getDataSourceDialect().getBinaryStreamFromBlob(resultSet, FileServiceColumnNames.CONTENT);
        try {
            fileDownloadProcessor.processContent(fileStream);
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    logger.error(Messages.UPLOAD_STREAM_FAILED_TO_CLOSE, e);
                }
            }
        }
    }

    private FileEntry getFileEntry(ResultSet resultSet) throws SQLException {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setId(resultSet.getString(FileServiceColumnNames.FILE_ID));
        fileEntry.setDigest(resultSet.getString(FileServiceColumnNames.DIGEST));
        fileEntry.setSpace(resultSet.getString(FileServiceColumnNames.SPACE));
        fileEntry.setName(resultSet.getString(FileServiceColumnNames.FILE_NAME));
        fileEntry.setNamespace(resultSet.getString(FileServiceColumnNames.NAMESPACE));
        fileEntry.setSize(getDataSourceDialect().getBigInteger(resultSet, FileServiceColumnNames.FILE_SIZE));
        fileEntry.setDigestAlgorithm(resultSet.getString(FileServiceColumnNames.DIGEST_ALGORITHM));
        fileEntry.setModified(resultSet.getDate(FileServiceColumnNames.MODIFIED));
        return fileEntry;
    }

    private void addFileEntriesAsBatches(PreparedStatement statement, List<FileEntry> entries) throws SQLException {
        for (FileEntry entry : entries) {
            statement.setString(1, entry.getId());
            statement.setString(2, entry.getSpace());
            statement.addBatch();
        }
    }

    public static class FileServiceColumnNames {
        public static final String CONTENT = "CONTENT";
        public static final String MODIFIED = "MODIFIED";
        public static final String DIGEST_ALGORITHM = "DIGEST_ALGORITHM";
        public static final String FILE_SIZE = "FILE_SIZE";
        public static final String NAMESPACE = "NAMESPACE";
        public static final String SPACE = "SPACE";
        public static final String FILE_NAME = "FILE_NAME";
        public static final String DIGEST = "DIGEST";
        public static final String FILE_ID = "FILE_ID";

        protected FileServiceColumnNames() {
        }
    }
}
