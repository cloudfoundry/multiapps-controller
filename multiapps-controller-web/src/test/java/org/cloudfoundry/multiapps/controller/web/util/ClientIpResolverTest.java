package org.cloudfoundry.multiapps.controller.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    private static final String CLIENT_IP = "203.0.113.42";
    private static final String REMOTE_ADDR = "10.0.0.1";

    @Mock
    private HttpServletRequest request;

    private final ClientIpResolver clientIpResolver = new ClientIpResolver();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void usesTrueClientIpHeaderWhenPresent() {
        Mockito.when(request.getHeader(Constants.X_CF_TRUE_CLIENT_IP_HEADER))
               .thenReturn(CLIENT_IP);

        assertEquals(CLIENT_IP, clientIpResolver.resolve(request));

        Mockito.verify(request, Mockito.never())
               .getRemoteAddr();
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderAbsent() {
        Mockito.when(request.getHeader(Constants.X_CF_TRUE_CLIENT_IP_HEADER))
               .thenReturn(null);
        Mockito.when(request.getRemoteAddr())
               .thenReturn(REMOTE_ADDR);

        assertEquals(REMOTE_ADDR, clientIpResolver.resolve(request));
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderBlank() {
        Mockito.when(request.getHeader(Constants.X_CF_TRUE_CLIENT_IP_HEADER))
               .thenReturn("   ");
        Mockito.when(request.getRemoteAddr())
               .thenReturn(REMOTE_ADDR);

        assertEquals(REMOTE_ADDR, clientIpResolver.resolve(request));
    }
}
