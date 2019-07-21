package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

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

import com.sap.cloud.lm.sl.cf.web.resources.CorrelationIdFilter;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationFilter;
import com.sap.cloud.lm.sl.cf.web.security.CsrfAccessDeniedHandler;
import com.sap.cloud.lm.sl.cf.web.security.CsrfHeadersFilter;
import com.sap.cloud.lm.sl.cf.web.security.CustomAuthenticationProvider;
import com.sap.cloud.lm.sl.cf.web.security.RequestSizeFilter;
import com.sap.cloud.lm.sl.cf.web.security.TokenStoreFactory;

@Configuration
public class SecurityConfiguration {

    @Inject
    @Bean
    public DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint(
        OAuth2AuthenticationEntryPoint oauthAuthenticationEntryPoint, BasicAuthenticationEntryPoint basicAuthenticationEntryPoint) {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(this::containsBearerAuthorizationHeader, oauthAuthenticationEntryPoint);
        DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegatingAuthenticationEntryPoint.setDefaultEntryPoint(basicAuthenticationEntryPoint);
        return delegatingAuthenticationEntryPoint;
    }

    private boolean containsBearerAuthorizationHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        return authorizationHeader != null && authorizationHeader.startsWith("bearer");
    }

    @Bean
    public OAuth2AuthenticationEntryPoint oauthAuthenticationEntryPoint() {
        OAuth2AuthenticationEntryPoint oauthAuthenticationEntryPoint = new OAuth2AuthenticationEntryPoint();
        oauthAuthenticationEntryPoint.setRealmName("CF Deploy Service");
        return oauthAuthenticationEntryPoint;
    }

    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("CF Deploy Service");
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

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider();
    }

    @Bean
    public CsrfAccessDeniedHandler accessDeniedHandler() {
        return new CsrfAccessDeniedHandler();
    }

    @Bean
    public CsrfHeadersFilter csrfHeadersFilter() {
        return new CsrfHeadersFilter();
    }

    @Bean
    public RequestSizeFilter requestSizeFilter() {
        return new RequestSizeFilter();
    }

    @Bean
    public AuthorizationFilter authFilter() {
        return new AuthorizationFilter();
    }

    @Inject
    @Bean
    public JdbcTokenStore tokenStore(DataSource dataSource) {
        return TokenStoreFactory.getTokenStore(dataSource);
    }

}
