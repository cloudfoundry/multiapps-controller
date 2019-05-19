package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

public class ClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHelper.class);

    private CloudControllerClient client;

    public ClientHelper(CloudControllerClient client) {
        this.client = client;
    }

    public void deleteRoute(String uri) {
        Pair<String, String> hostAndDomain = UriUtil.getHostAndDomain(uri);
        client.deleteRoute(hostAndDomain._1, hostAndDomain._2);
    }

    public String computeSpaceId(String orgName, String spaceName) {
        CloudSpace space = client.getSpace(orgName, spaceName, false);
        if (space != null) {
            return space.getMeta()
                .getGuid()
                .toString();
        }
        return null;
    }

    public Pair<String, String> computeOrgAndSpace(String spaceId) {
        CloudSpace space = attemptToFindSpace(spaceId);
        if (space != null) {
            return new Pair<>(space.getOrganization()
                .getName(), space.getName());
        }
        return null;
    }

    private CloudSpace attemptToFindSpace(String spaceId) {
        try {
            return client.getSpace(UUID.fromString(spaceId));
        } catch (CloudOperationException e) {
            // From our point of view 403 means the same as 404 - the user does not have access to a space, so it is like it does not exist
            // for him.
            if (e.getStatusCode()
                .equals(HttpStatus.FORBIDDEN)) {
                LOGGER.debug(MessageFormat.format("The user does not have access to space with ID {0}!", spaceId));
                return null;
            }
            if (e.getStatusCode()
                .equals(HttpStatus.NOT_FOUND)) {
                LOGGER.debug(MessageFormat.format("Space with ID {0} does not exist!", spaceId));
                return null;
            }
            throw e;
        }
    }

}
