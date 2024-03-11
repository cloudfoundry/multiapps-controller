package com.sap.cloud.lm.sl.cf.core.util;

import static org.cloudfoundry.client.constants.Constants.EXCHANGED_TOKEN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.core.Constants;

public final class SecurityUtil {

    public static final String CF_CLIENT_ID = "cf";
    public static final String CLIENT_SECRET = "";
    public static final String USER_INFO = "user_info";

    private SecurityUtil() {
    }

    public static OAuth2AuthenticationToken createAuthentication(UserInfo userInfo) {
        List<SimpleGrantedAuthority> authorities = getAuthorities(userInfo.getToken()
                                                                          .getScopes());
        String clientId = MapUtils.getString(userInfo.getToken()
                                                     .getAdditionalInfo(),
                                             Constants.CLIENT_ID);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(clientId, CLIENT_SECRET);
        attributes.put(USER_INFO, userInfo);
        OAuth2User oAuth2User = new DefaultOAuth2User(authorities, attributes, clientId);
        return new OAuth2AuthenticationToken(oAuth2User, authorities, clientId);
    }

    private static List<SimpleGrantedAuthority> getAuthorities(Set<String> scopes) {
        return scopes.stream()
                     .map(SimpleGrantedAuthority::new)
                     .collect(Collectors.toList());
    }

    public static UserInfo getTokenUserInfo(OAuth2AccessTokenWithAdditionalInfo token, TokenFactory tokenFactory) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        Optional<OAuth2AccessTokenWithAdditionalInfo> exchangedToken = getExchangedToken(token, tokenFactory);
        if (exchangedToken.isPresent()) {
            return new UserInfo(tokenProperties.getUserId(), tokenProperties.getUserName(), exchangedToken.get());
        }
        return new UserInfo(tokenProperties.getUserId(), tokenProperties.getUserName(), token);
    }

    private static Optional<OAuth2AccessTokenWithAdditionalInfo> getExchangedToken(OAuth2AccessTokenWithAdditionalInfo token,
                                                                                   TokenFactory tokenFactory) {
        String exchangedTokenValue = MapUtils.getString(token.getAdditionalInfo(), EXCHANGED_TOKEN);
        if (exchangedTokenValue == null) {
            return Optional.empty();
        }
        return Optional.of(tokenFactory.createToken(exchangedTokenValue, token.getAdditionalInfo()));
    }

}
