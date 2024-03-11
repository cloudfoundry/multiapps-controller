package com.sap.cloud.lm.sl.cf.client;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenProvider {

    OAuth2AccessTokenWithAdditionalInfo getToken();
}
