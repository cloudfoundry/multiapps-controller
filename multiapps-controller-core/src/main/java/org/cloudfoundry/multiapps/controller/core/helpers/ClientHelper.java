package org.cloudfoundry.multiapps.controller.core.helpers;

import java.text.MessageFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;

public class ClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHelper.class);

    private final CloudSpaceClient client;

    public ClientHelper(CloudSpaceClient client) {
        this.client = client;
    }

    public String computeSpaceId(String orgName, String spaceName) {
        try {
            CloudSpace space = client.getSpace(orgName, spaceName);
            return space.getGuid()
                        .toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public CloudSpace attemptToFindSpace(String spaceId) {
        try {
            return client.getSpace(UUID.fromString(spaceId));
        } catch (CloudOperationException e) {
            // From our point of view 403 means the same as 404 - the user does not have access to a space, so it is like it does not exist
            // for him.
            if (HttpStatus.FORBIDDEN == e.getStatusCode()) {
                LOGGER.debug(MessageFormat.format("The user does not have access to space with ID {0}!", spaceId));
                return null;
            }
            if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                LOGGER.debug(MessageFormat.format("Space with ID {0} does not exist!", spaceId));
                return null;
            }
            throw e;
        }
    }

}
