package org.cloudfoundry.multiapps.controller.process.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LazyArchiveInputStreamTest {

    private static final String FILE_ID_1 = "file-1";
    private static final String FILE_ID_2 = "file-2";
    private static final String CUSTOM_SPACE = "custom-space";

    @Mock
    private FileService fileService;
    @Mock
    private StepLogger stepLogger;

    private LazyArchiveInputStream lazyArchiveInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void readFromFirstArchivePart() throws IOException, FileStorageException {
        prepareStream(2);
        mockStream(0, 1, FILE_ID_1);
        int read = lazyArchiveInputStream.read();
        assertEquals(0, read);
        assertEquals(1, lazyArchiveInputStream.available());
    }

    @Test
    void readFromSecondArchivePart() throws IOException, FileStorageException {
        prepareStream(2);
        InputStream firstPartInputStream = mockStream(-1, 0, FILE_ID_1);
        mockStream(10, 1, FILE_ID_2);
        int read = lazyArchiveInputStream.read();
        assertEquals(10, read);
        assertEquals(1, lazyArchiveInputStream.available());
        verify(firstPartInputStream).close();
    }

    @Test
    void readWhenEOFReached() throws IOException, FileStorageException {
        prepareStream(0);
        InputStream firstPartInputStream = mockStream(-1, 0, FILE_ID_1);
        InputStream secondPartInputStream = mockStream(-1, 0, FILE_ID_2);
        lazyArchiveInputStream.read();
        int read = lazyArchiveInputStream.read();
        assertEquals(-1, read);
        assertEquals(0, lazyArchiveInputStream.available());
        verify(firstPartInputStream).close();
        verify(secondPartInputStream).close();
    }

    @Test
    void readBufferedFromFirstArchivePart() throws IOException, FileStorageException {
        prepareStream(2);
        mockStream(10, 1, FILE_ID_1);
        int bytesRead = lazyArchiveInputStream.read(new byte[1], 0, 1);
        assertEquals(1, bytesRead);
        assertEquals(1, lazyArchiveInputStream.available());
    }

    @Test
    void readBufferedFromSecondArchivePart() throws IOException, FileStorageException {
        prepareStream(2);
        InputStream firstPartInputStream = mockStream(-1, 0, FILE_ID_1);
        mockStream(10, 1, FILE_ID_2);
        int bytesRead = lazyArchiveInputStream.read(new byte[1], 0, 1);
        assertEquals(1, bytesRead);
        assertEquals(1, lazyArchiveInputStream.available());
        verify(firstPartInputStream).close();
    }

    @Test
    void readBufferedWhenEOFReached() throws IOException, FileStorageException {
        prepareStream(0);
        InputStream firstPartInputStream = mockStream(-1, 0, FILE_ID_1);
        InputStream secondPartInputStream = mockStream(-1, 0, FILE_ID_2);
        lazyArchiveInputStream.read(new byte[1], 0, 1);
        int read = lazyArchiveInputStream.read(new byte[1], 0, 1);
        assertEquals(-1, read);
        assertEquals(0, lazyArchiveInputStream.available());
        verify(firstPartInputStream).close();
        verify(secondPartInputStream).close();
    }

    private void prepareStream(int archiveSize) {
        lazyArchiveInputStream = new LazyArchiveInputStream(fileService, buildMockedFileEntries(), stepLogger, archiveSize);
    }

    private List<FileEntry> buildMockedFileEntries() {
        FileEntry fileEntry1 = buildFileEntry(FILE_ID_1, CUSTOM_SPACE);
        FileEntry fileEntry2 = buildFileEntry(FILE_ID_2, CUSTOM_SPACE);
        return List.of(fileEntry1, fileEntry2);
    }

    private FileEntry buildFileEntry(String id, String space) {
        return ImmutableFileEntry.builder()
                                 .id(id)
                                 .space(space)
                                 .size(BigInteger.valueOf(1))
                                 .build();
    }

    private InputStream mockStream(int firstValueFromStream, int totalBytesRead, String fileId) throws IOException, FileStorageException {
        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            buffer[0] = (byte) firstValueFromStream;
            return totalBytesRead;
        });
        when(fileService.openInputStream(CUSTOM_SPACE, fileId)).thenReturn(inputStream);
        return inputStream;
    }

}
