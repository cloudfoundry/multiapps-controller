package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;

class AccessTokenDaoTest {

    private static final AccessToken ACCESS_TOKEN = createAccessToken("DS", LocalDateTime.now()
                                                                                         .plusMinutes(5));

    private final AccessTokenDao accessTokenDao = createDao();

    private static AccessToken createAccessToken(String username, LocalDateTime expiresAt) {
        AccessToken accessToken = new AccessToken();
        accessToken.setUsername(username);
        accessToken.setExpiresAt(expiresAt);
        accessToken.setValue("value".getBytes());
        accessToken.setClientId("cf");
        return accessToken;
    }

    private static AccessTokenDao createDao() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        AccessTokenDtoDao accessTokenDtoDao = new AccessTokenDtoDao(entityManagerFactory);
        return new AccessTokenDao(accessTokenDtoDao);
    }

    @AfterEach
    void cleanUp() {
        accessTokenDao.deleteAllTokens();
    }

    @Test
    void testAdd() {
        accessTokenDao.add(ACCESS_TOKEN);
        List<AccessToken> accessTokens = accessTokenDao.findAll();
        assertEquals(1, accessTokens.size());
        assertTokensAreEqual(ACCESS_TOKEN, accessTokens.get(0));
    }

    @Test
    void testRemove() {
        accessTokenDao.add(ACCESS_TOKEN);
        AccessToken accessToken = accessTokenDao.findAll()
                                                .get(0);
        accessTokenDao.remove(accessToken);
        assertEquals(0, accessTokenDao.findAll()
                                      .size());
    }

    @Test
    void testGetOrderedTokensByUsername() {
        AccessToken firstAccessToken = createAccessToken("DS", LocalDateTime.now());
        AccessToken secondAccessToken = createAccessToken("DS", LocalDateTime.now()
                                                                             .plusMinutes(1));
        AccessToken thirdAccessToken = createAccessToken("DS", LocalDateTime.now()
                                                                            .plusMinutes(2));
        accessTokenDao.add(firstAccessToken);
        accessTokenDao.add(secondAccessToken);
        accessTokenDao.add(thirdAccessToken);
        AccessToken mostRecentToken = accessTokenDao.getTokensByUsernameSortedByExpirationDate("DS", OrderDirection.DESCENDING)
                                                    .get(0);
        assertTokensAreEqual(thirdAccessToken, mostRecentToken);
    }

    @Test
    void testDeleteAllTokens() {
        AccessToken firstAccessToken = createAccessToken("DS", LocalDateTime.now());
        AccessToken secondAccessToken = createAccessToken("DS", LocalDateTime.now()
                                                                             .plusMinutes(1));
        accessTokenDao.add(firstAccessToken);
        accessTokenDao.add(secondAccessToken);
        assertEquals(2, accessTokenDao.findAll()
                                      .size());
        accessTokenDao.deleteAllTokens();
        assertEquals(0, accessTokenDao.findAll()
                                      .size());
    }

    @Test
    void testDeleteTokensWithExpirationBefore() {
        AccessToken firstAccessToken = createAccessToken("DS", LocalDateTime.now());
        AccessToken secondAccessToken = createAccessToken("DS", LocalDateTime.now()
                                                                             .plusMinutes(1));
        AccessToken thirdAccessToken = createAccessToken("DS", LocalDateTime.now()
                                                                            .plusMinutes(2));
        accessTokenDao.add(firstAccessToken);
        accessTokenDao.add(secondAccessToken);
        accessTokenDao.add(thirdAccessToken);
        accessTokenDao.deleteTokensWithExpirationBefore(LocalDateTime.now());
        assertEquals(2, accessTokenDao.findAll()
                                      .size());
    }

    @Test
    void testAddTokenWithExchangedValue() {
        AccessToken accessToken = createAccessToken("DS", LocalDateTime.now());
        accessToken.setExchangedTokenValue("exchanged-value".getBytes());
        accessTokenDao.add(accessToken);
        AccessToken actualToken = accessTokenDao.findAll()
                                                .get(0);
        assertTokensAreEqual(accessToken, actualToken);
    }

    private void assertTokensAreEqual(AccessToken expectedToken, AccessToken actualToken) {
        assertEquals(expectedToken.getClientId(), actualToken.getClientId());
        assertEquals(expectedToken.getUsername(), actualToken.getUsername());
        assertEquals(expectedToken.getExpiresAt(), actualToken.getExpiresAt());
        assertEquals(expectedToken.getValue(), actualToken.getValue());
        assertEquals(expectedToken.getExchangedTokenValue(), actualToken.getExchangedTokenValue());
    }

}
