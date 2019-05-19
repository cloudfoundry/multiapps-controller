package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.executors.SqlQueryExecutor;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.ExternalSqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlFileQueryProvider;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public class FileService {

    protected static final String DEFAULT_TABLE_NAME = "LM_SL_PERSISTENCE_FILE";

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

    public FileEntry addFile(String space, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileInfoProcessor, InputStream is) throws FileStorageException {
        return addFile(space, null, name, fileInfoProcessor, is);
    }

    /**
     * Uploads a new file.
     *
     * @param space
     * @param namespace namespace where the file will be uploaded
     * @param name name of the uploaded file
     * @param fileProcessor file processor
     * @param is input stream to read the content from
     * @return an object representing the file upload
     * @throws FileStorageException
     */
    public FileEntry addFile(String space, String namespace, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileInfoProcessor, InputStream is) throws FileStorageException {
        // Stream the file to a temp location and get the size and MD5 digest
        // as an alternative we can pass the original stream to the database,
        // and decorate the blob stream to calculate digest and size, but this will still require
        // two roundtrips to the database (insert of the content and then update with the digest and
        // size), which is probably inefficient
        FileInfo fileInfo = null;
        FileEntry fileEntry = null;
        try (InputStream inputStream = is) {
            fileInfo = FileUploader.uploadFile(inputStream, fileInfoProcessor);
            fileEntry = addFile(space, namespace, name, fileInfoProcessor, fileInfo);
        } catch (IOException e) {
            logger.debug(e.getMessage(), e);
        } finally {
            if (fileInfo != null) {
                FileUploader.removeFile(fileInfo);
            }
        }
        return fileEntry;
    }

    public FileEntry addFile(String space, String namespace, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileInfoProcessor, File existingFile)
        throws FileStorageException {
        try {
            FileInfo fileInfo = createFileInfo(existingFile);

            return addFile(space, namespace, name, fileInfoProcessor, fileInfo);
        } catch (NoSuchAlgorithmException e) {
            throw new SLException(Messages.ERROR_CALCULATING_FILE_DIGEST, existingFile.getName(), e);
        } catch (FileNotFoundException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_FINDING_FILE_TO_UPLOAD, existingFile.getName()), e);
        } catch (IOException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_READING_FILE_CONTENT, existingFile.getName()), e);
        }
    }

    public List<FileEntry> listFiles(final String space, final String namespace) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListFilesQuery(space, namespace));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_FILES_WITH_SPACE_AND_NAMESPACE, space, namespace),
                e);
        }
    }

    public FileEntry getFile(final String space, final String id) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getRetrieveFileQuery(space, id));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    /**
     * Reads file from the storage.
     *
     * @param space
     * @param fileProcessor file processor
     * @throws FileStorageException
     */
    public void processFileContent(final FileDownloadProcessor fileDownloadProcessor) throws FileStorageException {
        fileStorage.processFileContent(fileDownloadProcessor);
    }

    public int deleteBySpaceAndNamespace(final String space, final String namespace) throws FileStorageException {
        fileStorage.deleteFilesBySpaceAndNamespace(space, namespace);
        return deleteFileAttributesBySpaceAndNamespace(space, namespace);
    }

    public int deleteBySpace(final String space) throws FileStorageException {
        fileStorage.deleteFilesBySpace(space);
        return deleteFileAttributesBySpace(space);
    }

    public int deleteModifiedBefore(Date modificationTime) throws FileStorageException {
        int deletedItems = fileStorage.deleteFilesModifiedBefore(modificationTime);
        return deleteFileAttributesModifiedBefore(modificationTime) + deletedItems;
    }

    public boolean deleteFile(final String space, final String id) throws FileStorageException {
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

    protected void storeFile(FileEntry fileEntry, FileInfo fileInfo) throws FileStorageException {
        fileStorage.addFile(fileEntry, fileInfo.getFile());
        storeFileAttributes(fileEntry);
    }

    protected boolean deleteFileAttribute(final String space, final String id) throws FileStorageException {
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

    protected int deleteFileAttributesBySpaceAndNamespace(final String space, final String namespace) throws FileStorageException {
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

    protected FileEntry createFileEntry(String space, String namespace, String name, FileInfo localFile) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setId(generateRandomId());
        fileEntry.setSpace(space);
        fileEntry.setName(name);
        fileEntry.setNamespace(namespace);
        fileEntry.setSize(localFile.getSize());
        fileEntry.setDigest(localFile.getDigest());
        fileEntry.setDigestAlgorithm(localFile.getDigestAlgorithm());
        fileEntry.setModified(new Timestamp(System.currentTimeMillis()));
        return fileEntry;
    }

    protected SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }

    protected SqlFileQueryProvider getSqlFileQueryProvider() {
        return sqlFileQueryProvider;
    }

    private FileInfo createFileInfo(File existingFile) throws NoSuchAlgorithmException, IOException {
        return new FileInfo(existingFile, BigInteger.valueOf(existingFile.length()),
            DigestHelper.computeFileChecksum(existingFile.toPath(), FileUploader.DIGEST_METHOD), FileUploader.DIGEST_METHOD);
    }

    private FileEntry addFile(String space, String namespace, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileInfoProcessor, FileInfo fileInfo)
        throws FileStorageException {

        FileEntry fileEntry = createFileEntry(space, namespace, name, fileInfo);
        storeFile(fileEntry, fileInfo);
        logger.debug(MessageFormat.format(Messages.STORED_FILE_0, fileEntry));
        return fileEntry;
    }

    private String generateRandomId() {
        return UUID.randomUUID()
            .toString();
    }

    private boolean storeFileAttributes(final FileEntry fileEntry) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileAttributesQuery(fileEntry));
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
