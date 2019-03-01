package com.sap.cloud.lm.sl.cf.core.security.token.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class SingleUserTokenStoreTest {

    private static final String LIQUIBASE_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/core/db/changelog/db-changelog.xml";

    private SingleUserTokenStore singleUserTokenStore;
    private DataSource testDataSource;

    private OAuth2AccessToken token1;
    private OAuth2AccessToken token2;

    @BeforeEach
    void setUp() throws Exception {
        setUpConnection();
        initializeData();
    }

    private void setUpConnection() throws Exception {
        this.testDataSource = TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION);
        singleUserTokenStore = new SingleUserTokenStore(testDataSource);
    }

    private void initializeData() {
        token1 = generateToken("lfaqwerty", Arrays.asList(TokenFactory.SCOPE_CC_READ, TokenFactory.SCOPE_CC_WRITE), "testUser", "1");
        token2 = generateToken("cfrtyghjj", Arrays.asList(TokenFactory.SCOPE_CC_ADMIN), "admin", "2");
        Arrays.asList(token1, token2)
            .stream()
            .forEach(token -> {
                singleUserTokenStore.storeAccessToken(token, generateAuthentication(token));
            });
    }

    private OAuth2AccessToken generateToken(String tokenValue, List<String> scope, String username, String userId) {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(tokenValue);
        token.setScope(new HashSet<>(scope));
        Map<String, Object> additionalInfo = new HashedMap<>();
        additionalInfo.put(TokenFactory.CLIENT_ID, SecurityUtil.CLIENT_ID);
        additionalInfo.put(TokenFactory.USER_ID, userId);
        additionalInfo.put(TokenFactory.USER_NAME, username);
        token.setAdditionalInformation(additionalInfo);
        return token;
    }

    private OAuth2Authentication generateAuthentication(OAuth2AccessToken token) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        return SecurityUtil.createAuthentication(tokenProperties.getClientId(), token.getScope(), SecurityUtil.getTokenUserInfo(token));
    }

    @AfterEach
    void tearDown() throws Exception {
        singleUserTokenStore.removeAccessToken(token1);
        singleUserTokenStore.removeAccessToken(token2);
        JdbcUtil.closeQuietly(testDataSource.getConnection());
    }

    @Test
    void testStoreNewAccessToken() {
        OAuth2AccessToken expectedToken = generateToken("foobar", Arrays.asList(TokenFactory.SCOPE_CC_WRITE, TokenFactory.SCOPE_OPENID),
            "xyz", "123");
        singleUserTokenStore.storeAccessToken(expectedToken, generateAuthentication(expectedToken));

        OAuth2AccessToken token = singleUserTokenStore.readAccessToken(expectedToken.getValue());
        assertNotNull(token, "Token was not found in token cache");
        assertEquals(expectedToken.getValue(), token.getValue());
        assertEquals(expectedToken.getScope(), token.getScope());
        assertEquals(TokenProperties.fromToken(expectedToken)
            .getUserName(),
            TokenProperties.fromToken(token)
                .getUserName());
    }

    @Test
    void testUpdateAccessToken() {
        DefaultOAuth2AccessToken expectedToken = new DefaultOAuth2AccessToken(token1);
        expectedToken.setValue("newtokenvalue");
        String username = TokenProperties.fromToken(expectedToken)
            .getUserName();
        singleUserTokenStore.storeAccessToken(expectedToken, generateAuthentication(expectedToken));

        assertNull(singleUserTokenStore.readAccessToken(token1.getValue()), MessageFormat
            .format("Token with old value {0} should be updated with {1} but it is not ", token1.getValue(), expectedToken.getValue()));

        Collection<OAuth2AccessToken> tokens = singleUserTokenStore.findTokensByUserName(username);
        assertEquals(1, tokens.size(), MessageFormat.format("Should be return only 1 token but result is:{0}", tokens.size()));
        OAuth2AccessToken token = tokens.stream()
            .findFirst()
            .get();
        assertEquals(expectedToken.getValue(), token.getValue());
    }

    @Test
    void testFindAuthenticationByUsername() {
        OAuth2Authentication expectedAuthentication = generateAuthentication(token2);
        UserInfo expectedUserInfo = (UserInfo) expectedAuthentication.getPrincipal();
        OAuth2Authentication authentication = singleUserTokenStore.findAuthenticationByUsername(TokenProperties.fromToken(token2)
            .getUserName());
        UserInfo userInfo = (UserInfo) authentication.getPrincipal();
        assertEquals(expectedAuthentication.getAuthorities(), authentication.getAuthorities());
        assertEquals(expectedAuthentication.getCredentials(), authentication.getCredentials());
        assertEquals(expectedUserInfo.getId(), userInfo.getId());
        assertEquals(expectedUserInfo.getName(), userInfo.getName());
        assertEquals(expectedUserInfo.getToken(), userInfo.getToken());
    }

}
