package org.cloudfoundry.multiapps.controller.web.security;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpaceWithUserTest {

    private static final UUID USER_GUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SPACE_GUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void testGettersExposeConstructorValues() {
        SpaceWithUser sut = new SpaceWithUser(USER_GUID, SPACE_GUID);

        Assertions.assertEquals(USER_GUID, sut.getUserGuid());
        Assertions.assertEquals(SPACE_GUID, sut.getSpaceGuid());
    }

    @Test
    void testEqualsReturnsTrueForSameInstance() {
        SpaceWithUser sut = new SpaceWithUser(USER_GUID, SPACE_GUID);

        Assertions.assertEquals(sut, sut);
    }

    @Test
    void testEqualsReturnsTrueForEquivalentValues() {
        SpaceWithUser a = new SpaceWithUser(USER_GUID, SPACE_GUID);
        SpaceWithUser b = new SpaceWithUser(USER_GUID, SPACE_GUID);

        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsReturnsFalseForDifferentUserGuid() {
        SpaceWithUser a = new SpaceWithUser(USER_GUID, SPACE_GUID);
        SpaceWithUser b = new SpaceWithUser(UUID.randomUUID(), SPACE_GUID);

        Assertions.assertNotEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseForDifferentSpaceGuid() {
        SpaceWithUser a = new SpaceWithUser(USER_GUID, SPACE_GUID);
        SpaceWithUser b = new SpaceWithUser(USER_GUID, UUID.randomUUID());

        Assertions.assertNotEquals(a, b);
    }

    @Test
    void testEqualsReturnsFalseForNull() {
        SpaceWithUser sut = new SpaceWithUser(USER_GUID, SPACE_GUID);

        Assertions.assertNotEquals(sut, null);
    }

    @Test
    void testEqualsReturnsFalseForOtherType() {
        SpaceWithUser sut = new SpaceWithUser(USER_GUID, SPACE_GUID);

        Assertions.assertNotEquals(sut, "not-a-space");
    }
}
