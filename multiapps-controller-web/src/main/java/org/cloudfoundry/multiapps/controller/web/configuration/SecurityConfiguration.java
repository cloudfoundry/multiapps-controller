package org.cloudfoundry.multiapps.controller.web.configuration;

import com.sap.cloudfoundry.client.facade.oauth2.TokenFactory;
import org.cloudfoundry.multiapps.controller.web.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import javax.inject.Inject;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@ComponentScan(basePackageClasses = org.cloudfoundry.multiapps.controller.PackageMarker.class)
@EnableWebSecurity
public class SecurityConfiguration {

    private final AuthenticationLoaderFilter authenticationLoaderFilter;
    private final CompositeUriAuthorizationFilter compositeUriAuthorizationFilter;
    private final RequestSizeFilter requestSizeFilter;
    private final CsrfHeadersFilter csrfHeadersFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

    @Inject
    public SecurityConfiguration(AuthenticationLoaderFilter authenticationLoaderFilter,
                                 CompositeUriAuthorizationFilter compositeUriAuthorizationFilter, RequestSizeFilter requestSizeFilter,
                                 CsrfHeadersFilter csrfHeadersFilter, ExceptionHandlerFilter exceptionHandlerFilter) {
        this.authenticationLoaderFilter = authenticationLoaderFilter;
        this.compositeUriAuthorizationFilter = compositeUriAuthorizationFilter;
        this.requestSizeFilter = requestSizeFilter;
        this.csrfHeadersFilter = csrfHeadersFilter;
        this.exceptionHandlerFilter = exceptionHandlerFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.sessionManagement()
                   .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                   .and()
                   .authorizeRequests()
                   .requestMatchers(antMatcher(HttpMethod.GET, "/**"))
                   .hasAnyAuthority(TokenFactory.SCOPE_CC_READ, TokenFactory.SCOPE_CC_ADMIN)
                   .requestMatchers(antMatcher(HttpMethod.POST, "/**"))
                   .hasAnyAuthority(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_CC_ADMIN)
                   .requestMatchers(antMatcher(HttpMethod.PUT, "/**"))
                   .hasAnyAuthority(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_CC_ADMIN)
                   .requestMatchers(antMatcher(HttpMethod.DELETE, "/**"))
                   .hasAnyAuthority(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_CC_ADMIN)
                   .and()
                   .addFilterBefore(authenticationLoaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                   .addFilterBefore(exceptionHandlerFilter, AuthenticationLoaderFilter.class)
                   .addFilterAfter(requestSizeFilter, AuthenticationLoaderFilter.class)
                   .addFilterAfter(csrfHeadersFilter, CsrfFilter.class)
                   .addFilterAfter(compositeUriAuthorizationFilter, SwitchUserFilter.class)
                   .exceptionHandling()
                   .accessDeniedHandler(accessDeniedHandler())
                   .and()
                   .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                         .requestMatchers(antMatcher("/public/**"));
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CsrfAccessDeniedHandler();
    }
}
