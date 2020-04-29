package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.net.MediaType;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableFileEntry;
import com.sap.cloud.lm.sl.common.util.DigestHelper;

public class ObjectStoreFileStorageTest {

    private static final String TEST_FILE_LOCATION = "src/test/resources/pexels-photo-401794.jpeg";
    private static final String SECOND_FILE_TEST_LOCATION = "src/test/resources/pexels-photo-463467.jpeg";
    private static final String DIGEST_METHOD = "MD5";
    private static final String CONTAINER = "container4e";

    private String spaceId;
    private String namespace;

    private FileStorage fileStorage;

    private BlobStoreContext blobStoreContext;

    @Before
    public void setUp() {
        createBlobStoreContext();
        fileStorage = new ObjectStoreFileStorage(blobStoreContext.getBlobStore(), CONTAINER) {
            @Override
            protected long getRetryWaitTime() {
                return 1;
            }
        };
        spaceId = UUID.randomUUID()
                      .toString();
        namespace = UUID.randomUUID()
                        .toString();
    }

    private void createBlobStoreContext() {
        blobStoreContext = ContextBuilder.newBuilder("transient")
                                         .buildView(BlobStoreContext.class);
        blobStoreContext.getBlobStore()
                        .createContainerInLocation(null, CONTAINER);
    }

    @After
    public void tearDown() {
        blobStoreContext.close();
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

    @Test
    public void deleteFilesBySpaceAndNamespace() throws Exception {
        FileEntry firstFile = addFile(TEST_FILE_LOCATION);
        FileEntry secondFile = addFile(SECOND_FILE_TEST_LOCATION);
        FileEntry fileInOtherSpace = addFile(TEST_FILE_LOCATION, "otherspace", namespace);
        FileEntry fileInOtherNamespace = addFile(TEST_FILE_LOCATION, spaceId, "othernamespace");

        fileStorage.deleteFilesBySpaceAndNamespace(spaceId, namespace);
        assertFileExists(true, fileInOtherNamespace);
        assertFileExists(true, fileInOtherSpace);
        assertFileExists(false, firstFile);
        assertFileExists(false, secondFile);

    }

    @Test
    public void deleteFilesModifiedBefore() throws Exception {
        long currentMillis = System.currentTimeMillis();
        final long oldFilesTtl = 1000 * 60 * 10; // 10min
        final long pastMoment = currentMillis - 1000 * 60 * 15; // before 15min

        FileEntry fileEntryToRemain1 = addFile(TEST_FILE_LOCATION);
        FileEntry fileEntryToRemain2 = addFile(SECOND_FILE_TEST_LOCATION);
        FileEntry fileEntryToDelete1 = addFile(TEST_FILE_LOCATION, spaceId, namespace, new Date(pastMoment));
        FileEntry fileEntryToDelete2 = addFile(SECOND_FILE_TEST_LOCATION, spaceId, null, new Date(pastMoment));

        String blobWithNoMetadataId = addBlobWithNoMetadata();

        int deletedFiles = fileStorage.deleteFilesModifiedBefore(new Date(currentMillis - oldFilesTtl));

        assertEquals(3, deletedFiles);
        assertFileExists(true, fileEntryToRemain1);
        assertFileExists(true, fileEntryToRemain2);
        assertFileExists(false, fileEntryToDelete1);
        assertFileExists(false, fileEntryToDelete2);
        assertNull(blobStoreContext.getBlobStore()
                                   .getBlob(CONTAINER, blobWithNoMetadataId));
    }

    private String addBlobWithNoMetadata() throws Exception {
        BlobStore blobStore = blobStoreContext.getBlobStore();
        Path path = Paths.get(TEST_FILE_LOCATION);
        long fileSize = FileUtils.sizeOf(path.toFile());
        String id = UUID.randomUUID()
                        .toString();
        Blob blob = blobStore.blobBuilder(id)
                             .payload(new FileInputStream(path.toFile()))
                             .contentDisposition(path.getFileName()
                                                     .toString())
                             .contentType(MediaType.OCTET_STREAM.toString())
                             .contentLength(fileSize)
                             .build();
        blobStore.putBlob(CONTAINER, blob);
        return id;
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
        return addFile(pathString, space, namespace, null);
    }

    private FileEntry addFile(String pathString, String space, String namespace, Date date) throws Exception {
        Path testFilePath = Paths.get(pathString)
                                 .toAbsolutePath();
        FileEntry fileEntry = createFileEntry(space, namespace);
        fileEntry = enrichFileEntry(fileEntry, testFilePath, date);
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

    private FileEntry enrichFileEntry(FileEntry fileEntry, Path path, Date date) {
        long sizeOfFile = FileUtils.sizeOf(path.toFile());
        BigInteger bigInteger = BigInteger.valueOf(sizeOfFile);
        return ImmutableFileEntry.builder()
                                 .from(fileEntry)
                                 .size(bigInteger)
                                 .modified(date != null ? date : new Date(System.currentTimeMillis()))
                                 .name(path.getFileName()
                                           .toString())
                                 .build();
    }

    private void assertFileExists(boolean exceptedFileExist, FileEntry actualFile) {
        Blob blob = blobStoreContext.getBlobStore()
                                    .getBlob(CONTAINER, actualFile.getId());
        boolean blobExists = blob != null;

        assertEquals(exceptedFileExist, blobExists);
    }

}
