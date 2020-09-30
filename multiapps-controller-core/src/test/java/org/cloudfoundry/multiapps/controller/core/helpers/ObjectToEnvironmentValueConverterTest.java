package org.cloudfoundry.multiapps.controller.core.helpers;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ObjectToEnvironmentValueConverterTest {

    private final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testConvert() {
        return Stream.of(
// @formatter:off
            // (0) There are escape sequences and they are preceded by 0 additional escape characters:
            Arguments.of("object-00.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-00.txt"))),
            // (1) There are no escape sequences:
            Arguments.of("object-01.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-01.txt"))),
            // (2) There are no escape sequences, because the marker characters are preceded by escaped escape characters:
            Arguments.of("object-02.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-02.txt"))),
            // (3) There are escape sequences and they are preceded by 2 additional escape characters:
            Arguments.of("object-03.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-03.txt"))),
            // (4) There are custom and other escape sequences:
            Arguments.of("object-04.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-04.txt"))),
            // (5) The object is a string:
            Arguments.of("object-05.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-05.txt")))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testConvert(String objectJsonFilePath, Expectation expectation) {
        tester.test(() -> new ObjectToEnvironmentValueConverter(true).convert(loadObject(objectJsonFilePath)), expectation);
    }

    private Object loadObject(String objectJsonFilePath) {
        String objectAsAString = TestUtil.getResourceAsString(objectJsonFilePath, getClass());
        return JsonUtil.fromJson(objectAsAString, Object.class);
    }

    private static String getResourceAsString(String resource) {
        return TestUtil.getResourceAsString(resource, ObjectToEnvironmentValueConverterTest.class);
    }

}
