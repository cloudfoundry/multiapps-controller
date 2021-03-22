package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenParser {

    OAuth2AccessTokenWithAdditionalInfo parse(String tokenString);

}
