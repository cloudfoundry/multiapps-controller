package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NamespaceConverterTest {

    private static final NamespaceConverter namespaceConverter = new NamespaceConverter();
    private static final Object CORRECT_NAMESPACE_NAME = "test-test-test-test-test";
    private static final Object INCORRECT_NAMESPACE_NAME = "test-test-test-test-test-test-test-test-test-test";

    @Test
    void testCorrectConversion() {
        Assertions.assertEquals("test-test-test-test-test", namespaceConverter.convert(CORRECT_NAMESPACE_NAME));
    }

    @Test
    void testExceptionIsThrownWhenInvalidNamepaceIsGiven() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> namespaceConverter.convert(INCORRECT_NAMESPACE_NAME));
    }
}
