package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableFileEntry;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.ExternalSqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.stream.AnalyzingInputStream;
import com.sap.cloud.lm.sl.cf.persistence.util.SqlQueryExecutor;

public class FileService {

    protected static final String DEFAULT_TABLE_NAME = "LM_SL_PERSISTENCE_FILE";
    public static final String DIGEST_METHOD = "MD5";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final FileStorage fileStorage;
    private final SqlQueryExecutor sqlQueryExecutor;
    private final SqlFileQueryProvider sqlFileQueryProvider;

    public FileService(DataSourceWithDialect dataSourceWithDialect, FileStorage fileStorage) {
        this(DEFAULT_TABLE_NAME, dataSourceWithDialect, fileStorage);
    }

    public FileService(String tableName, DataSourceWithDialect dataSourceWithDialect, FileStorage fileStorage) {
        this(dataSourceWithDialect, new ExternalSqlFileQueryProvider(tableName, dataSourceWithDialect.getDataSourceDialect()), fileStorage);
    }

    protected FileService(DataSourceWithDialect dataSourceWithDialect, SqlFileQueryProvider sqlFileQueryProvider, FileStorage fileStorage) {
        this.sqlQueryExecutor = new SqlQueryExecutor(dataSourceWithDialect.getDataSource());
        this.sqlFileQueryProvider = sqlFileQueryProvider.withLogger(logger);
        this.fileStorage = fileStorage;
    }

    public FileEntry addFile(String space, String name, InputStream inputStream) throws FileStorageException {
        return addFile(space, null, name, inputStream);
    }

    /**
     * Uploads a new file.
     *
     * @param space
     * @param namespace namespace where the file will be uploaded
     * @param name name of the uploaded file
     * @param fileUploadProcessor file processor
     * @param inputStream input stream to read the content from
     * @return an object representing the file upload
     * @throws FileStorageException
     */
    public FileEntry addFile(String space, String namespace, String name, InputStream inputStream) throws FileStorageException {
        try (InputStream autoClosedInputStream = inputStream) {
            FileEntry fileEntry = createFileEntry(space, namespace, name);
            fileEntry = storeFile(fileEntry, inputStream);
            logger.debug(MessageFormat.format(Messages.STORED_FILE_0, fileEntry));
            return fileEntry;
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
            return null;
        }
    }

    public FileEntry addFile(String space, String namespace, String name, File existingFile) throws FileStorageException {
        try {
            return addFile(space, namespace, name, new FileInputStream(existingFile));
        } catch (FileNotFoundException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_FINDING_FILE_TO_UPLOAD, existingFile.getName()), e);
        }
    }

    public List<FileEntry> listFiles(String space, String namespace) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListFilesQuery(space, namespace));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_FILES_WITH_SPACE_AND_NAMESPACE, space, namespace),
                                           e);
        }
    }

    public FileEntry getFile(String space, String id) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getRetrieveFileQuery(space, id));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    /**
     * Reads file from the storage.
     *
     * @param fileDownloadProcessor file processor
     * @throws FileStorageException
     */
    public void processFileContent(String space, String id, FileContentProcessor fileContentProcessor) throws FileStorageException {
        fileStorage.processFileContent(space, id, fileContentProcessor);
    }

    public int deleteBySpaceAndNamespace(String space, String namespace) throws FileStorageException {
        fileStorage.deleteFilesBySpaceAndNamespace(space, namespace);
        return deleteFileAttributesBySpaceAndNamespace(space, namespace);
    }

    public int deleteBySpace(String space) throws FileStorageException {
        fileStorage.deleteFilesBySpace(space);
        return deleteFileAttributesBySpace(space);
    }

    public int deleteModifiedBefore(Date modificationTime) throws FileStorageException {
        int deletedItems = fileStorage.deleteFilesModifiedBefore(modificationTime);
        return deleteFileAttributesModifiedBefore(modificationTime) + deletedItems;
    }

    public boolean deleteFile(String space, String id) throws FileStorageException {
        fileStorage.deleteFile(id, space);
        return deleteFileAttribute(space, id);
    }

    public int deleteFilesEntriesWithoutContent() throws FileStorageException {
        try {
            List<FileEntry> entries = getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListAllFilesQuery());
            List<FileEntry> missing = fileStorage.getFileEntriesWithoutContent(entries);
            return deleteFileEntries(missing);
        } catch (SQLException e) {
            throw new FileStorageException(Messages.ERROR_GETTING_ALL_FILES, e);
        }
    }

    private FileEntry storeFile(FileEntry fileEntry, InputStream inputStream) throws FileStorageException, IOException {
        try (AnalyzingInputStream analyzingInputStream = new AnalyzingInputStream(inputStream, MessageDigest.getInstance(DIGEST_METHOD))) {
            return storeFile(fileEntry, analyzingInputStream);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(MessageFormat.format(Messages.COULD_NOT_COMPUTE_DIGEST_OF_STREAM, e.getMessage()), e);
        }
    }

    protected FileEntry storeFile(FileEntry fileEntry, AnalyzingInputStream inputStream) throws FileStorageException {
        fileStorage.addFile(fileEntry, inputStream);
        FileEntry updatedFileEntry = updateFileEntry(fileEntry, inputStream);
        storeFileAttributes(updatedFileEntry);
        return updatedFileEntry;
    }

    protected ImmutableFileEntry updateFileEntry(FileEntry fileEntry, AnalyzingInputStream analyzingInputStream) {
        return ImmutableFileEntry.builder()
                                 .from(fileEntry)
                                 .size(BigInteger.valueOf(analyzingInputStream.getByteCount()))
                                 .digest(DatatypeConverter.printHexBinary(analyzingInputStream.getMessageDigest()
                                                                                              .digest()))
                                 .digestAlgorithm(DIGEST_METHOD)
                                 .build();
    }

    protected boolean deleteFileAttribute(String space, String id) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteFileEntryQuery(space, id));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    protected int deleteFileAttributesModifiedBefore(Date modificationTime) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteModifiedBeforeQuery(modificationTime));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    protected int deleteFileAttributesBySpaceAndNamespace(String space, String namespace) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteBySpaceAndNamespaceQuery(space, namespace));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    protected int deleteFileAttributesBySpace(String space) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteBySpaceQuery(space));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    protected FileEntry createFileEntry(String space, String namespace, String name) {
        return ImmutableFileEntry.builder()
                                 .id(generateRandomId())
                                 .space(space)
                                 .name(name)
                                 .namespace(namespace)
                                 .modified(new Timestamp(System.currentTimeMillis()))
                                 .build();
    }

    protected SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }

    protected SqlFileQueryProvider getSqlFileQueryProvider() {
        return sqlFileQueryProvider;
    }

    protected String generateRandomId() {
        return UUID.randomUUID()
                   .toString();
    }

    private boolean storeFileAttributes(FileEntry fileEntry) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getInsertFileAttributesQuery(fileEntry));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    private int deleteFileEntries(List<FileEntry> fileEntries) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteFileEntriesQuery(fileEntries));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

}
