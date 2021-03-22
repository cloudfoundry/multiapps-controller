package org.cloudfoundry.multiapps.controller.core.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.util.TokenProperties;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public class SecurityUtil {

    public static final String CLIENT_ID = "cf";
    public static final String CLIENT_SECRET = "";
    public static final String USER_INFO = "user_info";

    private SecurityUtil() {
    }

    public static OAuth2AuthenticationToken createAuthentication(UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = getAuthorities(userInfo.getToken()
                                                                          .getOAuth2AccessToken()
                                                                          .getScopes());
        Map<String, Object> attributes = Map.of(CLIENT_ID, CLIENT_SECRET, USER_INFO, userInfo);
        OAuth2User oAuth2User = new DefaultOAuth2User(authorities, attributes, CLIENT_ID);
        return new OAuth2AuthenticationToken(oAuth2User, authorities, CLIENT_ID);
    }

    private static List<SimpleGrantedAuthority> getAuthorities(Set<String> scopes) {
        return scopes.stream()
                     .map(SimpleGrantedAuthority::new)
                     .collect(Collectors.toList());
    }

    public static UserInfo getTokenUserInfo(OAuth2AccessTokenWithAdditionalInfo token) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        return new UserInfo(tokenProperties.getUserId(), tokenProperties.getUserName(), token);
    }

}
