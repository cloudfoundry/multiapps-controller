package org.cloudfoundry.multiapps.controller.core.cf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import reactor.core.publisher.Mono;

class TaggingRequestFilterFunctionTest {

    private static final String TEST_VERSION_VALUE = "1.58.0";
    private static final String TEST_ORG_VALUE = "faceorg";
    private static final String TEST_SPACE_VALUE = "myspace";
    private HttpHeaders actualHeaders;
    private ClientRequest clientRequest;
    @Mock
    private ExchangeFunction nextFilter;

    @BeforeEach
    void setUp() throws Exception {
        actualHeaders = new HttpHeaders();
        clientRequest = initializeClientRequest();
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    private ClientRequest initializeClientRequest() {
        return new ClientRequest() {

            @Override
            public Map<String, Object> attributes() {
                return null;
            }

            @Override
            public BodyInserter<?, ? super ClientHttpRequest> body() {
                return null;
            }

            @Override
            public MultiValueMap<String, String> cookies() {
                return null;
            }

            @Override
            public HttpHeaders headers() {
                return actualHeaders;
            }

            @Override
            public String logPrefix() {
                return null;
            }

            @Override
            public HttpMethod method() {
                return null;
            }

            @Override
            public URI url() {
                return null;
            }

            @Override
            public Mono<Void> writeTo(ClientHttpRequest arg0, ExchangeStrategies arg1) {
                return null;
            }
        };
    }

    @Test
    void testInjectOnlyDeployServiceVersion() throws IOException {
        TaggingRequestFilterFunction testedFilterFunction = new TaggingRequestFilterFunction(TEST_VERSION_VALUE);

        testedFilterFunction.filter(clientRequest, nextFilter);

        assertEquals("MTA deploy-service v1.58.0", actualHeaders.getFirst(TaggingRequestFilterFunction.TAG_HEADER_NAME));
        assertFalse(actualHeaders.containsKey(TaggingRequestFilterFunction.TAG_HEADER_ORG_NAME));
        assertFalse(actualHeaders.containsKey(TaggingRequestFilterFunction.TAG_HEADER_SPACE_NAME));
    }

    @Test
    void testInjectOrgAndSpaceValues() throws IOException {
        TaggingRequestFilterFunction testedFilterFunction = new TaggingRequestFilterFunction(TEST_VERSION_VALUE,
                                                                                             TEST_ORG_VALUE,
                                                                                             TEST_SPACE_VALUE);

        testedFilterFunction.filter(clientRequest, nextFilter);

        assertEquals("MTA deploy-service v1.58.0", actualHeaders.getFirst(TaggingRequestFilterFunction.TAG_HEADER_NAME));
        assertEquals(TEST_ORG_VALUE, actualHeaders.getFirst(TaggingRequestFilterFunction.TAG_HEADER_ORG_NAME));
        assertEquals(TEST_SPACE_VALUE, actualHeaders.getFirst(TaggingRequestFilterFunction.TAG_HEADER_SPACE_NAME));
    }

}
