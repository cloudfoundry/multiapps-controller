package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.xml.bind.DatatypeConverter;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dialects.DataSourceDialect;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.persistence.stream.DBInputStream;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;
import org.slf4j.Logger;

public abstract class SqlFileQueryProvider {

    private static final String INSERT_FILE_ATTRIBUTES_AND_CONTENT_WITHOUT_DIGEST = "INSERT INTO %s (FILE_ID, SPACE, FILE_NAME, NAMESPACE, FILE_SIZE, DIGEST_ALGORITHM, MODIFIED, OPERATION_ID, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_FILE_DIGEST = "UPDATE %s SET DIGEST = ? WHERE FILE_ID = ?";
    private static final String UPDATE_FILES_OPERATION_ID = "UPDATE %s SET OPERATION_ID = ? where FILE_ID = ANY(?)";
    private static final String INSERT_FILE_ATTRIBUTES = "INSERT INTO %s (FILE_ID, SPACE, FILE_NAME, NAMESPACE, FILE_SIZE, DIGEST, DIGEST_ALGORITHM, MODIFIED, OPERATION_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_FILES_WITHOUT_OPERATION_CREATED_AFTER_TIME1_AND_BEFORE_TIME2 = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE MODIFIED > ? AND MODIFIED < ? AND OPERATION_ID ISNULL";
    private static final String SELECT_ALL_FILES = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s";
    private static final String SELECT_FILES_BY_NAMESPACE_AND_SPACE_ID = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE NAMESPACE=? AND SPACE=?";
    private static final String SELECT_FILES_BY_SPACE_ID_WITH_NO_NAMESPACE = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE SPACE=? AND NAMESPACE IS NULL";
    private static final String SELECT_FILES_BY_SPACE_ID_AND_OPERATION_ID = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE SPACE=? AND OPERATION_ID=?";
    private static final String SELECT_FILES_BY_SPACE_ID = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE SPACE=?";
    private static final String SELECT_FILE_BY_ID_AND_SPACE_ID = "SELECT FILE_ID, SPACE, DIGEST, DIGEST_ALGORITHM, MODIFIED, FILE_NAME, NAMESPACE, FILE_SIZE, OPERATION_ID FROM %s WHERE FILE_ID=? AND SPACE=?";
    private static final String SELECT_FILE_WITH_CONTENT_BY_ID_AND_SPACE_ID = "SELECT FILE_ID, SPACE, %s FROM %s WHERE FILE_ID=? AND SPACE=?";
    private static final String DELETE_FILES_BY_NAMESPACE_AND_SPACE_ID = "DELETE FROM %s WHERE NAMESPACE=? AND SPACE=?";
    private static final String DELETE_FILES_BY_SPACE_ID = "DELETE FROM %s WHERE SPACE=?";
    private static final String DELETE_FILES_BY_IDS = "DELETE FROM %s WHERE FILE_ID = ANY(?)";
    private static final String DELETE_FILES_MODIFIED_BEFORE = "DELETE FROM %s WHERE MODIFIED<?";
    private static final String DELETE_FILE_BY_ID_AND_SPACE = "DELETE FROM %s WHERE FILE_ID=? AND SPACE=?";
    private static final String DELETE_FILES_WITHOUT_CONTENT = "DELETE FROM %s WHERE CONTENT IS NULL";

    private final String tableName;
    private final DataSourceDialect dataSourceDialect;
    private Logger logger;

    protected SqlFileQueryProvider(String tableName, DataSourceDialect dataSourceDialect) {
        this.tableName = tableName;
        this.dataSourceDialect = dataSourceDialect;
    }

    public SqlQuery<String> getStoreFileAndComputeDigestQuery(FileEntry entryWithoutDigest, InputStream content) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try (DigestInputStream dis = new DigestInputStream(content, MessageDigest.getInstance(Constants.DIGEST_ALGORITHM))) {
                statement = connection.prepareStatement(getInsertWithContentWithoutDigestQuery());
                statement.setString(1, entryWithoutDigest.getId());
                statement.setString(2, entryWithoutDigest.getSpace());
                statement.setString(3, entryWithoutDigest.getName());
                setOrNull(statement, 4, entryWithoutDigest.getNamespace());
                getDataSourceDialect().setBigInteger(statement, 5, entryWithoutDigest.getSize());
                statement.setString(6, Constants.DIGEST_ALGORITHM);
                statement.setTimestamp(7, Timestamp.valueOf(entryWithoutDigest.getModified()));
                statement.setString(8, entryWithoutDigest.getOperationId());
                setContentBinaryStream(statement, 9, dis);
                statement.executeUpdate();

                String digest = DatatypeConverter.printHexBinary(dis.getMessageDigest()
                                                                    .digest());
                statement = connection.prepareStatement(getUpdateDigestQuery());
                statement.setString(1, digest);
                statement.setString(2, entryWithoutDigest.getId());
                statement.executeUpdate();

                return digest;
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getUpdateFilesOperationIdQuery(List<String> filesIds, String operationId) {
        return connection -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(UPDATE_FILES_OPERATION_ID));
                statement.setString(1, operationId);
                statement.setArray(2, connection.createArrayOf("VARCHAR", filesIds.toArray(new String[0])));
                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    protected abstract void setContentBinaryStream(PreparedStatement statement, int index, InputStream content) throws SQLException;

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
                statement.setTimestamp(8, Timestamp.valueOf(fileEntry.getModified()));
                statement.setString(9, fileEntry.getOperationId());
                return statement.executeUpdate() > 0;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<List<FileEntry>> getListFilesQuery(String space, String namespace) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<FileEntry> files = new ArrayList<>();
                statement = getFilesStatementBasedOnNamespace(connection, space, namespace);
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

    public SqlQuery<List<FileEntry>> getListFilesBySpaceAndOperationId(String space, String operationId) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<FileEntry> files = new ArrayList<>();
                statement = connection.prepareStatement(getQuery(SELECT_FILES_BY_SPACE_ID_AND_OPERATION_ID));
                statement.setString(1, space);
                statement.setString(2, operationId);
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

    public SqlQuery<List<FileEntry>> getListFilesCreatedAfterAndBeforeWithoutOperationQuery(LocalDateTime after, LocalDateTime before) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<FileEntry> files = new ArrayList<>();
                statement = connection.prepareStatement(getSelectFilesWithoutOperationCreatedAfterTime1AndBeforeTime2Query());
                statement.setTimestamp(1, Timestamp.valueOf(after));
                statement.setTimestamp(2, Timestamp.valueOf(before));
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

    protected String getSelectFilesWithoutOperationCreatedAfterTime1AndBeforeTime2Query() {
        return getQuery(SELECT_FILES_WITHOUT_OPERATION_CREATED_AFTER_TIME1_AND_BEFORE_TIME2);
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

    public SqlQuery<FileEntry> getRetrieveFileQuery(String space, String id) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(getQuery(SELECT_FILE_BY_ID_AND_SPACE_ID));
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

    public SqlQuery<DBInputStream> openFileWithContentQuery(String space, String id) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(getSelectWithContentQuery());
                statement.setString(1, id);
                statement.setString(2, space);
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new DBInputStream(getContentBinaryStream(resultSet, getContentColumnName()),
                                                       statement,
                                                       resultSet,
                                                       connection);
                } else {
                    throw new SQLException(MessageFormat.format(Messages.FILE_NOT_FOUND, id));
                }
            } catch (Exception e) {
                JdbcUtil.closeQuietly(statement);
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(connection);
                throw new SQLException(e.getMessage(), e);
            }
        };
    }

    public <T> SqlQuery<T> getProcessFileWithContentQuery(String space, String id, FileContentProcessor<T> fileContentProcessor) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(getSelectWithContentQuery());
                statement.setString(1, id);
                statement.setString(2, space);
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return processFileContent(resultSet, fileContentProcessor);
                } else {
                    throw new SQLException(MessageFormat.format(Messages.FILE_NOT_FOUND, id));
                }
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteBySpaceAndNamespaceQuery(String space, String namespace) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_BY_NAMESPACE_AND_SPACE_ID));
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

    public SqlQuery<Integer> getDeleteBySpaceIdsQuery(List<String> spaceIds) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_BY_SPACE_ID));
                addSpaceIdsAsBatches(statement, spaceIds);
                int[] batchResults = statement.executeBatch();
                int deletedFiles = IntStream.of(batchResults)
                                            .sum();
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_WITH_SPACEIDS_1, deletedFiles, spaceIds));
                return deletedFiles;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteByFileIdsQuery(List<String> fileIds) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_BY_IDS));
                statement.setArray(1, connection.createArrayOf("VARCHAR", fileIds.toArray(new String[0])));
                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Integer> getDeleteModifiedBeforeQuery(LocalDateTime modificationTime) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getQuery(DELETE_FILES_MODIFIED_BEFORE));
                statement.setTimestamp(1, Timestamp.valueOf(modificationTime));
                int deletedFiles = statement.executeUpdate();
                logger.debug(MessageFormat.format(Messages.DELETED_0_FILES_MODIFIED_BEFORE_1, deletedFiles, modificationTime));
                return deletedFiles;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<Boolean> getDeleteFileEntryQuery(String space, String id) {
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

    private String getQuery(String statementTemplate) {
        return String.format(statementTemplate, tableName);
    }

    private String getInsertWithContentWithoutDigestQuery() {
        return String.format(INSERT_FILE_ATTRIBUTES_AND_CONTENT_WITHOUT_DIGEST, tableName, getContentColumnName());
    }

    private String getUpdateDigestQuery() {
        return String.format(UPDATE_FILE_DIGEST, tableName);
    }

    private String getSelectWithContentQuery() {
        return String.format(SELECT_FILE_WITH_CONTENT_BY_ID_AND_SPACE_ID, getContentColumnName(), tableName);
    }

    protected String getContentColumnName() {
        return Constants.FILE_ENTRY_CONTENT;
    }

    protected void setOrNull(PreparedStatement statement, int position, String value) throws SQLException {
        if (value == null) {
            statement.setNull(position, Types.NULL);
        } else {
            statement.setString(position, value);
        }
    }

    protected DataSourceDialect getDataSourceDialect() {
        return dataSourceDialect;
    }

    private <T> T processFileContent(ResultSet resultSet, FileContentProcessor<T> fileContentProcessor) throws SQLException {
        InputStream fileStream = getContentBinaryStream(resultSet, getContentColumnName());
        try {
            return fileContentProcessor.process(fileStream);
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

    protected abstract InputStream getContentBinaryStream(ResultSet resultSet, String columnName) throws SQLException;

    private PreparedStatement getFilesStatementBasedOnNamespace(Connection connection, String space, String namespace) throws SQLException {
        PreparedStatement statement;

        if (namespace == null) {
            statement = connection.prepareStatement(getQuery(SELECT_FILES_BY_SPACE_ID));
            statement.setString(1, space);
        } else if (namespace.equals("")) {
            statement = connection.prepareStatement(getQuery(SELECT_FILES_BY_SPACE_ID_WITH_NO_NAMESPACE));
            statement.setString(1, space);
        } else {
            statement = connection.prepareStatement(getQuery(SELECT_FILES_BY_NAMESPACE_AND_SPACE_ID));
            statement.setString(1, namespace);
            statement.setString(2, space);
        }

        return statement;
    }

    private FileEntry getFileEntry(ResultSet resultSet) throws SQLException {
        Timestamp modifiedAsTimestamp = resultSet.getTimestamp(Constants.FILE_ENTRY_MODIFIED);
        return ImmutableFileEntry.builder()
                                 .id(resultSet.getString(Constants.FILE_ENTRY_ID))
                                 .digest(resultSet.getString(Constants.FILE_ENTRY_DIGEST))
                                 .digestAlgorithm(resultSet.getString(Constants.FILE_ENTRY_DIGEST_ALGORITHM))
                                 .name(resultSet.getString(Constants.FILE_ENTRY_NAME))
                                 .namespace(resultSet.getString(Constants.FILE_ENTRY_NAMESPACE))
                                 .space(resultSet.getString(Constants.FILE_ENTRY_SPACE))
                                 .modified(modifiedAsTimestamp.toInstant()
                                                              .atZone(ZoneId.systemDefault())
                                                              .toLocalDateTime())
                                 .size(getDataSourceDialect().getBigInteger(resultSet, Constants.FILE_ENTRY_SIZE))
                                 .operationId(resultSet.getString(Constants.FILE_ENTRY_OPERATION_ID))
                                 .build();
    }

    private void addSpaceIdsAsBatches(PreparedStatement statement, List<String> spaceIds) throws SQLException {
        for (String spaceId : spaceIds) {
            statement.setString(1, spaceId);
            statement.addBatch();
        }
    }

    public SqlFileQueryProvider withLogger(Logger logger) {
        this.logger = logger;
        return this;
    }
}
