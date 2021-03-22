package org.cloudfoundry.multiapps.controller.core.util;

import java.io.Serializable;
import java.security.Principal;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public class UserInfo implements Principal, Serializable {

    private static final long serialVersionUID = 3250778087956531492L;

    private final String id;
    private final String name;
    private final OAuth2AccessTokenWithAdditionalInfo token;

    public UserInfo(String id, String name, OAuth2AccessTokenWithAdditionalInfo token) {
        this.id = id;
        this.name = name;
        this.token = token;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public OAuth2AccessTokenWithAdditionalInfo getToken() {
        return token;
    }

}
