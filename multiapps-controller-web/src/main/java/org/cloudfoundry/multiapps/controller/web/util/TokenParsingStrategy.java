package org.cloudfoundry.multiapps.controller.web.util;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenParsingStrategy {

    OAuth2AccessTokenWithAdditionalInfo parseToken(String tokenString);
}
