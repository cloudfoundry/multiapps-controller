package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import jakarta.inject.Named;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

@Named
public class FileMimeTypeValidator {

    private static final String APPLICATION_ZIP_MIME_TYPE = "application/zip";
    private static final String APPLICATION_OCTET_STREAM_MIME_TYPE = "application/octet-stream";
    private static final String TEXT_PLAIN_MIME_TYPE = "text/plain";
    private static final String YAML_FILE_EXTENSION = "yaml";
    private static final String EXTENSION_DESCRIPTOR_FILE_EXTENSION = "mtaext";
    private static final Tika tika = new Tika();

    public void validateMultipartFileMimeType(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException(Messages.THE_PROVIDED_MULTIPART_FILE_CANNOT_BE_EMPTY);
        }

        try {
            validateInputStreamMimeType(multipartFile.getInputStream(), multipartFile.getOriginalFilename());
        } catch (IOException e) {
            throw new SLException(e);
        }
    }

    public void validateInputStreamMimeType(InputStream uploadedFileInputStream, String filename) throws IOException {
        String detectedType = getFileMimeType(uploadedFileInputStream);
        switch (detectedType) {
            case TEXT_PLAIN_MIME_TYPE -> validateYamlFile(uploadedFileInputStream, filename);
            case APPLICATION_ZIP_MIME_TYPE, APPLICATION_OCTET_STREAM_MIME_TYPE -> {
            }
            default -> throw new IllegalArgumentException(MessageFormat.format(Messages.UNSUPPORTED_FILE_FORMAT, detectedType));
        }
    }

    private String getFileMimeType(InputStream uploadedFileInputStream) throws IOException {
        return tika.detect(uploadedFileInputStream);
    }

    private void validateYamlFile(InputStream uploadedFileInputStream, String filename) {
        validateTextFileExtension(filename);
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try {
            yaml.load(uploadedFileInputStream);
        } catch (YAMLException e) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.THE_PROVIDED_0_FILE_IS_INVALID, filename), e);
        }
    }

    private void validateTextFileExtension(String filename) {
        String fileExtension = FilenameUtils.getExtension(filename);

        if (!(YAML_FILE_EXTENSION.equals(fileExtension) || EXTENSION_DESCRIPTOR_FILE_EXTENSION.equals(fileExtension))) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.THE_PROVIDED_0_FILE_IS_INVALID, filename));
        }
    }
}
