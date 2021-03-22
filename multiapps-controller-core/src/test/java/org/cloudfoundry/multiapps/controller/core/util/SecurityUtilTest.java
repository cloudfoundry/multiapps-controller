package org.cloudfoundry.multiapps.controller.core.util;

import static com.sap.cloudfoundry.client.facade.oauth2.TokenFactory.SCOPE_CC_ADMIN;
import static org.cloudfoundry.multiapps.controller.client.util.TokenProperties.CLIENT_ID_KEY;
import static org.cloudfoundry.multiapps.controller.client.util.TokenProperties.USER_ID_KEY;
import static org.cloudfoundry.multiapps.controller.client.util.TokenProperties.USER_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class SecurityUtilTest {

    @Test
    void testCreateAuthentication() {
        OAuth2AccessTokenWithAdditionalInfo token = buildMockedOAuth2AccessToken(Set.of(SCOPE_CC_ADMIN), Collections.emptyMap());
        UserInfo userInfo = buildMockedUserInfo("id", "deploy-service", token);
        OAuth2AuthenticationToken oAuth2AuthenticationToken = SecurityUtil.createAuthentication(userInfo);
        OAuth2User principal = oAuth2AuthenticationToken.getPrincipal();
        assertFalse(principal.getAttributes()
                             .isEmpty());
        assertTrue(principal.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(authority -> Objects.equals(authority, SCOPE_CC_ADMIN)));
    }

    @Test
    void testTokenUserInfo() {
        Map<String, Object> additionalInfo = Map.of(CLIENT_ID_KEY, "client_id", USER_NAME_KEY, "username_key", USER_ID_KEY, "user_id");
        OAuth2AccessTokenWithAdditionalInfo token = buildMockedOAuth2AccessToken(Set.of(SCOPE_CC_ADMIN), additionalInfo);
        UserInfo userInfo = SecurityUtil.getTokenUserInfo(token);
        assertEquals("user_id", userInfo.getId());
        assertEquals("username_key", userInfo.getName());
        assertEquals(token, userInfo.getToken());
    }

    private OAuth2AccessTokenWithAdditionalInfo buildMockedOAuth2AccessToken(Set<String> scopes, Map<String, Object> additionalInfo) {
        OAuth2AccessTokenWithAdditionalInfo tokenWithAdditionalInfo = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        OAuth2AccessToken token = Mockito.mock(OAuth2AccessToken.class);
        Mockito.when(token.getScopes())
               .thenReturn(scopes);
        Mockito.when(tokenWithAdditionalInfo.getOAuth2AccessToken())
               .thenReturn(token);
        Mockito.when(tokenWithAdditionalInfo.getAdditionalInfo())
               .thenReturn(additionalInfo);
        return tokenWithAdditionalInfo;
    }

    private UserInfo buildMockedUserInfo(String id, String name, OAuth2AccessTokenWithAdditionalInfo token) {
        UserInfo userInfo = Mockito.mock(UserInfo.class);
        Mockito.when(userInfo.getId())
               .thenReturn(id);
        Mockito.when(userInfo.getName())
               .thenReturn(name);
        Mockito.when(userInfo.getToken())
               .thenReturn(token);
        return userInfo;
    }

}
