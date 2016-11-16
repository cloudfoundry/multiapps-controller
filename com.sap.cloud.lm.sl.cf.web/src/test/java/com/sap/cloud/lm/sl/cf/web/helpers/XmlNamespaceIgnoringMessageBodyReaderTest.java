package com.sap.cloud.lm.sl.cf.web.helpers;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.SAXParserFactory;

import org.glassfish.jersey.jaxb.internal.SaxParserFactoryInjectionProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.web.helpers.model1.Model1;
import com.sap.cloud.lm.sl.cf.web.helpers.model2.Model2;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class XmlNamespaceIgnoringMessageBodyReaderTest<T> {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Attempt to deserialize an entity with a namespace declared in package-info.java (the input DOES contain a namespace):
            {
                "entity-00.xml", Model1.class, "R:expected-entity-00.json", 
            },
            // (1) Attempt to deserialize an entity with a namespace declared in package-info.java (the input DOES NOT contain a namespace):
            {
                "entity-01.xml", Model1.class, "R:expected-entity-00.json", 
            },
            // (2) Attempt to deserialize an entity with a namespace declared in package-info.java (the input contains the wrong namespace):
            {
                "entity-02.xml", Model1.class, "R:expected-entity-00.json", 
            },
            // (3) Attempt to deserialize an entity without a namespace declared in package-info.java (the input DOES contain a namespace):
            {
                "entity-00.xml", Model2.class, "R:expected-entity-00.json", 
            },
            // (4) Attempt to deserialize an entity without a namespace declared in package-info.java (the input DOES NOT contain a namespace):
            {
                "entity-01.xml", Model2.class, "R:expected-entity-00.json", 
            },
            // (5) Attempt to deserialize an entity without a namespace declared in package-info.java (the input contains the wrong namespace):
            {
                "entity-02.xml", Model2.class, "R:expected-entity-00.json", 
            },
// @formatter:on
        });
    }

    private final String entityLocation;
    private final Class<T> classOfT;
    private final String expectedResult;

    private XmlNamespaceIgnoringMessageBodyReader<T> reader = new XmlNamespaceIgnoringMessageBodyReader<>(getSaxParserFactory());

    private static SAXParserFactory getSaxParserFactory() {
        return new SaxParserFactoryInjectionProvider(Mockito.mock(Configuration.class)).provide();
    }

    public XmlNamespaceIgnoringMessageBodyReaderTest(String entityLocation, Class<T> classOfT, String expectedResult) {
        this.entityLocation = entityLocation;
        this.classOfT = classOfT;
        this.expectedResult = expectedResult;
    }

    @Test
    public void testIsReadable() {
        assertTrue(reader.isReadable(classOfT, null, null, MediaType.APPLICATION_XML_TYPE));
    }

    @Test
    public void testReadFrom() {
        TestUtil.test(() -> {
            return reader.readFrom(classOfT, null, null, null, null, (getClass().getResourceAsStream(entityLocation)));
        } , expectedResult, getClass());
    }

}
