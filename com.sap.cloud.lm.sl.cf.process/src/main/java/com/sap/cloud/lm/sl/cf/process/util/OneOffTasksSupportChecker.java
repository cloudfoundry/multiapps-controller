package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;

@Component
public class OneOffTasksSupportChecker {

    public boolean areOneOffTasksSupported(CloudFoundryOperations client) {
        return clientSupportsTasks(client) && controllerSupportsTasks(client);
    }

    private boolean controllerSupportsTasks(CloudFoundryOperations client) {
        CloudInfo cloudInfo = client.getCloudInfo();
        if (!(cloudInfo instanceof CloudInfoExtended)) {
            return false;
        }
        CloudInfoExtended extendedCloudInfo = (CloudInfoExtended) cloudInfo;
        return extendedCloudInfo.hasTasksSupport();
    }

    private boolean clientSupportsTasks(CloudFoundryOperations client) {
        return client instanceof ClientExtensions;
    }

}
