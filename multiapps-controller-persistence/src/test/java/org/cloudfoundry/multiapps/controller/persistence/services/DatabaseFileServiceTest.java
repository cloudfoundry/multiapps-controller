package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.util.DigestHelper;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class DatabaseFileServiceTest {

    protected static final String SPACE_1 = "myspace";
    protected static final String SPACE_2 = "myspace2";
    protected static final String NAMESPACE_1 = "system/deployables";
    protected static final String NAMESPACE_2 = "dido";
    protected static final String PIC_RESOURCE_NAME = "pexels-photo-401794.jpeg";
    protected static final String FILE_OPERATION_ID = UUID.randomUUID()
                                                          .toString();
    protected static final int PIC_SIZE = 2095730;
    protected static final String PIC_STORAGE_NAME = "pic1.jpeg";
    private static final String SELECT_FILE_WITH_CONTENT = "SELECT FILE_ID FROM {0} WHERE FILE_ID=? AND CONTENT IS NOT NULL";
    private static final String UPDATE_MODIFICATION_TIME = "UPDATE {0} SET MODIFIED=? WHERE FILE_ID=?";
    private static final String LIQUIBASE_CHANGELOG_LOCATION = "org/cloudfoundry/multiapps/controller/persistence/db/changelog/db-changelog.xml";
    private static final String DIGEST_METHOD = "MD5";
    private static final String PIC_MD5_DIGEST = "b39a167875c3771c384c9aa5601fc2d6";
    protected FileService fileService;

    protected DataSourceWithDialect testDataSource;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        this.testDataSource = createDataSource();
        this.fileService = createFileService(testDataSource);
    }

    @AfterEach
    public void tearDown() throws Exception {
        sweepFiles();
        tearDownConnection();
    }

    @Test
    void testCorrectFileEntryTimestamp() throws Exception {
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);
        FileEntry fileEntryMetadata = fileService.getFile(SPACE_1, fileEntry.getId());
        // For some reason Java 17 with Ubuntu (as of now Sonar scan) uses nanoseconds precision
        // while Java 11 uses microseconds precision as well as the default Database timestamps
        // and this is the reason for the truncations
        // This should be considered during the migration
        // https://bugs.openjdk.org/browse/JDK-8242504
        assertEquals(fileEntry.getModified()
                              .truncatedTo(ChronoUnit.MILLIS),
                     fileEntryMetadata.getModified()
                                      .truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void successfulUploadAndGetFileTest() throws Exception {
        String space = SPACE_1;
        String namespace = NAMESPACE_1;
        FileEntry fileEntry = addTestFile(space, namespace);
        verifyFileIsStored(fileEntry);

        List<FileEntry> listFiles = fileService.listFiles(space, namespace);
        assertEquals(1, listFiles.size());
        FileEntry listFilesEntry = listFiles.get(0);
        assertEquals(fileEntry.getId(), listFilesEntry.getId());
        verifyFileEntry(listFilesEntry, space, namespace);

        FileEntry getFileEntry = fileService.getFile(space, fileEntry.getId());
        verifyFileEntry(getFileEntry, space, namespace);
    }

    @Test
    void processFileContentTest() throws Exception {
        Path expectedFile = Paths.get("src/test/resources/", PIC_RESOURCE_NAME);
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);
        String expectedFileDigest = DigestHelper.computeFileChecksum(expectedFile, DIGEST_METHOD)
                                                .toLowerCase();
        validateFileContent(fileEntry, expectedFileDigest);
    }

    @Test
    void deleteBySpaceAndNamespaceTest() throws Exception {
        addTestFile(SPACE_1, NAMESPACE_1);
        addTestFile(SPACE_1, NAMESPACE_1);
        int deleteByWrongSpace = fileService.deleteBySpaceAndNamespace(SPACE_2, NAMESPACE_1);
        assertEquals(0, deleteByWrongSpace);
        int deleteByWrongNamespace = fileService.deleteBySpaceAndNamespace(SPACE_2, NAMESPACE_2);
        assertEquals(0, deleteByWrongNamespace);
        int correctDelete = fileService.deleteBySpaceAndNamespace(SPACE_1, NAMESPACE_1);
        assertEquals(2, correctDelete);
        List<FileEntry> listFiles = fileService.listFiles(SPACE_1, NAMESPACE_1);
        assertEquals(0, listFiles.size());
    }

    @Test
    void deleteBySpaceAndNamespaceWithTwoNamespacesTest() throws Exception {
        addTestFile(SPACE_1, NAMESPACE_1);
        addTestFile(SPACE_1, NAMESPACE_2);
        int correctDelete = fileService.deleteBySpaceAndNamespace(SPACE_1, NAMESPACE_1);
        assertEquals(1, correctDelete);
        List<FileEntry> listFiles = fileService.listFiles(SPACE_1, NAMESPACE_1);
        assertEquals(0, listFiles.size());
        listFiles = fileService.listFiles(SPACE_1, NAMESPACE_2);
        assertEquals(1, listFiles.size());
    }

    @Test
    void deleteBySpaceTest() throws Exception {
        addTestFile(SPACE_1, NAMESPACE_1);
        addTestFile(SPACE_1, NAMESPACE_2);
        int deleteByWrongSpace = fileService.deleteBySpaceIds(List.of(SPACE_2));
        assertEquals(0, deleteByWrongSpace);
        int correctDelete = fileService.deleteBySpaceIds(List.of(SPACE_1));
        assertEquals(2, correctDelete);
        List<FileEntry> listFiles = fileService.listFiles(SPACE_1, null);
        assertEquals(0, listFiles.size());
    }

    @Test
    void deleteFileTest() throws Exception {
        FileEntry fileEntry = addTestFile(SPACE_1, NAMESPACE_1);

        boolean deleteFile = fileService.deleteFile(SPACE_2, fileEntry.getId());
        assertFalse(deleteFile);

        deleteFile = fileService.deleteFile(SPACE_1, fileEntry.getId());
        assertTrue(deleteFile);
    }

    @Test
    void deleteByModificationTimeTest() throws Exception {
        long currentMillis = System.currentTimeMillis();
        final long oldFilesTtl = 1000 * 60 * 10; // 10min
        Date pastMoment = new Date(currentMillis - 1000 * 60 * 15); // before 15min

        FileEntry fileEntryToRemain1 = addFileEntry(SPACE_1);
        FileEntry fileEntryToRemain2 = addFileEntry(SPACE_2);
        FileEntry fileEntryToDelete1 = addFileEntry(SPACE_1);
        FileEntry fileEntryToDelete2 = addFileEntry(SPACE_2);

        setModificationDate(fileEntryToDelete1, pastMoment);
        setModificationDate(fileEntryToDelete2, pastMoment);

        LocalDateTime deleteDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentMillis - oldFilesTtl), ZoneId.systemDefault());
        int deletedFiles = fileService.deleteModifiedBefore(deleteDate);

        assertNotNull(fileService.getFile(SPACE_1, fileEntryToRemain1.getId()));
        assertNotNull(fileService.getFile(SPACE_2, fileEntryToRemain2.getId()));

        assertEquals(2, deletedFiles);
        assertNull(fileService.getFile(SPACE_1, fileEntryToDelete1.getId()));
        assertNull(fileService.getFile(SPACE_2, fileEntryToDelete2.getId()));
    }

    @Test
    void testOpenInputStream() throws Exception {
        FileEntry fileEntry = addFile(SPACE_1, NAMESPACE_1, PIC_STORAGE_NAME, PIC_RESOURCE_NAME, FILE_OPERATION_ID);
        verifyFileIsStored(fileEntry);
        InputStream inputStream = null;
        InputStream existingFileStream = null;
           try {
            inputStream = fileService.openInputStream(SPACE_1, fileEntry.getId());
            existingFileStream = getResource(PIC_RESOURCE_NAME);
            byte[] fileContent = IOUtils.toByteArray(inputStream);
            byte[] existingFileContent = IOUtils.toByteArray(existingFileStream);
            assertArrayEquals(existingFileContent, fileContent);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(existingFileStream);
        }
    }

    protected FileService createFileService(DataSourceWithDialect dataSource) {
        return new DatabaseFileService(dataSource);
    }

    protected FileEntry addTestFile(String space, String namespace) throws Exception {
        return addFile(space, namespace, PIC_STORAGE_NAME, PIC_RESOURCE_NAME, FILE_OPERATION_ID);
    }

    protected FileEntry addFile(String space, String namespace, String fileName, String resourceName, String operationId) throws Exception {
        InputStream resourceStream = getResource(resourceName);
        FileEntry fileEntry = fileService.addFile(ImmutableFileEntry.builder()
                                                                    .space(space)
                                                                    .namespace(namespace)
                                                                    .name(fileName)
                                                                    .size(BigInteger.valueOf(PIC_SIZE))
                                                                    .operationId(operationId)
                                                                    .build(),
                                                  resourceStream);
        verifyFileEntry(fileEntry, space, namespace);
        return fileEntry;
    }

    protected InputStream getResource(String name) {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream(name);
    }

    private void verifyFileEntry(FileEntry entry, String space, String namespace) {
        // verify image name
        assertEquals(PIC_STORAGE_NAME, entry.getName());
        assertEquals(space, entry.getSpace());
        assertEquals(namespace, entry.getNamespace());
        // the size of the uploaded file
        assertEquals(BigInteger.valueOf(PIC_SIZE), entry.getSize());

        // verify the MD5 digest, compare with one taken with md5sum
        assertEquals(PIC_MD5_DIGEST.toLowerCase(), entry.getDigest()
                                                        .toLowerCase());
        assertEquals(DIGEST_METHOD, entry.getDigestAlgorithm());
    }

    private DataSourceWithDialect createDataSource() throws Exception {
        return new DataSourceWithDialect(TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION));
    }

    private void sweepFiles() throws Exception {
        fileService.deleteBySpaceIds(List.of(SPACE_1, SPACE_2));
    }

    private void tearDownConnection() throws Exception {
        // actually close the connection
        testDataSource.getDataSource()
                      .getConnection()
                      .close();
    }

    private FileEntry addFileEntry(String spaceId) throws FileStorageException {
        InputStream resourceStream = getResource(PIC_RESOURCE_NAME);
        return fileService.addFile(ImmutableFileEntry.builder()
                                                     .space(spaceId)
                                                     .namespace(NAMESPACE_1)
                                                     .name(PIC_STORAGE_NAME)
                                                     .size(BigInteger.valueOf(PIC_SIZE))
                                                     .build(),
                                   resourceStream);
    }

    private void validateFileContent(FileEntry storedFile, final String expectedFileChecksum) throws FileStorageException {
        fileService.processFileContent(storedFile.getSpace(), storedFile.getId(), contentStream -> {
            // make a digest out of the content and compare it to the original
            final byte[] digest = calculateFileDigest(contentStream);
            assertEquals(expectedFileChecksum, DatatypeConverter.printHexBinary(digest)
                                                                .toLowerCase());
            return null;
        });
    }

    private byte[] calculateFileDigest(InputStream contentStream) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_METHOD);
            int read = 0;
            byte[] buffer = new byte[4 * 1024];
            while ((read = contentStream.read(buffer)) > -1) {
                md.update(buffer, 0, read);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setModificationDate(FileEntry fileEntry, Date modificationDate) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = testDataSource.getDataSource()
                                      .getConnection()
                                      .prepareStatement(MessageFormat.format(UPDATE_MODIFICATION_TIME, FileService.DEFAULT_TABLE_NAME));
            statement.setTimestamp(1, new java.sql.Timestamp(modificationDate.getTime()));
            statement.setString(2, fileEntry.getId());
            statement.executeUpdate();
        } finally {
            JdbcUtil.closeQuietly(statement);
        }
    }

    protected void verifyFileIsStored(FileEntry fileEntry) throws Exception {
        PreparedStatement statement = null;
        try {
            statement = testDataSource.getDataSource()
                                      .getConnection()
                                      .prepareStatement(MessageFormat.format(SELECT_FILE_WITH_CONTENT, FileService.DEFAULT_TABLE_NAME));
            statement.setString(1, fileEntry.getId());
            ResultSet executeQuery = statement.executeQuery();
            assertTrue(executeQuery.next());
        } finally {
            JdbcUtil.closeQuietly(statement);
        }
    }
}
