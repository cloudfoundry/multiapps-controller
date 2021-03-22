package org.cloudfoundry.multiapps.controller.client;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenProvider {

    OAuth2AccessTokenWithAdditionalInfo getToken();

}
