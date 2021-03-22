package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAccessToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AccessTokenServiceTest {

    private static final LocalDateTime DATE_1 = LocalDateTime.of(2021, Month.MARCH, 15, 7, 54, 12);
    private static final LocalDateTime DATE_2 = LocalDateTime.of(2021, Month.MARCH, 15, 7, 54, 20);

    private static final AccessToken ACCESS_TOKEN_1 = createAccessToken(1, getFakeTokenValue(), "hello@gmail.com", DATE_1);
    private static final AccessToken ACCESS_TOKEN_2 = createAccessToken(2, getFakeTokenValue(), "hello@gmail.com", DATE_1);
    private static final AccessToken ACCESS_TOKEN_3 = createAccessToken(3, getFakeTokenValue(), "another@gmail.com", DATE_1);
    private final AccessTokenService accessTokenService = createAccessTokenService();

    @AfterEach
    void cleanUp() {
        accessTokenService.createQuery()
                          .delete();
    }

    @Test
    void testAdd() {
        accessTokenService.add(ACCESS_TOKEN_1);
        List<AccessToken> accessTokens = accessTokenService.createQuery()
                                                           .list();
        assertEquals(1, accessTokens.size());
        verifyAccessTokensAreEqual(ACCESS_TOKEN_1, accessTokens.get(0));
    }

    @Test
    void testFindById() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.add(ACCESS_TOKEN_2);
        AccessToken accessToken = accessTokenService.createQuery()
                                                    .id(1L)
                                                    .singleResult();
        verifyAccessTokensAreEqual(ACCESS_TOKEN_1, accessToken);
    }

    @Test
    void testDeleteById() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.createQuery()
                          .id(ACCESS_TOKEN_1.getId())
                          .delete();
        assertTrue(accessTokenService.createQuery()
                                     .list()
                                     .isEmpty());
    }

    @Test
    void testFindByUsername() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.add(ACCESS_TOKEN_2);
        accessTokenService.add(ACCESS_TOKEN_3);
        List<AccessToken> accessTokens = accessTokenService.createQuery()
                                                           .username("hello@gmail.com")
                                                           .list();
        assertEquals(2, accessTokens.size());
        verifyAccessTokensAreEqual(ACCESS_TOKEN_1, accessTokens.get(0));
        verifyAccessTokensAreEqual(ACCESS_TOKEN_2, accessTokens.get(1));
    }

    @Test
    void testDeleteByUsername() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.add(ACCESS_TOKEN_2);
        accessTokenService.add(ACCESS_TOKEN_3);
        assertEquals(2, accessTokenService.createQuery()
                                          .username("hello@gmail.com")
                                          .delete());
        List<AccessToken> accessTokens = accessTokenService.createQuery()
                                                           .list();
        assertEquals(1, accessTokens.size());
        verifyAccessTokensAreEqual(ACCESS_TOKEN_3, accessTokens.get(0));
    }

    @Test
    void testFindLessThanTokens() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.add(ACCESS_TOKEN_2);
        accessTokenService.add(ACCESS_TOKEN_3);
        List<AccessToken> accessTokens = accessTokenService.createQuery()
                                                           .expiresBefore(DATE_2)
                                                           .list();
        assertEquals(3, accessTokens.size());
        verifyAccessTokensAreEqual(ACCESS_TOKEN_1, accessTokens.get(0));
        verifyAccessTokensAreEqual(ACCESS_TOKEN_2, accessTokens.get(1));
        verifyAccessTokensAreEqual(ACCESS_TOKEN_3, accessTokens.get(2));
    }

    @Test
    void testDeleteByOlderThan() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.add(ACCESS_TOKEN_2);
        assertEquals(2, accessTokenService.createQuery()
                                          .expiresBefore(DATE_2)
                                          .delete());
    }

    @Test
    void testDeleteByOlderThanNothingDeleted() {
        accessTokenService.add(ACCESS_TOKEN_1);
        accessTokenService.add(ACCESS_TOKEN_2);
        assertEquals(2, accessTokenService.createQuery()
                                          .expiresBefore(DATE_2)
                                          .delete());
    }

    @Test
    void testUpdateToken() {
        accessTokenService.add(ACCESS_TOKEN_1);
        AccessToken accessToken = createAccessToken(1, getFakeTokenValue(), "hello@gmail.com", DATE_2);
        AccessToken updateAccessToken = accessTokenService.update(ACCESS_TOKEN_1, accessToken);
        verifyAccessTokensAreEqual(accessToken, updateAccessToken);
        List<AccessToken> accessTokens = accessTokenService.createQuery()
                                                           .list();
        assertEquals(1, accessTokens.size());
        verifyAccessTokensAreEqual(accessToken, accessTokens.get(0));
    }

    @Test
    void testThrowExceptionOnConflictingEntity() {
        accessTokenService.add(ACCESS_TOKEN_1);
        Exception exception = assertThrows(ConflictException.class, () -> accessTokenService.add(ACCESS_TOKEN_1));
        assertEquals("Access token with ID \"1\" already exist", exception.getMessage());
    }

    @Test
    void testThrowExceptionOnEntityNotFound() {
        Exception exception = assertThrows(NotFoundException.class, () -> accessTokenService.update(ACCESS_TOKEN_1, ACCESS_TOKEN_2));
        assertEquals("Access token with ID \"1\" does not exist", exception.getMessage());
    }

    private AccessTokenService createAccessTokenService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        AccessTokenService.AccessTokenMapper accessTokenMapper = new AccessTokenService.AccessTokenMapper();
        return new AccessTokenService(entityManagerFactory, accessTokenMapper);
    }

    private static AccessToken createAccessToken(long id, byte[] value, String username, LocalDateTime expiresAt) {
        return ImmutableAccessToken.builder()
                                   .id(id)
                                   .value(value)
                                   .username(username)
                                   .expiresAt(expiresAt)
                                   .build();
    }

    private static byte[] getFakeTokenValue() {
        return TestUtil.getResourceAsString("access-token-value.txt", AccessTokenServiceTest.class)
                       .getBytes(StandardCharsets.UTF_8);
    }

    private void verifyAccessTokensAreEqual(AccessToken expectedAccessToken, AccessToken actualAccessToken) {
        assertEquals(expectedAccessToken.getId(), actualAccessToken.getId());
        assertArrayEquals(expectedAccessToken.getValue(), actualAccessToken.getValue());
        assertEquals(expectedAccessToken.getUsername(), actualAccessToken.getUsername());
        assertEquals(expectedAccessToken.getExpiresAt(), actualAccessToken.getExpiresAt());
    }
}
