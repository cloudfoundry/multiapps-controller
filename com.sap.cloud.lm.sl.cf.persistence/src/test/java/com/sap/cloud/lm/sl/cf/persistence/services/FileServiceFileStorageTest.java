package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableFileEntry;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public class FileServiceFileStorageTest {

    private static final String TEST_FILE_LOCATION = "src/test/resources/pexels-photo-401794.jpeg";
    private static final String SECOND_FILE_TEST_LOCATION = "src/test/resources/pexels-photo-463467.jpeg";
    private static final String DIGEST_METHOD = "MD5";

    private String spaceId;
    private String namespace;

    private FileStorage fileStorage;

    private Path temporaryStorageLocation;

    @Before
    public void setUp() throws Exception {
        this.temporaryStorageLocation = Files.createTempDirectory("testfileStorage");
        fileStorage = new FileSystemFileStorage(temporaryStorageLocation.toString());
        spaceId = UUID.randomUUID()
                      .toString();
        namespace = UUID.randomUUID()
                        .toString();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(temporaryStorageLocation.toFile());
    }

    @Test
    public void addFileTest() throws Exception {
        FileEntry fileEntry = addFile(TEST_FILE_LOCATION);
        assertFileExists(true, fileEntry);
    }

    @Test
    public void getFileEntriesWithoutContent() throws Exception {
        List<FileEntry> fileEntries = new ArrayList<>();
        FileEntry existingFile = addFile(TEST_FILE_LOCATION);
        fileEntries.add(existingFile);
        FileEntry existingFile2 = addFile(SECOND_FILE_TEST_LOCATION);
        fileEntries.add(existingFile2);
        FileEntry nonExistingFile = createFileEntry();
        fileEntries.add(nonExistingFile);

        List<FileEntry> withoutContent = fileStorage.getFileEntriesWithoutContent(fileEntries);
        assertEquals(1, withoutContent.size());
        assertEquals(nonExistingFile.getId(), withoutContent.get(0)
                                                            .getId());
    }

    @Test
    public void deleteFile() throws Exception {
        FileEntry fileThatWillBeDeleted = addFile(TEST_FILE_LOCATION);
        FileEntry fileThatStays = addFile(SECOND_FILE_TEST_LOCATION);

        fileStorage.deleteFile(fileThatWillBeDeleted.getId(), fileThatWillBeDeleted.getSpace());
        assertFileExists(false, fileThatWillBeDeleted);
        assertFileExists(true, fileThatStays);

    }

    @Test
    public void deleteFilesBySpace() throws Exception {
        FileEntry firstFile = addFile(TEST_FILE_LOCATION);
        FileEntry secondFile = addFile(SECOND_FILE_TEST_LOCATION);
        FileEntry fileInOtherSpace = addFile(TEST_FILE_LOCATION, "otherspace", namespace);

        fileStorage.deleteFilesBySpace(spaceId);
        assertFileExists(false, firstFile);
        assertFileExists(false, secondFile);
        assertFileExists(true, fileInOtherSpace);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void deleteFilesBySpaceAndNamespace() throws Exception {
        fileStorage.deleteFilesBySpaceAndNamespace(spaceId, namespace);

    }

    @Test
    public void deleteFilesModifiedBefore() throws Exception {
        long currentMillis = System.currentTimeMillis();
        final long oldFilesTtl = 1000 * 60 * 10; // 10min
        final long pastMoment = currentMillis - 1000 * 60 * 15; // before 15min

        FileEntry fileEntryToRemain1 = addFile(TEST_FILE_LOCATION);
        FileEntry fileEntryToRemain2 = addFile(SECOND_FILE_TEST_LOCATION);
        FileEntry fileEntryToDelete1 = addFile(TEST_FILE_LOCATION);
        FileEntry fileEntryToDelete2 = addFile(SECOND_FILE_TEST_LOCATION);

        Files.setLastModifiedTime(getFileLocation(fileEntryToDelete1), FileTime.fromMillis(pastMoment));
        Files.setLastModifiedTime(getFileLocation(fileEntryToDelete2), FileTime.fromMillis(pastMoment));

        Path oldNonDeployerFile = Files.createFile(Paths.get(temporaryStorageLocation.toString(), "random"));
        Files.setLastModifiedTime(oldNonDeployerFile, FileTime.fromMillis(pastMoment));

        int deletedFiles = fileStorage.deleteFilesModifiedBefore(new Date(currentMillis - oldFilesTtl));

        assertEquals(3, deletedFiles);
        assertFalse(Files.exists(oldNonDeployerFile));
        assertFileExists(true, fileEntryToRemain1);
        assertFileExists(true, fileEntryToRemain2);
        assertFileExists(false, fileEntryToDelete1);
        assertFileExists(false, fileEntryToDelete2);
    }

    @Test
    public void processFileContent() throws Exception {
        FileEntry fileEntry = addFile(TEST_FILE_LOCATION);
        String testFileDigest = DigestHelper.computeFileChecksum(Paths.get(TEST_FILE_LOCATION), DIGEST_METHOD)
                                            .toLowerCase();
        validateFileContent(fileEntry, testFileDigest);
    }

    @Test(expected = FileStorageException.class)
    public void testFileContentNotExisting() throws Exception {
        String fileId = "not-existing-file-id";
        String fileSpace = "not-existing-space-id";
        String fileDigest = DigestHelper.computeFileChecksum(Paths.get(TEST_FILE_LOCATION), DIGEST_METHOD)
                                        .toLowerCase();
        FileEntry dummyFileEntry = ImmutableFileEntry.builder()
                                                     .id(fileId)
                                                     .space(fileSpace)
                                                     .build();
        validateFileContent(dummyFileEntry, fileDigest);
    }

    private void validateFileContent(FileEntry storedFile, final String expectedFileChecksum) throws FileStorageException {
        fileStorage.processFileContent(storedFile.getSpace(), storedFile.getId(), contentStream -> {
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

    private FileEntry addFile(String pathString) throws Exception {
        return addFile(pathString, spaceId, namespace);
    }

    private FileEntry addFile(String pathString, String space, String namespace) throws Exception {
        Path testFilePath = Paths.get(pathString)
                                 .toAbsolutePath();
        FileEntry fileEntry = createFileEntry(space, namespace);
        fileStorage.addFile(fileEntry, testFilePath.toFile());
        return fileEntry;
    }

    private FileEntry createFileEntry() {
        return createFileEntry(spaceId, namespace);
    }

    private FileEntry createFileEntry(String space, String namespace) {
        return ImmutableFileEntry.builder()
                                 .id(UUID.randomUUID()
                                         .toString())
                                 .space(space)
                                 .namespace(namespace)
                                 .build();
    }

    private void assertFileExists(boolean exceptedFileExist, FileEntry actualFile) {
        assertEquals(exceptedFileExist, Files.exists(getFileLocation(actualFile)));
    }

    private Path getFileLocation(FileEntry actualFile) {
        return Paths.get(temporaryStorageLocation.toString(), actualFile.getSpace(), "files", actualFile.getId());
    }

}
