package org.cloudfoundry.multiapps.controller.web.security;

import java.util.Objects;
import java.util.UUID;

public class SpaceWithUser {

    private final UUID userGuid;
    private final UUID spaceGuid;

    public SpaceWithUser(UUID userGuid, UUID spaceGuid) {
        this.userGuid = userGuid;
        this.spaceGuid = spaceGuid;
    }

    public UUID getUserGuid() {
        return userGuid;
    }

    public UUID getSpaceGuid() {
        return spaceGuid;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        SpaceWithUser spaceWithUser = (SpaceWithUser) object;
        return Objects.equals(userGuid, spaceWithUser.userGuid) && Objects.equals(spaceGuid, spaceWithUser.spaceGuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userGuid, spaceGuid);
    }
}
