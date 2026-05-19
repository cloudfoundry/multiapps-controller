package org.cloudfoundry.multiapps.controller.web.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.HandlerMapping;

class ServletUtilTest {

    @Mock
    private ServletRequest servletRequest;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testGetPathVariablesReturnsMapFromRequestAttribute() {
        Map<String, String> pathVariables = Map.of("id", "42");
        Mockito.when(servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
               .thenReturn(pathVariables);

        Map<String, String> result = ServletUtil.getPathVariables(servletRequest);

        Assertions.assertEquals(pathVariables, result);
    }

    @Test
    void testGetPathVariableLooksUpByName() {
        Mockito.when(servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
               .thenReturn(Map.of("operationId", "op-123"));

        String result = ServletUtil.getPathVariable(servletRequest, "operationId");

        Assertions.assertEquals("op-123", result);
    }

    @Test
    void testDecodeUriReadsAndDecodesRequestURI() {
        Mockito.when(httpServletRequest.getRequestURI())
               .thenReturn("/path/with%20space");

        String result = ServletUtil.decodeUri(httpServletRequest);

        Assertions.assertEquals("/path/with space", result);
    }

    @Test
    void testDecodeHandlesPercentEncodedString() {
        Assertions.assertEquals("hello world", ServletUtil.decode("hello%20world"));
    }

    @Test
    void testRemoveInvalidForwardSlashesCollapsesRepeatedSlashes() {
        Assertions.assertEquals("/a/b/c", ServletUtil.removeInvalidForwardSlashes("/a//b///c"));
    }

    @Test
    void testRemoveInvalidForwardSlashesLeavesSingleSlashesUnchanged() {
        Assertions.assertEquals("/a/b", ServletUtil.removeInvalidForwardSlashes("/a/b"));
    }

    @Test
    void testSendWritesBodyAndStatus() throws IOException {
        StringWriter buffer = new StringWriter();
        Mockito.when(httpServletResponse.getWriter())
               .thenReturn(new PrintWriter(buffer));

        ServletUtil.send(httpServletResponse, 418, "I'm a teapot");

        Mockito.verify(httpServletResponse)
               .setStatus(418);
        Mockito.verify(httpServletResponse)
               .setCharacterEncoding("UTF-8");
        Assertions.assertEquals("I'm a teapot", buffer.toString());
    }
}
