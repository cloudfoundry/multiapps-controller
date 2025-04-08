package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.tika.Tika;
import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.stream.DBInputStream;

@Named
public class FileMimeTypeValidator {

    private static final String APPLICATION_OCTET_STREAM_MIME_TYPE = "application/octet-stream";
    private static final String APPLICATION_ZIP_MIME_TYPE = "application/zip";
    private static final String TEXT_PLAIN_MIME_TYPE = "text/plain";
    private static final Tika mimeTypeDetector = new Tika();
    private final DescriptorParserFacadeFactory descriptorParserFactory;
    private final FileService fileService;

    @Inject
    public FileMimeTypeValidator(FileService fileService, DescriptorParserFacadeFactory descriptorParserFactory) {
        this.fileService = fileService;
        this.descriptorParserFactory = descriptorParserFactory;
    }

    public void validateFileType(String spaceGuid, String appArchiveId, Consumer<String> stepLogger) {
        try (InputStream fileInputStream = fileService.openInputStream(spaceGuid, appArchiveId);
            InputStream inputStreamToValidate = (fileInputStream instanceof DBInputStream)
                ? fileInputStream
                : new BufferedInputStream(fileInputStream)) {
            validateInputStreamMimeType(inputStreamToValidate, stepLogger);
        } catch (FileStorageException | IOException e) {
            throw new SLException(e);
        }
    }

    private void validateInputStreamMimeType(InputStream uploadedFileInputStream, Consumer<String> stepLogger) throws IOException {
        String detectedType = getFileMimeType(uploadedFileInputStream);

        switch (detectedType) {
            case TEXT_PLAIN_MIME_TYPE -> validateYamlFile(uploadedFileInputStream, stepLogger);
            case APPLICATION_ZIP_MIME_TYPE, APPLICATION_OCTET_STREAM_MIME_TYPE -> {
            }
            default -> stepLogger.accept(MessageFormat.format(Messages.UNSUPPORTED_FILE_FORMAT, detectedType));
        }

    }

    private String getFileMimeType(InputStream uploadedFileInputStream) throws IOException {
        return mimeTypeDetector.detect(uploadedFileInputStream);
    }

    private void validateYamlFile(InputStream uploadedFileInputStream, Consumer<String> stepLogger) {
        try {
            descriptorParserFactory.getInstanceWithDisabledDuplicateKeys()
                                   .parseExtensionDescriptor(uploadedFileInputStream);
        } catch (ParsingException e) {
            stepLogger.accept(Messages.EXTENSION_DESCRIPTORS_COULD_NOT_BE_PARSED_TO_VALID_YAML);
            stepLogger.accept(e.getMessage());
        }
    }
}