package org.cloudfoundry.multiapps.controller.web.util;

import java.io.InputStream;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.web.util.bar.Bar;
import org.cloudfoundry.multiapps.controller.web.util.foo.Foo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

class XmlNamespaceIgnoringHttpMessageConverterTest {

    private final Tester tester = Tester.forClass(getClass());
    private final XmlNamespaceIgnoringHttpMessageConverter converter = new XmlNamespaceIgnoringHttpMessageConverter();

    public static Stream<Arguments> testReadFrom() {
        return Stream.of(
// @formatter:off
            // (0) Attempt to deserialize an entity with a namespace declared in package-info.java (the input DOES contain a namespace):
            Arguments.of("entity-00.xml", Foo.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json")),
            // (1) Attempt to deserialize an entity with a namespace declared in package-info.java (the input DOES NOT contain a namespace):
            Arguments.of("entity-01.xml", Foo.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json")),
            // (2) Attempt to deserialize an entity with a namespace declared in package-info.java (the input contains the wrong namespace):
            Arguments.of("entity-02.xml", Foo.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json")),
            // (3) Attempt to deserialize an entity without a namespace declared in package-info.java (the input DOES contain a namespace):
            Arguments.of("entity-00.xml", Bar.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json")),
            // (4) Attempt to deserialize an entity without a namespace declared in package-info.java (the input DOES NOT contain a namespace):
            Arguments.of("entity-01.xml", Bar.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json")),
            // (5) Attempt to deserialize an entity without a namespace declared in package-info.java (the input contains the wrong namespace):
            Arguments.of("entity-02.xml", Bar.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReadFrom(String entityResource, Class<?> type, Expectation expectation) {
        tester.test(() -> converter.read(type, createHttpInputMessage(entityResource)), expectation);
    }

    private HttpInputMessage createHttpInputMessage(String resource) {
        return new HttpInputMessage() {

            @Override
            public HttpHeaders getHeaders() {
                return HttpHeaders.EMPTY;
            }

            @Override
            public InputStream getBody() {
                return getClass().getResourceAsStream(resource);
            }

        };
    }

}
