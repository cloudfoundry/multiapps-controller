package com.sap.cloud.lm.sl.cf.core.cf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import com.sap.cloud.lm.sl.cf.core.util.Configuration;

import static org.mockito.Mockito.when;

public class TaggingRequestInterceptorTest {

    private static final String TEST_ORG_VALUE = "faceorg";
    private static final String TEST_SPACE_VALUE = "myspace";
    private org.springframework.http.HttpRequest requestStub;
    private byte[] body = new byte[] {};
    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private Configuration configuration;

    @Before
    public void setUp() {
        requestStub = new AbstractClientHttpRequest() {
            public URI getURI() {
                return null;
            }

            public HttpMethod getMethod() {
                return null;
            }

            protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
                return null;
            }

            protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
                return null;
            }
        };
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInjectGenericValue() throws IOException {
        TaggingRequestInterceptor testedInterceptor = new TaggingRequestInterceptor();
        testedInterceptor.intercept(requestStub, body, execution);
        assertNotNull(requestStub.getHeaders());
        assertTrue(requestStub.getHeaders()
            .containsKey(TaggingRequestInterceptor.TAG_HEADER_NAME));
        String expectedValue = testedInterceptor.getHeaderValue();
        Optional<String> foundValue = requestStub.getHeaders()
            .get(TaggingRequestInterceptor.TAG_HEADER_NAME)
            .stream()
            .filter(value -> value.equals(expectedValue))
            .findFirst();
        assertTrue(foundValue.isPresent());
    }

    @Test
    public void testInjectOrgAndSpaceValues() throws IOException {
        TaggingRequestInterceptor testedInterceptor = new TaggingRequestInterceptor(TEST_ORG_VALUE, TEST_SPACE_VALUE);
        testedInterceptor.intercept(requestStub, body, execution);
        HttpHeaders headers = requestStub.getHeaders();
        assertNotNull(headers);
        assertTrue(headers.containsKey(TaggingRequestInterceptor.TAG_HEADER_ORG_NAME));
        assertTrue(headers.containsKey(TaggingRequestInterceptor.TAG_HEADER_SPACE_NAME));
        String expectedValue = testedInterceptor.getHeaderValue();
        Optional<String> foundValue = headers.get(TaggingRequestInterceptor.TAG_HEADER_NAME)
            .stream()
            .filter(value -> value.equals(expectedValue))
            .findFirst();
        assertTrue(foundValue.isPresent());
        Optional<String> foundOrgValue = headers.get(TaggingRequestInterceptor.TAG_HEADER_ORG_NAME)
            .stream()
            .filter(value -> value.equals(TEST_ORG_VALUE))
            .findFirst();
        assertTrue(foundOrgValue.isPresent());
        Optional<String> foundSpaceValue = headers.get(TaggingRequestInterceptor.TAG_HEADER_SPACE_NAME)
            .stream()
            .filter(value -> value.equals(TEST_SPACE_VALUE))
            .findFirst();
        assertTrue(foundSpaceValue.isPresent());

    }

    @Test
    public void testGetHeaderValue() throws IOException {
        TaggingRequestInterceptor testedInterceptor = new TaggingRequestInterceptor(null, null) {
            @Override
            protected Configuration getConfiguration() {
                return configuration;
            }
        };
        final String dsversion = "9.9.9-SNAPSHOT";
        when(configuration.getVersion()).thenReturn(dsversion);
        String headerValue = testedInterceptor.getHeaderValue();
        assertTrue(headerValue.contains(dsversion));
        assertTrue(headerValue.contains("deploy-service"));
    }

    @Test
    public void addHeadersOnlyOnceTest() throws IOException {
        TaggingRequestInterceptor testedInterceptor = new TaggingRequestInterceptor(TEST_ORG_VALUE, TEST_SPACE_VALUE);
        testedInterceptor.intercept(requestStub, body, execution);
        testedInterceptor.intercept(requestStub, body, execution);
        testedInterceptor.intercept(requestStub, body, execution);
        HttpHeaders headers = requestStub.getHeaders();
        String expectedValue = testedInterceptor.getHeaderValue();
        long tagCount = headers.get(TaggingRequestInterceptor.TAG_HEADER_NAME)
            .stream()
            .filter(value -> value.equals(expectedValue))
            .count();
        assertEquals("Main tag header occurence is not 1", 1l, tagCount);
        long orgTagCount = headers.get(TaggingRequestInterceptor.TAG_HEADER_ORG_NAME)
            .stream()
            .filter(value -> value.equals(TEST_ORG_VALUE))
            .count();
        assertEquals("Org tag header occurence is not 1", 1l, orgTagCount);
        long spaceTagCount = headers.get(TaggingRequestInterceptor.TAG_HEADER_SPACE_NAME)
            .stream()
            .filter(value -> value.equals(TEST_SPACE_VALUE))
            .count();
        assertEquals("Space tag header occurence is not 1", 1l, spaceTagCount);
    }
}
