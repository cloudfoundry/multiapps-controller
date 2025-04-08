package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.InputStream;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FileMimeTypeValidatorTest {

    @Mock
    private FileService fileService;

    private FileMimeTypeValidator fileMimeTypeValidator;

    private final String TEST_SPACE_ID = UUID.randomUUID()
                                             .toString();
    private final String TEST_FILE_ID = UUID.randomUUID()
                                            .toString();

    private DescriptorParserFacadeFactory descriptorParserFacadeFactory;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        int maxAliases = 5;
        ApplicationConfiguration applicationConfiguration = Mockito.mock(ApplicationConfiguration.class);
        when(applicationConfiguration.getSnakeyamlMaxAliasesForCollections())
            .thenReturn(maxAliases);
        descriptorParserFacadeFactory = new DescriptorParserFacadeFactory(applicationConfiguration);
        fileMimeTypeValidator = new FileMimeTypeValidator(fileService, descriptorParserFacadeFactory);
    }

    @Test
    void testValidationWithCorrectYaml() throws FileStorageException {
        InputStream mtadYaml = getClass().getResourceAsStream("valid-format.yaml");
        when(fileService.openInputStream(TEST_SPACE_ID, TEST_FILE_ID)).thenReturn(mtadYaml);
        fileMimeTypeValidator = new FileMimeTypeValidator(fileService, descriptorParserFacadeFactory);
        fileMimeTypeValidator.validateFileType(TEST_SPACE_ID, TEST_FILE_ID, message -> {
        });
    }

    @Test
    void testValidationWithDuplicateKeys() throws FileStorageException {
        InputStream mtadYaml = getClass().getResourceAsStream("duplicate-key.yaml");
        when(fileService.openInputStream(TEST_SPACE_ID, TEST_FILE_ID)).thenReturn(mtadYaml);
        fileMimeTypeValidator = new FileMimeTypeValidator(fileService, descriptorParserFacadeFactory);
        fileMimeTypeValidator.validateFileType(TEST_SPACE_ID, TEST_FILE_ID, message -> {
            boolean doesMessageContainWarningMessage = message.contains(Messages.EXTENSION_DESCRIPTORS_COULD_NOT_BE_PARSED_TO_VALID_YAML);
            boolean doesMessageContainException = message.contains("Error while parsing YAML string");
            assertTrue(doesMessageContainException || doesMessageContainWarningMessage);
        });
    }
}
