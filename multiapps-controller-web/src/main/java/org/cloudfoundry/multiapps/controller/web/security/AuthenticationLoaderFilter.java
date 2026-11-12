package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.ClientIpResolver;
import org.cloudfoundry.multiapps.controller.web.util.FailedAuthRateLimiter;
import org.cloudfoundry.multiapps.controller.web.util.TokenGenerator;
import org.cloudfoundry.multiapps.controller.web.util.TokenGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Named
public class AuthenticationLoaderFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationLoaderFilter.class);
    private static final String BASIC_SCHEME = "basic";

    private final TokenGeneratorFactory tokenGeneratorFactory;
    private final FailedAuthRateLimiter failedAuthRateLimiter;
    private final ClientIpResolver clientIpResolver;

    @Inject
    public AuthenticationLoaderFilter(TokenGeneratorFactory tokenGeneratorFactory, ApplicationConfiguration applicationConfiguration,
                                      FailedAuthRateLimiter failedAuthRateLimiter, ClientIpResolver clientIpResolver) {
        this.tokenGeneratorFactory = tokenGeneratorFactory;
        this.failedAuthRateLimiter = failedAuthRateLimiter;
        this.clientIpResolver = clientIpResolver;
        if (applicationConfiguration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String ip = clientIpResolver.resolve(request);
        String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaderValue == null) {
            failWithUnauthorized(ip, null, Messages.NO_AUTHORIZATION_HEADER_WAS_PROVIDED);
        }
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = generateOauthToken(ip, authorizationHeaderValue);
        UserInfo tokenUserInfo = SecurityUtil.getTokenUserInfo(oAuth2AccessTokenWithAdditionalInfo);
        loadAuthenticationInContext(tokenUserInfo);
        filterChain.doFilter(request, response);
    }

    private void failWithUnauthorized(String ip, String username, String message) {
        SecurityContextHolder.clearContext();
        LOGGER.error(message);
        failedAuthRateLimiter.recordFailedAttempt(ip, username);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private OAuth2AccessTokenWithAdditionalInfo generateOauthToken(String ip, String authorizationHeaderValue) {
        String[] tokenStringElements = authorizationHeaderValue.split("\\s");
        if (tokenStringElements.length != 2) {
            failWithUnauthorized(ip, null, Messages.INVALID_AUTHORIZATION_HEADER_WAS_PROVIDED);
        }
        String scheme = tokenStringElements[0];
        String token = tokenStringElements[1];
        String username = extractUsername(scheme, token);
        failedAuthRateLimiter.checkAllowed(ip, username);
        try {
            TokenGenerator tokenGenerator = tokenGeneratorFactory.createGenerator(scheme);
            return tokenGenerator.generate(token);
        } catch (RuntimeException e) {
            failedAuthRateLimiter.recordFailedAttempt(ip, username);
            throw e;
        }
    }

    private String extractUsername(String scheme, String token) {
        if (!BASIC_SCHEME.equalsIgnoreCase(scheme)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder()
                                              .decode(token), StandardCharsets.UTF_8);
            int colonIndex = decoded.indexOf(':');
            return colonIndex == -1 ? null : decoded.substring(0, colonIndex);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void loadAuthenticationInContext(UserInfo tokenUserInfo) {
        OAuth2AuthenticationToken authentication = SecurityUtil.createAuthentication(tokenUserInfo);
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
    }

}
