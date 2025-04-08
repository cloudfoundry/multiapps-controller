package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.zip.ZipInputStream;

import org.apache.tika.Tika;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class FileMimeTypeValidator {

    private static final String APPLICATION_ZIP_MIME_TYPE = "application/zip";
    private static final String APPLICATION_OCTET_STREAM_MIME_TYPE = "application/octet-stream";
    private static final String TEXT_PLAIN_MIME_TYPE = "text/plain";

    private FileMimeTypeValidator() {
    }

    public static void validateMultipartFileMimeType(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException(Messages.INVALID_MULTIPART_FILE);
        }multipartFile.getName()
        try {
            validateInputStreamMimeType(multipartFile.getInputStream());
        } catch (IOException e) {
            throw new SLException(e);
        }
    }

    public static void validateInputStreamMimeType(InputStream uploadedFileInputStream) throws IOException {
        String detectedType = getFileMimeType(uploadedFileInputStream);

        switch (detectedType) {
            case APPLICATION_ZIP_MIME_TYPE -> validateZipFile(uploadedFileInputStream);
            case TEXT_PLAIN_MIME_TYPE -> validateYamlFile(uploadedFileInputStream);
            case APPLICATION_OCTET_STREAM_MIME_TYPE -> {
                // We skip the validation because we enter this case during chunk deploy
            }
            default -> throw new IllegalArgumentException(MessageFormat.format(Messages.UNSUPPORTED_FILE_FORMAT, detectedType));
        }
    }

    private static String getFileMimeType(InputStream uploadedFileInputStream) throws IOException {
        Tika tika = new Tika();
        return tika.detect(uploadedFileInputStream);
    }

    private static void validateZipFile(InputStream uploadedFileInputStream) throws IOException {
        try (InputStream fileContent = uploadedFileInputStream; ZipInputStream fileZipContent = new ZipInputStream(fileContent)) {
            if (fileZipContent.getNextEntry() == null) {
                throw new IllegalArgumentException(Messages.INVALID_MTAR_FILE);
            }
        }
    }

    private static void validateYamlFile(InputStream uploadedFileInputStream) {
        Yaml yaml = new Yaml();
        try {
            yaml.load(uploadedFileInputStream);
        } catch (YAMLException e) {
            throw new IllegalArgumentException(Messages.INVALID_YAML_FILE);
        }
    }
}
