package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class FileMimeTypeValidatorTest {

    private MultipartFile multipartFile;
    private InputStream inputStream;
    private final FileMimeTypeValidator fileMimeTypeValidator = new FileMimeTypeValidator();

    @BeforeEach
    void setUp() {
        multipartFile = Mockito.mock(MultipartFile.class);
    }

    @Test
    void testValidateMultipartFileMimeTypeWithNullFile() {
        assertThrows(IllegalArgumentException.class, () -> {
            fileMimeTypeValidator.validateMultipartFileMimeType(null);
        }, Messages.THE_PROVIDED_MULTIPART_FILE_CANNOT_BE_EMPTY);
    }

    @Test
    void testValidateMultipartFileMimeTypeWithEmptyFile() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            fileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
        }, Messages.THE_PROVIDED_MULTIPART_FILE_CANNOT_BE_EMPTY);
    }

    @Test
    void testValidateMultipartFileMimeType_ValidYamlFile() throws Exception {
        inputStream = new ByteArrayInputStream("test: test".getBytes());

        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("valid.yaml");

        fileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
    }

    @Test
    void testValidateMultipartFileOctetStreamMimeType() throws Exception {
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("valid.zip");

        fileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
    }

    @Test
    void testValidateMultipartFileOApplicationZipMimeType() throws Exception {
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getOriginalFilename()).thenReturn("valid.zip");

        fileMimeTypeValidator.validateMultipartFileMimeType(multipartFile);
    }
}
