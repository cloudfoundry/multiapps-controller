package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.common.util.DigestHelper;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class DatabaseFileServiceTest {

    private static final String UPDATE_MODIFICATION_TIME = "UPDATE {0} SET MODIFIED=? WHERE FILE_ID=?";
    private static final String DEFAULT_TABLE_NAME = "LM_SL_PERSISTENCE_FILE";

    private static final String LIQUIBASE_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/persistence/db/changelog/db-changelog.xml";

    protected static final String DIGEST_METHOD = "MD5";

    private static final String PIC1_RESOURCE_NAME = "pexels-photo-401794.jpeg";
    private static final String PIC1_STORAGE_NAME = "pic1.jpeg";
    private static final int PIC1_SIZE = 2095730;
    private static final String PIC1_MD5_DIGEST = "b39a167875c3771c384c9aa5601fc2d6";
    private static final String SYSTEM_NAMESPACE = "system/deployables";
    protected static final String MY_SPACE_ID = "myspace";
    protected static final String MY_SPACE_2_ID = "myspace2";

    private static final String PIC2_RESOURCE_NAME = "pexels-photo-463467.jpeg";
    private static final String PIC2_STORAGE_NAME = "pic2.jpeg";
    private static final String PERSONAL_NAMESPACE = "dido";

    private static final BigInteger MAX_UPLOAD_SIZE = BigInteger.valueOf(PIC1_SIZE);

    protected AbstractFileService fileService;

    private FileEntry storedFile;

    protected DataSourceWithDialect testDataSource;

    @Before
    public void setUp() throws Exception {
        this.testDataSource = createDataSource();
        this.fileService = createFileService(testDataSource);
        insertInitialData();
    }

    protected DataSourceWithDialect createDataSource() throws Exception {
        return new DataSourceWithDialect(TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION));
    }

    protected AbstractFileService createFileService(DataSourceWithDialect dataSource) {
        return new DatabaseFileService(dataSource);
    }

    @After
    public void tearDown() throws Exception {
        sweepFiles();
        tearDownConnection();
    }

    protected void sweepFiles() throws FileStorageException, Exception {
        fileService.deleteAll(MY_SPACE_ID, SYSTEM_NAMESPACE);
        fileService.deleteAll(MY_SPACE_ID, PERSONAL_NAMESPACE);
    }

    protected void tearDownConnection() throws Exception {
        // actually close the connection
        testDataSource.getDataSource()
            .getConnection()
            .close();
    }

    protected void insertInitialData() throws Exception {
        storedFile = addFileEntry(MY_SPACE_ID);
    }

    @SuppressWarnings("deprecation")
    private FileEntry addFileEntry(String spaceId) throws FileStorageException {
        InputStream resourceStream = getResource(PIC1_RESOURCE_NAME);
        return fileService.addFile(spaceId, SYSTEM_NAMESPACE, PIC1_STORAGE_NAME, new DefaultFileUploadProcessor(false), resourceStream);
    }

    private InputStream getResource(String name) {
        return Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(name);
    }

    private void verifyInitialEntry(FileEntry entry) {
        // verify image name
        assertEquals(PIC1_STORAGE_NAME, entry.getName());
        assertEquals(SYSTEM_NAMESPACE, entry.getNamespace());
        // the size of the uploaded file
        assertEquals(BigInteger.valueOf(PIC1_SIZE), entry.getSize());

        // verify the MD5 digest, compare with one taken with md5sum
        assertEquals(PIC1_MD5_DIGEST.toLowerCase(), entry.getDigest()
            .toLowerCase());
        assertEquals(DIGEST_METHOD, entry.getDigestAlgorithm());
    }

    @Test
    public void testUploadTwoIdenticalFiles() throws Exception {
        List<FileEntry> listFiles = fileService.listFiles(MY_SPACE_ID, SYSTEM_NAMESPACE);
        assertEquals(1, listFiles.size());
        FileEntry entry = listFiles.get(0);

        verifyInitialEntry(entry);
    }

    @Test
    public void testUploadTwoFiles() throws Exception {
        InputStream resourceStream = getResource(PIC2_RESOURCE_NAME);
        fileService.addFile(MY_SPACE_ID, SYSTEM_NAMESPACE, PIC2_STORAGE_NAME, new DefaultFileUploadProcessor(false), resourceStream);
        List<FileEntry> listFiles = fileService.listFiles(MY_SPACE_ID, SYSTEM_NAMESPACE);
        assertEquals(2, listFiles.size());
    }

    @Test
    public void testGetFile() throws FileStorageException, NoSuchAlgorithmException, IOException {
        FileEntry fileEntry = fileService.getFile(MY_SPACE_ID, storedFile.getId());
        verifyInitialEntry(fileEntry);
    }

    @Test
    public void testFileContent() throws Exception {
        Path expectedFile = Paths.get("src/test/resources/", PIC1_RESOURCE_NAME);
        String expectedFileDigest = DigestHelper.computeFileChecksum(expectedFile, DIGEST_METHOD)
            .toLowerCase();
        validateFileContent(storedFile, expectedFileDigest);
    }

    protected void validateFileContent(FileEntry storedFile, final String expectedFileChecksum) throws FileStorageException {
        fileService
            .processFileContent(new DefaultFileDownloadProcessor(storedFile.getSpace(), storedFile.getId(), new FileContentProcessor() {
                @Override
                public void processFileContent(InputStream contentStream) throws NoSuchAlgorithmException, IOException {
                    // make a digest out of the content and compare it to the original
                    final byte[] digest = calculateFileDigest(contentStream);
                    assertEquals(expectedFileChecksum, DatatypeConverter.printHexBinary(digest)
                        .toLowerCase());
                }

            }));
    }

    protected byte[] calculateFileDigest(InputStream contentStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance(DIGEST_METHOD);
        int read = 0;
        byte[] buffer = new byte[4 * 1024];
        while ((read = contentStream.read(buffer)) > -1) {
            md.update(buffer, 0, read);
        }
        return md.digest();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testNamespaceIsolation() throws Exception {
        List<FileEntry> personalFiles = fileService.listFiles(MY_SPACE_ID, PERSONAL_NAMESPACE);
        assertEquals(0, personalFiles.size());

        List<FileEntry> systemFiles = fileService.listFiles(MY_SPACE_ID, SYSTEM_NAMESPACE);
        assertEquals(1, systemFiles.size());

        InputStream resourceStream = getResource(PIC2_RESOURCE_NAME);

        fileService.addFile(MY_SPACE_ID, PERSONAL_NAMESPACE, PIC2_STORAGE_NAME, new DefaultFileUploadProcessor(false), resourceStream);
        personalFiles = fileService.listFiles(MY_SPACE_ID, PERSONAL_NAMESPACE);
        assertEquals(1, personalFiles.size());

        systemFiles = fileService.listFiles(MY_SPACE_ID, SYSTEM_NAMESPACE);
        assertEquals(1, systemFiles.size());

        fileService.deleteFile(MY_SPACE_ID, systemFiles.get(0)
            .getId());

        personalFiles = fileService.listFiles(MY_SPACE_ID, PERSONAL_NAMESPACE);
        assertEquals(1, personalFiles.size());

        systemFiles = fileService.listFiles(MY_SPACE_ID, SYSTEM_NAMESPACE);
        assertEquals(0, systemFiles.size());
    }

    @Test
    public void testDeleteFile() throws FileStorageException {
        fileService.deleteFile(MY_SPACE_ID, storedFile.getId());

        FileEntry missingEntry = fileService.getFile(MY_SPACE_ID, storedFile.getId());
        assertNull(missingEntry);

        List<FileEntry> namespaceFiles = fileService.listFiles(MY_SPACE_ID, SYSTEM_NAMESPACE);
        assertEquals(0, namespaceFiles.size());
    }

    @Test
    public void testDeleteByModificationTime() throws Exception {
        long currentMillis = System.currentTimeMillis();
        final long oldFilesTtl = 1000 * 60 * 10; // 10min
        Date pastMoment = new Date(currentMillis - 1000 * 60 * 15); // before 15min

        FileEntry fileEntryToRemain1 = addFileEntry(MY_SPACE_ID);
        FileEntry fileEntryToRemain2 = addFileEntry(MY_SPACE_2_ID);
        FileEntry fileEntryToDelete1 = addFileEntry(MY_SPACE_ID);
        FileEntry fileEntryToDelete2 = addFileEntry(MY_SPACE_2_ID);

        setMofidicationDate(fileEntryToDelete1, pastMoment);
        setMofidicationDate(fileEntryToDelete2, pastMoment);

        int deletedFiles = fileService.deleteByModificationTime(new Date(currentMillis - oldFilesTtl));

        assertNotNull(fileService.getFile(MY_SPACE_ID, fileEntryToRemain1.getId()));
        assertNotNull(fileService.getFile(MY_SPACE_2_ID, fileEntryToRemain2.getId()));

        assertEquals(2, deletedFiles);
        assertNull(fileService.getFile(MY_SPACE_ID, fileEntryToDelete1.getId()));
        assertNull(fileService.getFile(MY_SPACE_2_ID, fileEntryToDelete2.getId()));
    }

    private void setMofidicationDate(FileEntry fileEntry, Date modificationDate) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = testDataSource.getDataSource()
                .getConnection()
                .prepareStatement(MessageFormat.format(UPDATE_MODIFICATION_TIME, DEFAULT_TABLE_NAME));
            statement.setTimestamp(1, new java.sql.Timestamp(modificationDate.getTime()));
            statement.setString(2, fileEntry.getId());
            statement.executeUpdate();
        } finally {
            JdbcUtil.closeQuietly(statement);
        }
    }

}
