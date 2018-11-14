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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;
import com.sap.cloud.lm.sl.cf.persistence.executors.SqlQueryExecutor;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.FileInfo;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlFileQueryProvider;
import com.sap.cloud.lm.sl.cf.persistence.security.VirusScanner;
import com.sap.cloud.lm.sl.cf.persistence.security.VirusScannerException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public abstract class AbstractFileService {

    protected static final String DEFAULT_TABLE_NAME = "LM_SL_PERSISTENCE_FILE";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private SqlFileQueryProvider sqlFileQueryProvider;
    private final String tableName;
    private final DataSourceWithDialect dataSourceWithDialect;
    private final SqlQueryExecutor sqlQueryExecutor;
    private VirusScanner virusScanner;

    protected AbstractFileService(String tableName, DataSourceWithDialect dataSourceWithDialect) {
        this.tableName = tableName;
        this.dataSourceWithDialect = dataSourceWithDialect;
        this.sqlQueryExecutor = new SqlQueryExecutor(dataSourceWithDialect.getDataSource());
        sqlFileQueryProvider = new SqlFileQueryProvider(tableName, dataSourceWithDialect.getDataSourceDialect(), logger);
    }

    public void setVirusScanner(VirusScanner virusScanner) {
        this.virusScanner = virusScanner;
    }

    public FileEntry addFile(String space, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileUploadProcessor, InputStream is)
        throws FileStorageException {
        return addFile(space, null, name, fileUploadProcessor, is);
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
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileUploadProcessor, InputStream is)
        throws FileStorageException {
        // Stream the file to a temp location and get the size and MD5 digest
        // as an alternative we can pass the original stream to the database,
        // and decorate the blob stream to calculate digest and size, but this will still require
        // two roundtrips to the database (insert of the content and then update with the digest and
        // size), which is probably inefficient
        FileInfo fileUpload = null;
        try {
            fileUpload = FileUploader.uploadFile(is, fileUploadProcessor);

            return addFile(space, namespace, name, fileUploadProcessor, fileUpload);
        } finally {
            IOUtils.closeQuietly(is);
            if (fileUpload != null) {
                FileUploader.removeFile(fileUpload);
            }
        }
    }

    public FileEntry addFile(String space, String namespace, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileUploadProcessor, File existingFile)
        throws FileStorageException {
        try {
            FileInfo fileInfo = createFileInfo(existingFile);

            return addFile(space, namespace, name, fileUploadProcessor, fileInfo);
        } catch (NoSuchAlgorithmException e) {
            throw new SLException(Messages.ERROR_CALCULATING_FILE_DIGEST, existingFile.getName(), e);
        } catch (FileNotFoundException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_FINDING_FILE_TO_UPLOAD, existingFile.getName()), e);
        } catch (IOException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_READING_FILE_CONTENT, existingFile.getName()), e);
        }
    }

    private FileInfo createFileInfo(File existingFile) throws NoSuchAlgorithmException, IOException {
        return new FileInfo(existingFile, BigInteger.valueOf(existingFile.length()),
            DigestHelper.computeFileChecksum(existingFile.toPath(), FileUploader.DIGEST_METHOD), FileUploader.DIGEST_METHOD);
    }

    private FileEntry addFile(String space, String namespace, String name,
        FileUploadProcessor<? extends OutputStream, ? extends OutputStream> fileUploadProcessor, FileInfo fileInfo)
        throws FileStorageException {

        if (fileUploadProcessor.shouldScanFile()) {
            scan(fileInfo);
        }
        FileEntry fileEntry = createFileEntry(space, namespace, name, fileInfo);
        storeFile(fileEntry, fileInfo);
        logger.debug(MessageFormat.format(Messages.STORED_FILE_0, fileEntry));
        return fileEntry;
    }

    protected void scan(FileInfo file) throws FileStorageException {
        if (virusScanner == null) {
            throw new FileStorageException(Messages.NO_VIRUS_SCANNER_CONFIGURED);
        }
        try {
            logger.info(MessageFormat.format(Messages.SCANNING_FILE, file.getFile()));
            virusScanner.scanFile(file.getFile());
            logger.info(MessageFormat.format(Messages.SCANNING_FILE_SUCCESS, file.getFile()));
        } catch (VirusScannerException e) {
            logger.error(MessageFormat.format(Messages.DELETING_LOCAL_FILE_BECAUSE_OF_INFECTION, file.getFile()));
            FileUploader.removeFile(file);
            throw e;
        }
    }

    protected FileEntry createFileEntry(String space, String namespace, String name, FileInfo fileInfo) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setId(generateRandomId());
        fileEntry.setSpace(space);
        fileEntry.setName(name);
        fileEntry.setNamespace(namespace);
        fileEntry.setSize(fileInfo.getSize());
        fileEntry.setDigest(fileInfo.getDigest());
        fileEntry.setDigestAlgorithm(fileInfo.getDigestAlgorithm());
        fileEntry.setModified(new Timestamp(System.currentTimeMillis()));
        return fileEntry;
    }

    protected String generateRandomId() {
        return UUID.randomUUID()
            .toString();
    }

    private void storeFile(FileEntry fileEntry, FileInfo fileUpload) throws FileStorageException {
        InputStream fileStream = null;
        try {
            fileStream = fileUpload.getInputStream();
            if (!storeFile(fileEntry, fileStream)) {
                throw new FileStorageException(
                    MessageFormat.format(Messages.FILE_UPLOAD_FAILED, fileEntry.getName(), fileEntry.getNamespace()));
            }
        } finally {
            IOUtils.closeQuietly(fileStream);
        }
    }

    protected abstract boolean storeFile(final FileEntry fileEntry, final InputStream inputStream) throws FileStorageException;

    protected boolean storeFileAttributes(final FileEntry fileEntry) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileAttributesQuery(fileEntry));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    public int deleteAll(final String space, final String namespace) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteAllQuery(space, namespace));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    public abstract void deleteBySpace(final String space) throws FileStorageException;

    public int deleteModifiedBefore(Date modificationTime) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteModifiedBeforeQuery(modificationTime));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
        }
    }

    public boolean deleteFile(final String space, final String id) throws FileStorageException {
        deleteFileContent(space, id);
        return deleteFileAttributes(space, id);
    }

    protected abstract void deleteFileContent(String space, String id) throws FileStorageException;

    protected boolean deleteFileAttributes(final String space, final String id) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteFileAttributesQuery(space, id));
        } catch (SQLException e) {
            throw new FileStorageException(e.getMessage(), e);
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

    public List<FileEntry> listFiles(final String namespace) throws FileStorageException {
        return listFiles(null, namespace);
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
    public abstract void processFileContent(final FileDownloadProcessor fileDownloadProcessor) throws FileStorageException;

    protected String getQuery(String statementTemplate) {
        return String.format(statementTemplate, tableName);
    }

    protected DataSourceWithDialect getDataSourceWithDialect() {
        return dataSourceWithDialect;
    }

    protected DataSourceDialect getDataSourceDialect() {
        return dataSourceWithDialect.getDataSourceDialect();
    }

    protected SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }

    protected SqlFileQueryProvider getSqlFileQueryProvider() {
        return sqlFileQueryProvider;
    }

}
