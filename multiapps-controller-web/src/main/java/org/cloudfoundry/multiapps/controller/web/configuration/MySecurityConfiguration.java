package org.cloudfoundry.multiapps.controller.web.configuration;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.web.security.CompositeUriAuthorizationFilter;
import org.cloudfoundry.multiapps.controller.web.security.CsrfHeadersFilter;
import org.cloudfoundry.multiapps.controller.web.security.RequestSizeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.csrf.CsrfFilter;

@EnableWebSecurity
@Configuration
//@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
//@DependsOn("dataSource")
public class MySecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    CompositeUriAuthorizationFilter compositeUriAuthorizationFilter;
    @Autowired
    RequestSizeFilter requestSizeFilter;
    @Autowired
    CsrfHeadersFilter csrfHeadersFilter;
    @Inject
    DataSource dataSource;

    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .and()
            .authorizeRequests()
            // .antMatchers("/public/**").permitAll()
            // .antMatchers("/helloReader")
            // .hasAuthority("Reader")
            // .antMatchers("/helloWriter")
            // .hasAuthority("Writer")
            // .antMatchers("/helloAdministrator")
            // .hasAuthority("Administrator")
            // .antMatchers("/callback/v1.0/**")
            // .hasAuthority("Callback")
            .antMatchers(HttpMethod.GET, "/**")
            .hasAnyAuthority("SCOPE_cloud_controller.read", "SCOPE_cloud_controller.admin")
            .antMatchers(HttpMethod.PUT, "/**")
            .hasAnyAuthority("SCOPE_cloud_controller.write", "SCOPE_cloud_controller.admin")
            .antMatchers(HttpMethod.POST, "/**")
            .hasAnyAuthority("SCOPE_cloud_controller.write", "SCOPE_cloud_controller.admin")
            .antMatchers(HttpMethod.DELETE, "/**")
            .hasAnyAuthority("SCOPE_cloud_controller.write", "SCOPE_cloud_controller.admin")
            // .denyAll()
            .and()
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwkSetUri("https://uaa.cf.sap.hana.ondemand.com")))
            .addFilterAfter(csrfHeadersFilter, CsrfFilter.class)
            .addFilterAfter(csrfHeadersFilter, AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterAfter(compositeUriAuthorizationFilter, SwitchUserFilter.class);

        // http
        // .authorizeRequests(authorize -> authorize
        // .anyRequest().authenticated()
        // )
        // .oauth2ResourceServer(oauth2 -> oauth2
        // .jwt(jwt -> jwt
        // .jwkSetUri("https://idp.example.com/.well-known/jwks.json")
        // )
        // );
    }

    @Override
    public void configure(final WebSecurity web) {
        web.ignoring()
           .antMatchers("/public/**");
    }
    
    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource);
    }
}