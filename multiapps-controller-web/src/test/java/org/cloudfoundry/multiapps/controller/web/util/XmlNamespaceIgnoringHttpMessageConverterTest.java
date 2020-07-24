package org.cloudfoundry.multiapps.controller.web.util;

import java.io.InputStream;
import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.web.util.XmlNamespaceIgnoringHttpMessageConverter;
import org.cloudfoundry.multiapps.controller.web.util.bar.Bar;
import org.cloudfoundry.multiapps.controller.web.util.foo.Foo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

@RunWith(Parameterized.class)
public class XmlNamespaceIgnoringHttpMessageConverterTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Attempt to deserialize an entity with a namespace declared in package-info.java (the input DOES contain a namespace):
            {
                "entity-00.xml", Foo.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"), 
            },
            // (1) Attempt to deserialize an entity with a namespace declared in package-info.java (the input DOES NOT contain a namespace):
            {
                "entity-01.xml", Foo.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"), 
            },
            // (2) Attempt to deserialize an entity with a namespace declared in package-info.java (the input contains the wrong namespace):
            {
                "entity-02.xml", Foo.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"), 
            },
            // (3) Attempt to deserialize an entity without a namespace declared in package-info.java (the input DOES contain a namespace):
            {
                "entity-00.xml", Bar.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"), 
            },
            // (4) Attempt to deserialize an entity without a namespace declared in package-info.java (the input DOES NOT contain a namespace):
            {
                "entity-01.xml", Bar.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"), 
            },
            // (5) Attempt to deserialize an entity without a namespace declared in package-info.java (the input contains the wrong namespace):
            {
                "entity-02.xml", Bar.class, new Expectation(Expectation.Type.JSON, "expected-entity-00.json"), 
            },
// @formatter:on
        });
    }

    private final Tester tester = Tester.forClass(getClass());

    private final String entityResource;
    private final Class<?> type;
    private final Expectation expectation;

    private final XmlNamespaceIgnoringHttpMessageConverter converter = new XmlNamespaceIgnoringHttpMessageConverter();

    public XmlNamespaceIgnoringHttpMessageConverterTest(String entityResource, Class<?> type, Expectation expectation) {
        this.entityResource = entityResource;
        this.type = type;
        this.expectation = expectation;
    }

    @Test
    public void testReadFrom() {
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
