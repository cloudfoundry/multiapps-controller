package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.http.HttpStatus;

public class SpaceGetter {

    public CloudSpace findSpace(CloudFoundryOperations client, String orgName, String spaceName) {
        return client.getSpaces().stream().filter((s) -> isSameSpace(s, orgName, spaceName)).findAny().orElse(null);
    }

    private boolean isSameSpace(CloudSpace space, String orgName, String spaceName) {
        return space.getName().equals(spaceName) && space.getOrganization().getName().equals(orgName);
    }

    public CloudSpace getSpace(CloudFoundryOperations client, String spaceId) {
        return client.getSpaces().stream().filter((s) -> isSameSpace(s, spaceId)).findAny().orElseThrow(
            () -> new CloudFoundryException(HttpStatus.NOT_FOUND, MessageFormat.format("Space with ID {0} does not exist", spaceId)));
    }

    private boolean isSameSpace(CloudSpace space, String spaceId) {
        return space.getMeta().getGuid().toString().equals(spaceId);
    }

}
