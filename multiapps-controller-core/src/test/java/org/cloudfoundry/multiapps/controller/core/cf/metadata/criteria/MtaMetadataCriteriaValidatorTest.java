package org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MtaMetadataCriteriaValidatorTest {

    static Stream<Arguments> testValidateLabelKey() {
        return Stream.of(
        // @formatter:off
                      // (1) Blank prefix
                      Arguments.of("", true, "Metadata's label key, should not be empty"),
                      // (2) Exceeded max length
                      Arguments.of(StringUtils.repeat('a', 65), true, 
                                   "Metadata's label key, should not be longer than \"63\" characters. Currently it is \"65\" characters with value \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\""),
                      // (3) Start with non alphanumeric
                      Arguments.of("-foo", true, "Metadata's label key, should start with (alphanumeric character). Currently it does not and the value is \"-foo\""),
                      // (4) End with non alphanumeric
                      Arguments.of("foo-", true, "Metadata's label key, should end with (alphanumeric character). Currently it does not and the value is \"foo-\""),
                      // (5) Not match to custom pattern
                      Arguments.of("foo$bar", true, "Metadata's label key, should (contain only alphanumeric characters, \"-\", \"_\" or \".\"). Currently it does not and the value is \"foo$bar\""),
                      // (6) Valid Label key
                      Arguments.of("foo-bar_1.0", false, null)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testValidateLabelKey(String labelKey, boolean shouldThrowException, String expectedExceptionMessage) {
        assertValidation(() -> MtaMetadataCriteriaValidator.validateLabelKey(labelKey), shouldThrowException, expectedExceptionMessage);
    }

    private void assertValidation(Executable executable, boolean shouldThrowException, String expectedExceptionMessage) {
        if (shouldThrowException) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
            assertEquals(expectedExceptionMessage, exception.getMessage());
            return;
        }
        assertDoesNotThrow(executable);
    }

    static Stream<Arguments> testValidateLabelValue() {
        return Stream.of(
                         // (1) Blank prefix
                         Arguments.of("", false, null),
                         // (2) Exceeded max length
                         Arguments.of(StringUtils.repeat('a', 65), true,
                                      "Metadata's label value, should not be longer than \"63\" characters. Currently it is \"65\" characters with value \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\""),
                         // (3) Start with non alphanumeric
                         Arguments.of("-foo", true,
                                      "Metadata's label value, should start with (alphanumeric character). Currently it does not and the value is \"-foo\""),
                         // (4) End with non alphanumeric
                         Arguments.of("foo-", true,
                                      "Metadata's label value, should end with (alphanumeric character). Currently it does not and the value is \"foo-\""),
                         // (5) Not match to custom pattern
                         Arguments.of("foo$bar", true,
                                      "Metadata's label value, should (contain only alphanumeric characters, \"-\", \"_\" or \".\"). Currently it does not and the value is \"foo$bar\""),
                         // (6) Valid Label value
                         Arguments.of("quux", false, null));
    }

    @ParameterizedTest
    @MethodSource
    void testValidateLabelValue(String labelValue, boolean shouldThrowException, String expectedExceptionMessage) {
        assertValidation(() -> MtaMetadataCriteriaValidator.validateLabelValue(labelValue), shouldThrowException, expectedExceptionMessage);
    }

}
