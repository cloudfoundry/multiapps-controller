package com.sap.cloud.lm.sl.cf.web.security;

import static com.sap.cloud.lm.sl.cf.client.util.TokenFactory.CLIENT_ID;
import static com.sap.cloud.lm.sl.cf.client.util.TokenFactory.EXPIRES_AT_KEY;
import static com.sap.cloud.lm.sl.cf.client.util.TokenProperties.USER_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
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

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.util.TokenGenerator;
import com.sap.cloud.lm.sl.cf.web.util.TokenGeneratorFactory;

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

    public static Stream<Arguments> testWithInvalidAuthorizationHeaderBasicAuth() {
        return Stream.of(
// @formatter:off
                Arguments.of("Bearer      "),
                Arguments.of("Basic       ")
// @formatter:on
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
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

    @ParameterizedTest
    @MethodSource
    void testWithInvalidAuthorizationHeaderBasicAuth(String bearerToken) throws ServletException, IOException {
        OAuth2AccessTokenWithAdditionalInfo mockedToken = getMockedOAuth2AccessTokenWithAdditionalInfo();
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION))
               .thenReturn(bearerToken);
        mockTokenParsingStrategyFactory(mockedToken);
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class,
                                                                       () -> authenticationLoaderFilter.doFilterInternal(request, response,
                                                                                                                         filterChain));
        assertEquals(HttpStatus.UNAUTHORIZED, responseStatusException.getStatus());
    }

    private OAuth2AccessTokenWithAdditionalInfo getMockedOAuth2AccessTokenWithAdditionalInfo() {
        OAuth2AccessToken oAuth2AccessToken = Mockito.mock(OAuth2AccessToken.class);
        Set<String> tokenScopes = new HashSet<>();
        tokenScopes.add("some-scope");
        Mockito.when(oAuth2AccessToken.getTokenValue())
               .thenReturn("Bearer token");
        Mockito.when(oAuth2AccessToken.getScopes())
               .thenReturn(tokenScopes);
        Mockito.when(oAuth2AccessToken.getExpiresAt())
               .thenReturn(Instant.now()
                                  .plus(Duration.ofMinutes(3)));
        Map<String, Object> tokenProperties = new HashMap<>();
        tokenProperties.put(USER_NAME_KEY, "test-user");
        tokenProperties.put(EXPIRES_AT_KEY, EXPIRES_AT_IN_MILLIS);
        tokenProperties.put(CLIENT_ID, "cf");
        return new OAuth2AccessTokenWithAdditionalInfo(oAuth2AccessToken, tokenProperties);
    }

    private void mockTokenParsingStrategyFactory(OAuth2AccessTokenWithAdditionalInfo token) {
        TokenGenerator tokenGenerator = Mockito.mock(TokenGenerator.class);
        Mockito.when(tokenGenerator.generate(any()))
               .thenReturn(token);
        Mockito.when(tokenGeneratorFactory.createGenerator(any()))
               .thenReturn(tokenGenerator);
    }

}
