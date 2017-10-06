package com.sap.cloud.lm.sl.cf.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import com.sap.cloud.lm.sl.cf.client.util.TokenUtil;

public class SecurityUtil {

    public static final String CLIENT_ID = "cf";
    public static final String CLIENT_SECRET = "";

    public static OAuth2Authentication createAuthentication(String clientId, Set<String> scope, UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = getAuthorities(scope);
        OAuth2Request request = new OAuth2Request(new HashMap<String, String>(), clientId, authorities, true, scope, null, null, null,
            null);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userInfo, "", authorities);
        return new OAuth2Authentication(request, auth);
    }

    private static List<SimpleGrantedAuthority> getAuthorities(Set<String> scopes) {
        return scopes.stream().map(scope -> new SimpleGrantedAuthority(scope)).collect(Collectors.toList());
    }

    public static UserInfo getTokenUserInfo(OAuth2AccessToken token) {
        return new UserInfo(TokenUtil.getTokenUserId(token), TokenUtil.getTokenUserName(token), token);
    }

}
