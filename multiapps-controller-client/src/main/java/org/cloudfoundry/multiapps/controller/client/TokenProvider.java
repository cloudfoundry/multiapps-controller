package org.cloudfoundry.multiapps.controller.client;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenProvider {

    OAuth2AccessTokenWithAdditionalInfo getToken();

}
