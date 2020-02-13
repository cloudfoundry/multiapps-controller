package com.sap.cloud.lm.sl.cf.core.cf;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.uaa.UAAClient;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Named
public class OAuthClientFactory {

    private final RestUtil restUtil = new RestUtil();
    @Inject
    private TokenService tokenService;
    @Inject
    private UAAClient uaaClient;
    @Inject
    private ApplicationConfiguration configuration;

    public OAuthClient createOAuthClient() {
        RestTemplate restTemplate = createRestTemplate();
        return new OAuthClientExtended(uaaClient.getUaaUrl(), restTemplate, tokenService);
    }

    private RestTemplate createRestTemplate() {
        return restUtil.createRestTemplate(null, configuration.shouldSkipSslValidation());
    }

}
