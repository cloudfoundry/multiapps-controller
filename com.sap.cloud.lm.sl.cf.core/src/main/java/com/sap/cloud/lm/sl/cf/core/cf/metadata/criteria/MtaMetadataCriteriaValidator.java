package com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

class MtaMetadataCriteriaValidator {

    private static final String SHOULD_COMPLY_WITH_PATTERN = "{0}, should ({1}). Currently it does not and the value is \"{2}\"";
    private static final String SHOULD_CONTAIN = "{0}, should contain ({1}). Currently it does not and the value is \"{2}\"";
    private static final String SHOULD_NOT_BE_LONGER_THAN = "{0}, should not be longer than \"{1}\" characters. Currently it is \"{2}\" characters with value \"{3}\"";
    private static final String SHOULD_END_WITH = "{0}, should end with ({1}). Currently it does not and the value is \"{2}\"";
    private static final String ANY_CHARS = ".*";
    private static final String ALPHANUMERIC_CHARACTER_PATTERN = "[A-Za-z0-9]";
    private static final String ALPHANUMERIC_CHARACTER = "alphanumeric character";
    private static final String SHOULD_NOT_BE_EMPTY = "{0}, should not be empty";
    private static final String LABEL_KEY_PATTERN_DESCRIPTION = "contain only alphanumeric characters, \"-\", \"_\" or \".\"";
    private static final String LABEL_KEY_PATTERN = "[A-Za-z0-9-_\\.]*";
    private static final String LABEL_KEY_PREFIX_PATTERN_DESCRIPTION = "contain only alphanumeric characters, \".\" or \"-\"";
    private static final String FORWARD_SLASH = "/";
    private static final String DOT = ".";
    private static final String LABEL_KEY_PREFIX_PATTERN = "[A-Za-z0-9\\.-]+";
    private static final String LABLE_KEY_PREFIX = "Metadata's lable key prefix";
    private static final String LABLE_KEY = "Metadata's lable key";
    private static final String LABLE_VALUE = "Metadata's lable value";
    private static final int MAX_LABEL_LEY_PREFIX_LENGTH = 253;
    private static final int MAX_LABEL_KEY_LENGTH = 63;
    private static final int MAX_LABEL_VALUE_LENGTH = 63;

    public static void validateLabelKeyPrefix(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return;
        }
        validateMaxLength(prefix, LABLE_KEY_PREFIX, MAX_LABEL_LEY_PREFIX_LENGTH);
        validateContains(prefix, LABLE_KEY_PREFIX, DOT);
        validateEndsWith(prefix, LABLE_KEY_PREFIX, FORWARD_SLASH, FORWARD_SLASH);
        validateCustomPatternMatches(prefix, LABLE_KEY_PREFIX, LABEL_KEY_PREFIX_PATTERN, LABEL_KEY_PREFIX_PATTERN_DESCRIPTION);
    }

    public static void validateLabelKey(String key) {
        validateNotBlank(key, LABLE_KEY);
        validateMaxLength(key, LABLE_KEY, MAX_LABEL_KEY_LENGTH);
        validateStartsWithAlphanumeric(key, LABLE_KEY);
        validateEndsWithAlphanumeric(key, LABLE_KEY);
        validateCustomPatternMatches(key, LABLE_KEY, LABEL_KEY_PATTERN, LABEL_KEY_PATTERN_DESCRIPTION);
    }

    public static void validateLabelValue(String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        validateMaxLength(value, LABLE_VALUE, MAX_LABEL_VALUE_LENGTH);
        validateStartsWithAlphanumeric(value, LABLE_VALUE);
        validateEndsWithAlphanumeric(value, LABLE_VALUE);
        validateCustomPatternMatches(value, LABLE_VALUE, LABEL_KEY_PATTERN, LABEL_KEY_PATTERN_DESCRIPTION);
    }

    private static void validateNotBlank(String value, String valueName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(MessageFormat.format(SHOULD_NOT_BE_EMPTY, valueName));
        }
    }

    private static void validateStartsWithAlphanumeric(String value, String valueName) {
        validateStartsWith(value, valueName, ALPHANUMERIC_CHARACTER_PATTERN, ALPHANUMERIC_CHARACTER);
    }

    private static void validateEndsWithAlphanumeric(String value, String valueName) {
        validateEndsWith(value, valueName, ALPHANUMERIC_CHARACTER_PATTERN, ALPHANUMERIC_CHARACTER);
    }

    private static void validateStartsWith(String value, String valueName, String pattern, String patternDescription) {
        if (!value.matches(pattern + ANY_CHARS)) {
            throw new IllegalArgumentException(MessageFormat.format(SHOULD_END_WITH, valueName, patternDescription, value));
        }
    }

    private static void validateEndsWith(String value, String valueName, String pattern, String patternDescription) {
        if (!value.matches(ANY_CHARS + pattern)) {
            throw new IllegalArgumentException(MessageFormat.format(SHOULD_END_WITH, valueName, patternDescription, value));
        }
    }

    private static void validateMaxLength(String value, String valueName, int maxLength) {
        int valueLength = StringUtils.length(value);
        if (valueLength > maxLength) {
            throw new IllegalArgumentException(
                MessageFormat.format(SHOULD_NOT_BE_LONGER_THAN, valueName, maxLength, valueLength, value));
        }
    }

    private static void validateContains(String value, String valueName, String sequence) {
        if (!value.contains(sequence)) {
            throw new IllegalArgumentException(MessageFormat.format(SHOULD_CONTAIN, valueName, sequence, value));
        }
    }

    private static void validateCustomPatternMatches(String value, String valueName, String pattern,
        String patternContentDesxcription) {
        if (!value.matches(pattern)) {
            throw new IllegalArgumentException(
                MessageFormat.format(SHOULD_COMPLY_WITH_PATTERN, valueName, patternContentDesxcription, value));
        }
    }
}