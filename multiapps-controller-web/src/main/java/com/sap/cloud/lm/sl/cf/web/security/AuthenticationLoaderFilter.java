package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SSLUtil;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.TokenGenerator;
import com.sap.cloud.lm.sl.cf.web.util.TokenGeneratorFactory;
import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
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

    private final TokenGeneratorFactory tokenGeneratorFactory;
    private final TokenFactory tokenFactory;

    @Inject
    public AuthenticationLoaderFilter(TokenGeneratorFactory tokenGeneratorFactory, ApplicationConfiguration applicationConfiguration, TokenFactory tokenFactory) {
        this.tokenGeneratorFactory = tokenGeneratorFactory;
        this.tokenFactory = tokenFactory;
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
        UserInfo tokenUserInfo = SecurityUtil.getTokenUserInfo(oAuth2AccessTokenWithAdditionalInfo, tokenFactory);
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
        if (tokenStringElements.length != 2) {
            failWithUnauthorized(Messages.INVALID_AUTHORIZATION_HEADER_WAS_PROVIDED);
        }
        TokenGenerator tokenGenerator = tokenGeneratorFactory.createGenerator(tokenStringElements[0]);
        return tokenGenerator.generate(tokenStringElements[1]);
    }

    private void loadAuthenticationInContext(UserInfo tokenUserInfo) {
        OAuth2AuthenticationToken authentication = SecurityUtil.createAuthentication(tokenUserInfo);
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
    }

}
