package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class FileMimeTypeValidatorTest {

    private Tika tika;
    private MultipartFile multipartFile;
    private InputStream inputStream;

    @BeforeEach
    void setUp() {
        tika = Mockito.mock(Tika.class);
        multipartFile = Mockito.mock(MultipartFile.class);
    }

    @Test
    void testValidateMultipartFileMimeTypeWithNullFile() {
        assertThrows(IllegalArgumentException.class, () -> {
            FileMimeTypeValidator.validateMultipartFileMimeType(null);
        }, Messages.INVALID_MULTIPART_FILE);
    }

    @Test
    void testValidateMultipartFileMimeTypeWithEmptyFile() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            FileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
        }, Messages.INVALID_MULTIPART_FILE);
    }

    @Test
    void testValidateMultipartFileMimeType_ValidYamlFile() throws Exception {
        inputStream = new ByteArrayInputStream("test: test".getBytes());

        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("valid.yaml");
        when(tika.detect(inputStream)).thenReturn("text/plain");

        FileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
    }

    @Test
    void testValidateMultipartFileOctetStreamMimeType() throws Exception {
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("valid.zip");
        when(tika.detect(inputStream)).thenReturn("application/octet-stream");

        FileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
    }

    @Test
    void testValidateMultipartFileOApplicationZipMimeType() throws Exception {
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("valid.zip");
        when(tika.detect(inputStream)).thenReturn("application/zip");

        FileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
    }
}
