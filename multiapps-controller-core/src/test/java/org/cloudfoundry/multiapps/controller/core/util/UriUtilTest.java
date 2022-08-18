package org.cloudfoundry.multiapps.controller.core.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UriUtilTest {

    static Stream<Arguments> testStripScheme() {
        return Stream.of(Arguments.of("https://host.domain/with/path", "host.domain/with/path"),
                         Arguments.of("host.domain/with/path", "host.domain/with/path"),
                         Arguments.of("https://valid-domain:4000", "valid-domain:4000"),
                         Arguments.of("valid-domain:4000", "valid-domain:4000"));
    }

    @MethodSource
    @ParameterizedTest
    void testStripScheme(String uri, String strippedUri) {
        Assertions.assertEquals(strippedUri, UriUtil.stripScheme(uri));
    }
}
