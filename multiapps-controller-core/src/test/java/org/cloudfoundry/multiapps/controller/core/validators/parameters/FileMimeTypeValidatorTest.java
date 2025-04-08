package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

class FileMimeTypeValidatorTest {

    private MultipartFile multipartFile;
    private InputStream inputStream;
    private final FileMimeTypeValidator fileMimeTypeValidator = new FileMimeTypeValidator(null, null);

    @BeforeEach
    void setUp() {
        multipartFile = Mockito.mock(MultipartFile.class);
    }

}
