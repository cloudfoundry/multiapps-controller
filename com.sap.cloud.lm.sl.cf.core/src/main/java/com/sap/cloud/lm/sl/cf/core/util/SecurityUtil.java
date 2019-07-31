package com.sap.cloud.lm.sl.cf.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.core.Constants;

public class SecurityUtil {

    public static final String CLIENT_ID = "cf";
    public static final String CLIENT_SECRET = "";

    private SecurityUtil() {
    }

    public static OAuth2Authentication createAuthentication(String clientId, Set<String> scope, UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = getAuthorities(scope);
        OAuth2Request request = new OAuth2Request(new HashMap<String, String>(),
                                                  clientId,
                                                  authorities,
                                                  true,
                                                  scope,
                                                  null,
                                                  null,
                                                  null,
                                                  null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userInfo, "", authorities);
        return new OAuth2Authentication(request, auth);
    }

    private static List<SimpleGrantedAuthority> getAuthorities(Set<String> scopes) {
        return scopes.stream()
                     .map(SimpleGrantedAuthority::new)
                     .collect(Collectors.toList());
    }

    public static UserInfo getTokenUserInfo(OAuth2AccessToken token) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        OAuth2AccessToken exchangedToken = getExchangedToken(token);
        if (exchangedToken != null) {
            return new UserInfo(tokenProperties.getUserId(), tokenProperties.getUserName(), exchangedToken);
        }
        return new UserInfo(tokenProperties.getUserId(), tokenProperties.getUserName(), token);
    }

    private static OAuth2AccessToken getExchangedToken(OAuth2AccessToken token) {
        String exchangedTokenValue = MapUtils.getString(token.getAdditionalInformation(), Constants.EXCHANGED_TOKEN);
        if (exchangedTokenValue == null) {
            return null;
        }
        DefaultOAuth2AccessToken exchangedToken = new DefaultOAuth2AccessToken(token);
        exchangedToken.setValue(exchangedTokenValue);
        return exchangedToken;
    }

}
