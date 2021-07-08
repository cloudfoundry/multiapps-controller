package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SSLUtil;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.web.Messages;
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

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

@Named
public class AuthenticationLoaderFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationLoaderFilter.class);

    private final TokenGeneratorFactory tokenGeneratorFactory;

    @Inject
    public AuthenticationLoaderFilter(TokenGeneratorFactory tokenGeneratorFactory, ApplicationConfiguration applicationConfiguration) {
        this.tokenGeneratorFactory = tokenGeneratorFactory;
        if (applicationConfiguration.shouldSkipSslValidation()) {
            SSLUtil.disableSSLValidation();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaderValue == null) {
            failWithUnauthorized(Messages.NO_AUTHORIZATION_HEADER_WAS_PROVIDED);
        }
        OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo = generateOauthToken(authorizationHeaderValue);
        UserInfo tokenUserInfo = SecurityUtil.getTokenUserInfo(oAuth2AccessTokenWithAdditionalInfo);
        loadAuthenticationInContext(tokenUserInfo);
        filterChain.doFilter(request, response);
    }

    private void failWithUnauthorized(String message) {
        SecurityContextHolder.clearContext();
        LOGGER.error(message);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private OAuth2AccessTokenWithAdditionalInfo generateOauthToken(String authorizationHeaderValue) {
        String[] tokenStringElements = authorizationHeaderValue.split("\\s");
        TokenGenerator tokenGenerator = tokenGeneratorFactory.createGenerator(tokenStringElements[0]);
        return tokenGenerator.generate(tokenStringElements[1]);
    }

    private void loadAuthenticationInContext(UserInfo tokenUserInfo) {
        OAuth2AuthenticationToken authentication = SecurityUtil.createAuthentication(tokenUserInfo);
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
    }

}
