package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

public class ClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHelper.class);

    private CloudControllerClient client;
    private SpaceGetter spaceGetter;

    public ClientHelper(CloudControllerClient client, SpaceGetter spaceGetter) {
        this.client = client;
        this.spaceGetter = spaceGetter;
    }

    public void deleteRoute(String uri, boolean portBasedRouting) {
        if (!portBasedRouting) {
            uri = UriUtil.removePort(uri);
        }
        Pair<String, String> hostAndDomain = UriUtil.getHostAndDomain(uri);
        if (client instanceof XsCloudControllerClient) {
            XsCloudControllerClient xsClient = (XsCloudControllerClient) client;
            xsClient.deleteRoute(hostAndDomain._1, hostAndDomain._2, UriUtil.getPath(uri));
        } else {
            client.deleteRoute(hostAndDomain._1, hostAndDomain._2);
        }
    }

    public String computeSpaceId(String orgName, String spaceName) {
        CloudSpace space = spaceGetter.findSpace(client, orgName, spaceName);
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
                                   .getName(),
                              space.getName());
        }
        return null;
    }

    private CloudSpace attemptToFindSpace(String spaceId) {
        try {
            return spaceGetter.getSpace(client, spaceId);
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
