package com.sap.cloud.lm.sl.cf.core.util;

import static com.sap.cloud.lm.sl.cf.client.util.TokenProperties.CLIENT_ID_KEY;
import static com.sap.cloud.lm.sl.cf.client.util.TokenProperties.USER_ID_KEY;
import static com.sap.cloud.lm.sl.cf.client.util.TokenProperties.USER_NAME_KEY;
import static com.sap.cloud.lm.sl.cf.core.util.SecurityUtil.USER_INFO;
import static org.cloudfoundry.client.constants.Constants.EXCHANGED_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.Constants;

class SecurityUtilTest {

    @Test
    void testCreateAuthentication() {
        OAuth2AccessTokenWithAdditionalInfo token = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getToken()).thenReturn(token);
        when(token.getScopes()).thenReturn(createScopes());
        Map<String, Object> info = new HashMap<>();
        info.put(Constants.CLIENT_ID, "cf");
        when(token.getAdditionalInfo()).thenReturn(info);
        OAuth2AuthenticationToken authentication = SecurityUtil.createAuthentication(userInfo);
        assertEquals(userInfo, authentication.getPrincipal()
                                             .getAttributes()
                                             .get(USER_INFO));
        assertEquals(createScopes(), authentication.getAuthorities()
                                                   .stream()
                                                   .map(GrantedAuthority::getAuthority)
                                                   .collect(Collectors.toSet()));
    }

    @Test
    void testGetTokenUserInfoWithoutExchangedToken() {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(CLIENT_ID_KEY, "key");
        additionalInfo.put(USER_NAME_KEY, "deploy-service-test");
        additionalInfo.put(USER_ID_KEY, "123");
        OAuth2AccessTokenWithAdditionalInfo token = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(token.getAdditionalInfo()).thenReturn(additionalInfo);
        TokenFactory tokenFactory = getMockedTokenFactory("tokenValue", additionalInfo, token);
        UserInfo userInfo = SecurityUtil.getTokenUserInfo(token, tokenFactory);
        assertEquals("deploy-service-test", userInfo.getName());
        assertEquals("123", userInfo.getId());
        assertEquals(token, userInfo.getToken());
    }

    @Test
    void testGetTokenUserInfoWithExchangedToken() {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put(USER_NAME_KEY, "deploy-service-test");
        additionalInfo.put(USER_ID_KEY, "123");
        additionalInfo.put(EXCHANGED_TOKEN, "exchanged_token_value");
        OAuth2AccessTokenWithAdditionalInfo token = mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(token.getAdditionalInfo()).thenReturn(additionalInfo);
        TokenFactory tokenFactory = getMockedTokenFactory("exchanged_token_value", additionalInfo, token);
        UserInfo userInfo = SecurityUtil.getTokenUserInfo(token, tokenFactory);
        assertEquals("deploy-service-test", userInfo.getName());
        assertEquals("123", userInfo.getId());
        assertEquals(token, userInfo.getToken());
    }

    private Set<String> createScopes() {
        Set<String> scopes = new HashSet<>();
        scopes.add(TokenFactory.SCOPE_CC_WRITE);
        scopes.add(TokenFactory.SCOPE_CC_READ);
        return scopes;
    }

    private TokenFactory getMockedTokenFactory(String tokenValue, Map<String, Object> additionalInfo,
                                               OAuth2AccessTokenWithAdditionalInfo token) {
        TokenFactory tokenFactory = mock(TokenFactory.class);
        when(tokenFactory.createToken(tokenValue, additionalInfo)).thenReturn(token);
        return tokenFactory;
    }

}
