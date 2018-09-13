package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileUploadProcessor;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public class FileSystemFileServiceTest extends DatabaseFileServiceTest {

    private static final String TEST_FILE_LOCATION = "src/test/resources/pexels-photo-401794.jpeg";
    private static final String SECOND_FILE_TEST_LOCATION = "src/test/resources/pexels-photo-463467.jpeg";

    private String spaceId;
    private String namespace;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Path temporaryStorageLocation;

    @Before
    public void setUp() throws Exception {
        this.testDataSource = createDataSource();
        this.temporaryStorageLocation = Files.createTempDirectory("testfileService");
        fileService = createFileService(testDataSource);
        spaceId = UUID.randomUUID()
            .toString();
        namespace = UUID.randomUUID()
            .toString();
        insertInitialData();
    }

    @Test
    public void testAddFile() throws FileStorageException, IOException {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION)
            .toAbsolutePath();
        FileEntry addedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));
        assertFileExists(true, addedFile);
    }

    @Test
    public void testAddFileWichAlreadyExists() throws FileStorageException, IOException {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION)
            .toAbsolutePath();
        fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), testFilePath.toFile());
        FileEntry addedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), testFilePath.toFile());
        assertFileExists(true, addedFile);
    }

    @Test
    public void testAddExistingFile() throws FileStorageException, IOException {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION)
            .toAbsolutePath();
        FileEntry addedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), testFilePath.toFile());

        assertFileExists(true, addedFile);
    }

    @Test
    public void testDeleteAllFiles() throws FileStorageException {
        try {
            fileService.deleteAll(spaceId, namespace);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testGetFile() throws FileStorageException, NoSuchAlgorithmException, IOException {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION)
            .toAbsolutePath();
        FileEntry addedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), testFilePath.toFile());

        FileEntry actualFile = fileService.getFile(spaceId, addedFile.getId());
        String expectedFileFileDigest = DigestHelper.computeFileChecksum(testFilePath, DIGEST_METHOD);
        String actualFileFileDigest = actualFile.getDigest();
        Assert.assertEquals(expectedFileFileDigest, actualFileFileDigest);
    }

    @Test
    public void testGetFileWithNull() throws FileStorageException {
        FileEntry actualFile = fileService.getFile(null, null);
        assertNull(actualFile);
    }

    @Test
    public void testDeleteFile() throws FileStorageException {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION)
            .toAbsolutePath();
        FileEntry addedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), testFilePath.toFile());

        boolean wasDeleted = fileService.deleteFile(spaceId, addedFile.getId());
        Assert.assertTrue(wasDeleted);
    }

    @Test
    public void testUploadTwoFiles() throws FileStorageException, IOException {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION);
        FileEntry firstAddedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));

        Path secondTestFilePath = Paths.get(SECOND_FILE_TEST_LOCATION);
        FileEntry secondAddedFile = fileService.addFile(spaceId, namespace, secondTestFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(secondTestFilePath));

        List<FileEntry> fileEntries = fileService.listFiles(spaceId, namespace);
        Assert.assertEquals(2, fileEntries.size());
        validateFilesEquality(fileEntries, firstAddedFile, secondAddedFile);

    }

    @Override
    public void testFileContent() throws Exception {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION);
        FileEntry addedFile = fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));

        String testFileDigest = DigestHelper.computeFileChecksum(testFilePath, DIGEST_METHOD)
            .toLowerCase();
        validateFileContent(addedFile, testFileDigest);
    }

    @Test
    public void testFileContentNotExisting() throws Exception {
        String fileId = "not-existing-file-id";
        String fileSpace = "not-existing-space-id";
        String fileDigest = DigestHelper.computeFileChecksum(Paths.get(TEST_FILE_LOCATION), DIGEST_METHOD)
            .toLowerCase();
        FileEntry dummyFileEntry = new FileEntry();
        dummyFileEntry.setId(fileId);
        dummyFileEntry.setSpace(fileSpace);
        expectedException.expect(FileStorageException.class);
        expectedException.expectMessage(MessageFormat.format(Messages.FILE_WITH_ID_AND_SPACE_DOES_NOT_EXIST, fileId, fileSpace));
        validateFileContent(dummyFileEntry, fileDigest);
    }

    @Override
    public void testUploadTwoIdenticalFiles() throws Exception {
        Path testFilePath = Paths.get(TEST_FILE_LOCATION);
        fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));

        fileService.addFile(spaceId, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));

        List<FileEntry> listFiles = fileService.listFiles(spaceId, namespace);
        Assert.assertEquals(2, listFiles.size());
    }

    @Override
    public void testNamespaceIsolation() throws Exception {
        // No need for testing as the isolation is covered in the other test case scenarios
    }

    @Override
    public void testDeleteByModificationTime() throws Exception {
        long currentMillis = System.currentTimeMillis();
        final long oldFilesTtl = 1000 * 60 * 10; // 10min
        final long pastMoment = currentMillis - 1000 * 60 * 15; // before 15min

        Path testFilePath = Paths.get(TEST_FILE_LOCATION)
            .toAbsolutePath();
        Path secondTestFilePath = Paths.get(SECOND_FILE_TEST_LOCATION)
            .toAbsolutePath();
        FileEntry fileEntryToRemain1 = fileService.addFile(MY_SPACE_ID, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));
        FileEntry fileEntryToRemain2 = fileService.addFile(MY_SPACE_2_ID, namespace, secondTestFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(secondTestFilePath));
        FileEntry fileEntryToDelete1 = fileService.addFile(MY_SPACE_ID, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));
        FileEntry fileEntryToDelete2 = fileService.addFile(MY_SPACE_2_ID, namespace, testFilePath.toFile()
            .getName(), new DefaultFileUploadProcessor(false), Files.newInputStream(testFilePath));

        Files.setLastModifiedTime(getFileLocation(fileEntryToDelete1), FileTime.fromMillis(pastMoment));
        Files.setLastModifiedTime(getFileLocation(fileEntryToDelete2), FileTime.fromMillis(pastMoment));

        Path oldNonDeployerFile = Files.createFile(Paths.get(temporaryStorageLocation.toString(), "random"));
        Files.setLastModifiedTime(oldNonDeployerFile, FileTime.fromMillis(pastMoment));

        int deletedFiles = fileService.deleteModifiedBefore(new Date(currentMillis - oldFilesTtl));

        assertFileExists(true, fileEntryToRemain1);
        assertFileExists(true, fileEntryToRemain2);

        assertEquals(3, deletedFiles);
        assertFileExists(false, fileEntryToDelete1);
        assertFileExists(false, fileEntryToDelete2);
        Assert.assertFalse(Files.exists(oldNonDeployerFile));
    }

    private void validateFilesEquality(List<FileEntry> actualFileEntries, FileEntry... expected) {
        for (FileEntry expectedFileEntry : expected) {
            FileEntry actualFileEntry = find(expectedFileEntry, actualFileEntries);
            Assert.assertNotNull(actualFileEntry);
            validateFilesEquality(expectedFileEntry, actualFileEntry);
        }
    }

    private void validateFilesEquality(FileEntry expected, FileEntry actualFileEntry) {
        Assert.assertEquals(expected.getId(), actualFileEntry.getId());
        Assert.assertEquals(expected.getName(), actualFileEntry.getName());
        Assert.assertEquals(expected.getNamespace(), actualFileEntry.getNamespace());
        Assert.assertEquals(expected.getSpace(), actualFileEntry.getSpace());
        Assert.assertEquals(expected.getDigest(), actualFileEntry.getDigest());
        Assert.assertEquals(expected.getDigestAlgorithm(), actualFileEntry.getDigestAlgorithm());
        Assert.assertEquals(expected.getSize(), actualFileEntry.getSize());
    }

    private FileEntry find(FileEntry expected, List<FileEntry> actualFileEntries) {
        for (FileEntry fileEntry : actualFileEntries) {
            if (expected.getId()
                .equals(fileEntry.getId())) {
                return fileEntry;
            }
        }
        return null;
    }

    private void assertFileExists(boolean exceptedFileExist, FileEntry actualFile) {
        Assert.assertEquals(exceptedFileExist, Files.exists(getFileLocation(actualFile)));
    }

    private Path getFileLocation(FileEntry actualFile) {
        return Paths.get(temporaryStorageLocation.toString(), actualFile.getSpace(), "files", actualFile.getId());
    }

    @After
    public void tearDown() throws Exception {
        try {
            tearDownConnection();
            FileUtils.deleteDirectory(temporaryStorageLocation.toFile());
        } catch (IOException e) {
        }
    }

    @Override
    protected AbstractFileService createFileService(DataSourceWithDialect dataSource) {
        return new FileSystemFileService(testDataSource, temporaryStorageLocation.toString());
    }
}
