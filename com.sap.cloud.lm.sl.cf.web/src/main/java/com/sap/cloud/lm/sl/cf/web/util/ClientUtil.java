package com.sap.cloud.lm.sl.cf.web.util;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.common.SLException;

public final class ClientUtil {

    public static CloudFoundryOperations getCloudFoundryClient(CloudFoundryClientProvider provider, String org, String space)
        throws SLException {
        return provider.getCloudFoundryClient(SecurityContextUtil.getUserInfo().getToken(), org, space, null);
    }

    private static boolean isTheSameSpace(CloudSpace space, String orgName, String spaceName) {
        return space.getOrganization().getName().equals(orgName) && space.getName().equals(spaceName);
    }

    public static String computeSpaceId(CloudFoundryOperations client, String orgName, String spaceName) {
        for (CloudSpace space : client.getSpaces()) {
            if (isTheSameSpace(space, orgName, spaceName)) {
                return space.getMeta().getGuid().toString();
            }
        }
        return null;
    }

}
