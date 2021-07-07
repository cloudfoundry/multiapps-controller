package org.cloudfoundry.multiapps.controller.web.configuration;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.web.security.AuthenticationLoaderFilter;
import org.cloudfoundry.multiapps.controller.web.security.CompositeUriAuthorizationFilter;
import org.cloudfoundry.multiapps.controller.web.security.CsrfHeadersFilter;
import org.cloudfoundry.multiapps.controller.web.security.ExceptionHandlerFilter;
import org.cloudfoundry.multiapps.controller.web.security.RequestSizeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import com.sap.cloudfoundry.client.facade.oauth2.TokenFactory;

@ComponentScan(basePackageClasses = org.cloudfoundry.multiapps.controller.PackageMarker.class)
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

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

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.GET, "/**")
            .hasAnyAuthority(TokenFactory.SCOPE_CC_READ, TokenFactory.SCOPE_CC_ADMIN)
            .antMatchers(HttpMethod.POST, "/**")
            .hasAnyAuthority(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_CC_ADMIN)
            .antMatchers(HttpMethod.PUT, "/**")
            .hasAnyAuthority(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_CC_ADMIN)
            .antMatchers(HttpMethod.DELETE, "/**")
            .hasAnyAuthority(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_CC_ADMIN)
            .and()
            .addFilterBefore(authenticationLoaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterBefore(exceptionHandlerFilter, AuthenticationLoaderFilter.class)
            .addFilterAfter(requestSizeFilter, AuthenticationLoaderFilter.class)
            .addFilterAfter(csrfHeadersFilter, CsrfFilter.class)
            .addFilterAfter(compositeUriAuthorizationFilter, SwitchUserFilter.class)
            .exceptionHandling()
            .accessDeniedHandler(accessDeniedHandler());
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
           .antMatchers("/public/**");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CsrfAccessDeniedHandler();
    }
}
