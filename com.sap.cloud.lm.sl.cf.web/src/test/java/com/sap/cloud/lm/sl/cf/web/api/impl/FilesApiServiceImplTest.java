package com.sap.cloud.lm.sl.cf.web.api.impl;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileUploadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.util.DefaultConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.model.FileMetadata;

public class FilesApiServiceImplTest {

    @Mock
    private FileService fileService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private FileItemIterator fileItemIterator;

    @Mock
    private FileItemStream fileItemStream;

    @Mock
    private ServletFileUpload servletFileUpload;

    @Mock
    private SecurityContext securityContext;

    private static final long MAX_PERMITTED_SIZE = new DefaultConfiguration().getMaxUploadSize();

    @InjectMocks
    private FilesApiServiceImpl testedClass = new FilesApiServiceImpl() {
        @Override
        protected ServletFileUpload getFileUploadServlet() {
            return servletFileUpload;
        };

    };

    private static final String SPACE_GUID = "896e6be9-8217-4a1c-b938-09b30966157a";
    private static final String NAMESPACE_GUID = "0a42c085-b772-4b1e-bf4d-75c463aab5f6";

    private static final String DIGEST_CHARACTER_TABLE = "123456789ABCDEF";

    @Before
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        AuditLoggingProvider.setFacade(Mockito.mock(AuditLoggingFacade.class));
    }

    @Test
    public void testGetMtaFiles() throws Exception {
        FileEntry entryOne = createFileEntry("test.mtar");
        FileEntry entryTwo = createFileEntry("extension.mtaet");
        Mockito.when(fileService.listFiles(Mockito.eq(SPACE_GUID), Mockito.eq(null)))
               .thenReturn(Arrays.asList(entryOne, entryTwo));
        Response response = testedClass.getMtaFiles(securityContext, SPACE_GUID);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        List<FileMetadata> files = (List<FileMetadata>) response.getEntity();
        assertEquals(2, files.size());
        assertMetadataMatches(entryOne, files.get(0));
        assertMetadataMatches(entryTwo, files.get(1));

    }

    @Test
    public void testGetMtaFilesError() throws Exception {
        Mockito.when(fileService.listFiles(Mockito.eq(SPACE_GUID), Mockito.eq(null)))
               .thenThrow(new FileStorageException("error"));
        Assertions.assertThrows(WebApplicationException.class, () -> testedClass.getMtaFiles(securityContext, SPACE_GUID));
    }

    @Test
    public void testUploadMtaFile() throws Exception {
        String fileName = "test.mtar";
        FileEntry fileEntry = createFileEntry(fileName);

        Mockito.when(servletFileUpload.getItemIterator(Mockito.eq(request)))
               .thenReturn(fileItemIterator);
        Mockito.when(fileItemIterator.hasNext())
               .thenReturn(true, true, false); // has two form entries
        Mockito.when(fileItemIterator.next())
               .thenReturn(fileItemStream);
        Mockito.when(fileItemStream.isFormField())
               .thenReturn(false, true); // only first entry is a file
        Mockito.when(fileItemStream.openStream())
               .thenReturn(Mockito.mock(InputStream.class));
        Mockito.when(fileItemStream.getName())
               .thenReturn(fileName);
        Mockito.when(fileService.addFile(Mockito.eq(SPACE_GUID), Mockito.eq(fileName), (FileUploadProcessor) Mockito.any(), Mockito.any()))
               .thenReturn(fileEntry);

        Response response = testedClass.uploadMtaFile(request, securityContext, SPACE_GUID);

        Mockito.verify(servletFileUpload)
               .setSizeMax(Mockito.eq(new DefaultConfiguration().getMaxUploadSize()));
        Mockito.verify(fileItemIterator, Mockito.times(3))
               .hasNext();
        Mockito.verify(fileItemStream)
               .openStream();
        Mockito.verify(fileService)
               .addFile(Mockito.eq(SPACE_GUID), Mockito.eq(fileName), (FileUploadProcessor) Mockito.any(), Mockito.any());

        FileMetadata fileMetadata = (FileMetadata) response.getEntity();
        assertMetadataMatches(fileEntry, fileMetadata);
    }

    @Test
    public void testUploadMtaFileErrorSizeExceeded() throws Exception {
        Mockito.when(servletFileUpload.getItemIterator(Mockito.eq(request)))
               .thenThrow(new SizeLimitExceededException("size limit exceeded", MAX_PERMITTED_SIZE + 1024, MAX_PERMITTED_SIZE));
        Assertions.assertThrows(WebApplicationException.class, () -> testedClass.uploadMtaFile(request, securityContext, SPACE_GUID));
    }

    private void assertMetadataMatches(FileEntry expected, FileMetadata actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getSpace(), actual.getSpace());
        assertEquals(expected.getSize(), actual.getSize());
        assertEquals(expected.getDigest(), actual.getDigest());
        assertEquals(expected.getDigestAlgorithm(), actual.getDigestAlgorithm());
    }

    private FileEntry createFileEntry(String name) {
        FileEntry entry = new FileEntry();
        entry.setId(UUID.randomUUID()
                        .toString());
        entry.setDigest(generateRandomDigest());
        entry.setDigestAlgorithm("MD5");
        entry.setName(name);
        entry.setNamespace(NAMESPACE_GUID);
        entry.setSize(BigInteger.valueOf(new Random().nextInt(1024 * 1024 * 10)));
        entry.setSpace(SPACE_GUID);
        return entry;
    }

    private String generateRandomDigest() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            builder.append(DIGEST_CHARACTER_TABLE.charAt(new Random().nextInt(DIGEST_CHARACTER_TABLE.length())));
        }
        return builder.toString();
    }
}
