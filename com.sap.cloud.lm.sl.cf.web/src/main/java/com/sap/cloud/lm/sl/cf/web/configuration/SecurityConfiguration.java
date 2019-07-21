package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.sap.cloud.lm.sl.cf.web.security.TokenStoreFactory;

@Configuration
public class SecurityConfiguration {

    private static final String CF_DEPLOY_SERVICE = "CF Deploy Service";

    @Inject
    @Bean
    public DelegatingAuthenticationEntryPoint
           delegatingAuthenticationEntryPoint(OAuth2AuthenticationEntryPoint oauthAuthenticationEntryPoint,
                                              BasicAuthenticationEntryPoint basicAuthenticationEntryPoint) {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(this::containsBearerAuthorizationHeader, oauthAuthenticationEntryPoint);
        DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegatingAuthenticationEntryPoint.setDefaultEntryPoint(basicAuthenticationEntryPoint);
        return delegatingAuthenticationEntryPoint;
    }

    private boolean containsBearerAuthorizationHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        return StringUtils.startsWithIgnoreCase(authorizationHeader, "bearer");
    }

    @Bean
    public OAuth2AuthenticationEntryPoint oauthAuthenticationEntryPoint() {
        OAuth2AuthenticationEntryPoint oauthAuthenticationEntryPoint = new OAuth2AuthenticationEntryPoint();
        oauthAuthenticationEntryPoint.setRealmName(CF_DEPLOY_SERVICE);
        return oauthAuthenticationEntryPoint;
    }

    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName(CF_DEPLOY_SERVICE);
        return basicAuthenticationEntryPoint;
    }

    @Inject
    @Bean
    public UnanimousBased accessDecisionManager(WebExpressionVoter webExpressionVoter, AuthenticatedVoter authenticatedVoter) {
        return new UnanimousBased(Arrays.asList(webExpressionVoter, authenticatedVoter));
    }

    @Bean
    public AuthenticatedVoter authenticatedVoter() {
        return new AuthenticatedVoter();
    }

    @Inject
    @Bean
    public JdbcTokenStore tokenStore(DataSource dataSource) {
        return TokenStoreFactory.getTokenStore(dataSource);
    }

}
