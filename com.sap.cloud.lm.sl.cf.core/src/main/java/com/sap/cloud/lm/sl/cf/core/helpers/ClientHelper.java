package com.sap.cloud.lm.sl.cf.core.helpers;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

public class ClientHelper {

    private CloudFoundryOperations client;

    public ClientHelper(CloudFoundryOperations client) {
        this.client = client;
    }

    public void deleteRoute(String uri, boolean portBasedRouting) {
        if (!portBasedRouting) {
            uri = UriUtil.removePort(uri);
        }
        Pair<String, String> hostAndDomain = UriUtil.getHostAndDomain(uri);
        if (client instanceof ClientExtensions) {
            ClientExtensions clientExtensions = (ClientExtensions) client;
            clientExtensions.deleteRoute(hostAndDomain._1, hostAndDomain._2, UriUtil.getPath(uri));
        } else {
            client.deleteRoute(hostAndDomain._1, hostAndDomain._2);
        }

    }

    public String computeSpaceId(String orgName, String spaceName) {
        CloudSpace space = client.getSpaces().stream().filter((s) -> isSameSpace(s, orgName, spaceName)).findAny().orElse(null);
        if (space != null) {
            return space.getMeta().getGuid().toString();
        }
        return null;
    }

    private boolean isSameSpace(CloudSpace space, String orgName, String spaceName) {
        return space.getName().equals(spaceName) && space.getOrganization().getName().equals(orgName);
    }

    public Pair<String, String> computeOrgAndSpace(String spaceId) {
        CloudSpace space = client.getSpaces().stream().filter((s) -> isSameSpace(s, spaceId)).findAny().orElse(null);
        if (space != null) {
            return new Pair<>(space.getOrganization().getName(), space.getName());
        }
        return null;
    }

    private boolean isSameSpace(CloudSpace space, String spaceId) {
        return space.getMeta().getGuid().toString().equals(spaceId);
    }

}
