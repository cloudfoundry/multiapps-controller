package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CfUserMetadata;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;

public class CfMetadataValidator {

    private static final Pattern KEY_PATTERN = Pattern.compile(
        "([a-zA-Z0-9][a-zA-Z0-9\\-_.]{0,251}[a-zA-Z0-9]/)?[a-zA-Z0-9][a-zA-Z0-9\\-_.]{0,61}[a-zA-Z0-9]|[a-zA-Z0-9]");

    private static final Set<String> RESERVED_KEY_NAMES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(MtaMetadataLabels.MTA_ID,
                                   MtaMetadataLabels.MTA_NAMESPACE,
                                   MtaMetadataLabels.SPACE_GUID,
                                   MtaMetadataAnnotations.MTA_ID,
                                   MtaMetadataAnnotations.MTA_VERSION,
                                   MtaMetadataAnnotations.MTA_NAMESPACE,
                                   MtaMetadataAnnotations.MTA_MODULE,
                                   MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES,
                                   MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES,
                                   MtaMetadataAnnotations.MTA_RESOURCE)));

    private static final String MTA_KEY_PREFIX = "mta_";
    private static final int MAX_LABEL_VALUE_LENGTH = 63;
    private static final int MAX_ANNOTATION_VALUE_LENGTH = 5000;

    public void validate(CfUserMetadata metadata) {
        metadata.getLabels()
                .forEach((key, value) -> validateLabelEntry(key, value));
        metadata.getAnnotations()
                .forEach((key, value) -> validateAnnotationEntry(key, value));
    }

    private void validateLabelEntry(String key, String value) {
        validateKeyFormat(key);
        validateKeyNotReserved(key);
        if (value != null && value.length() > MAX_LABEL_VALUE_LENGTH) {
            throw new ContentException(MessageFormat.format(Messages.CF_METADATA_LABEL_VALUE_TOO_LONG_0, key));
        }
    }

    private void validateAnnotationEntry(String key, String value) {
        validateKeyFormat(key);
        validateKeyNotReserved(key);
        if (value != null && value.length() > MAX_ANNOTATION_VALUE_LENGTH) {
            throw new ContentException(MessageFormat.format(Messages.CF_METADATA_ANNOTATION_VALUE_TOO_LONG_0, key));
        }
    }

    private void validateKeyFormat(String key) {
        if (!KEY_PATTERN.matcher(key)
                        .matches()) {
            throw new ContentException(MessageFormat.format(Messages.CF_METADATA_INVALID_LABEL_KEY_0, key));
        }
    }

    private void validateKeyNotReserved(String key) {
        String nameSegment = extractNameSegment(key);
        if (nameSegment.startsWith(MTA_KEY_PREFIX) || RESERVED_KEY_NAMES.contains(nameSegment) || RESERVED_KEY_NAMES.contains(key)) {
            throw new ContentException(MessageFormat.format(Messages.CF_METADATA_RESERVED_LABEL_KEY_0, key));
        }
    }

    private String extractNameSegment(String key) {
        int slashIndex = key.indexOf('/');
        return slashIndex >= 0 ? key.substring(slashIndex + 1) : key;
    }

}
