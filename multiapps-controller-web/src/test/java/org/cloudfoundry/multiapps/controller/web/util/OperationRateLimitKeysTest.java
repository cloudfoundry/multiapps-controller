package org.cloudfoundry.multiapps.controller.web.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationRateLimitKeysTest {

    private static final String SPACE_GUID = "3d4d3f9a-1a2b-4c5d-8e9f-0a1b2c3d4e5f";
    private static final String OTHER_SPACE_GUID = "9f8e7d6c-5b4a-3c2d-1e0f-abcdef123456";
    private static final String USER = "john.doe";
    private static final String OTHER_USER = "jane.roe";

    @Test
    void testSpaceKeyIsDeterministic() {
        assertEquals(OperationRateLimitKeys.spaceKey(SPACE_GUID), OperationRateLimitKeys.spaceKey(SPACE_GUID));
    }

    @Test
    void testUserKeyIsDeterministic() {
        assertEquals(OperationRateLimitKeys.userKey(SPACE_GUID, USER), OperationRateLimitKeys.userKey(SPACE_GUID, USER));
    }

    @Test
    void testSpaceKeyAndUserKeyAreDisjointForSameSpace() {
        assertNotEquals(OperationRateLimitKeys.spaceKey(SPACE_GUID), OperationRateLimitKeys.userKey(SPACE_GUID, USER));
    }

    @Test
    void testDifferentSpacesProduceDifferentKeys() {
        assertNotEquals(OperationRateLimitKeys.spaceKey(SPACE_GUID), OperationRateLimitKeys.spaceKey(OTHER_SPACE_GUID));
    }

    @Test
    void testDifferentUsersInSameSpaceProduceDifferentKeys() {
        assertNotEquals(OperationRateLimitKeys.userKey(SPACE_GUID, USER), OperationRateLimitKeys.userKey(SPACE_GUID, OTHER_USER));
    }

    @Test
    void testSameUserInDifferentSpacesProduceDifferentKeys() {
        assertNotEquals(OperationRateLimitKeys.userKey(SPACE_GUID, USER), OperationRateLimitKeys.userKey(OTHER_SPACE_GUID, USER));
    }

    @Test
    void testNoCollisionsAcrossRealisticSamples() {
        List<String> spaceGuids = List.of(SPACE_GUID, OTHER_SPACE_GUID, "11111111-2222-3333-4444-555555555555",
                                          "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        List<String> users = List.of(USER, OTHER_USER, "admin", "service-account-1");
        Set<Long> keys = new HashSet<>();
        for (String spaceGuid : spaceGuids) {
            keys.add(OperationRateLimitKeys.spaceKey(spaceGuid));
            for (String user : users) {
                keys.add(OperationRateLimitKeys.userKey(spaceGuid, user));
            }
        }
        int expectedDistinctKeys = spaceGuids.size() + spaceGuids.size() * users.size();
        assertEquals(expectedDistinctKeys, keys.size());
    }

    @Test
    void testSpaceKeyIsStableAcrossRuns() {
        long firstValue = OperationRateLimitKeys.spaceKey(SPACE_GUID);
        long secondValue = OperationRateLimitKeys.spaceKey(SPACE_GUID);
        assertTrue(firstValue == secondValue);
    }
}
