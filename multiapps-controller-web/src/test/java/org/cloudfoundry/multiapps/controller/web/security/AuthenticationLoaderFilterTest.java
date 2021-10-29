package org.cloudfoundry.multiapps.controller.web.security;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.EXPIRES_AT_KEY;
import static org.cloudfoundry.multiapps.controller.client.util.TokenProperties.USER_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.web.util.TokenGenerator;
import org.cloudfoundry.multiapps.controller.web.util.TokenGeneratorFactory;
import org.cloudfoundry.multiapps.mta.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class AuthenticationLoaderFilterTest {

    private static final long EXPIRES_AT_IN_MILLIS = 1617114290L;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private TokenGeneratorFactory tokenGeneratorFactory;
    @InjectMocks
    private AuthenticationLoaderFilter authenticationLoaderFilter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testNonAuthorizedRequest() {
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class,
                                                                       () -> authenticationLoaderFilter.doFilterInternal(request, response,
                                                                                                                         filterChain));
        assertEquals(HttpStatus.UNAUTHORIZED, responseStatusException.getStatus());
    }

    @Test
    void testAuthorizedCall() throws ServletException, IOException {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = getMockedOAuth2AccessTokenWithAdditionalInfo();
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn("Bearer token");
        mockTokenParsingStrategyFactory(mockedToken);
        authenticationLoaderFilter.doFilterInternal(request, response, filterChain);
        Mockito.verify(filterChain)
               .doFilter(request, response);
    }

    public static Stream<Arguments> testWithInvalidAuthorizationHeaderBasicAuth() {
        return Stream.of(
// @formatter:off
                Arguments.of("Bearer      "),
                Arguments.of("Basic         ")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWithInvalidAuthorizationHeaderBasicAuth(String bearerToken) throws ServletException, IOException {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = getMockedOAuth2AccessTokenWithAdditionalInfo();
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
                .thenReturn(bearerToken);
        mockTokenParsingStrategyFactory(mockedToken);
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> authenticationLoaderFilter.doFilterInternal(request, response, filterChain));
        assertEquals(HttpStatus.UNAUTHORIZED, responseStatusException.getStatus());
    }

    private OAuth2AccessTokenWithAdditionalInfo getMockedOAuth2AccessTokenWithAdditionalInfo() {
        OAuth2AccessTokenWithAdditionalInfo token = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn("Bearer token");
        Mockito.when(oAuth2AccessToken.getScopes())
               .thenReturn(Set.of("some-scope"));
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(Duration.ofMinutes(3)));
        Mockito.when(token.getOAuth2AccessToken())
               .thenReturn(oAuth2AccessToken);
        Map<String, Object> tokenProperties = Map.of(USER_NAME_KEY, "test-user", EXPIRES_AT_KEY, EXPIRES_AT_IN_MILLIS);
        Mockito.when(token.getAdditionalInfo())
               .thenReturn(tokenProperties);
        return token;
    }

    private void mockTokenParsingStrategyFactory(OAuth2AccessTokenWithAdditionalInfo token) {

        TokenGenerator tokenGenerator = Mockito.mock(TokenGenerator.class);
        Mockito.when(tokenGenerator.generate(any()))
               .thenReturn(token);
        Mockito.when(tokenGeneratorFactory.createGenerator(any()))
               .thenReturn(tokenGenerator);
    }

}
