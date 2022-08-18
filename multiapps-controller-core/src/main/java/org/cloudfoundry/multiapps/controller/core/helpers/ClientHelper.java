package org.cloudfoundry.multiapps.controller.core.helpers;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

public class ClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHelper.class);

    private final CloudControllerClient client;

    public ClientHelper(CloudControllerClient client) {
        this.client = client;
    }

    public void deleteRoute(CloudRoute route) {
        client.deleteRoute(route.getHost(), route.getDomain()
                                                 .getName(), route.getPath());
    }

    public String computeSpaceId(String orgName, String spaceName) {
        CloudSpace space = client.getSpace(orgName, spaceName, false);
        if (space != null) {
            return space.getMetadata()
                        .getGuid()
                        .toString();
        }
        return null;
    }

    public CloudTarget computeTarget(String spaceId) {
        CloudSpace space = attemptToFindSpace(spaceId);
        if (space != null) {
            return new CloudTarget(space.getOrganization()
                                        .getName(),
                                   space.getName());
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
