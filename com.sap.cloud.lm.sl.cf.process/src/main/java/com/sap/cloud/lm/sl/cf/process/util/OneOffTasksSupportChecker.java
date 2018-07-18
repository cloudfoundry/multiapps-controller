package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;

@Component
public class OneOffTasksSupportChecker {

    public boolean areOneOffTasksSupported(CloudControllerClient client) {
        return clientSupportsTasks(client) && controllerSupportsTasks(client);
    }

    private boolean controllerSupportsTasks(CloudControllerClient client) {
        CloudInfo cloudInfo = client.getCloudInfo();
        if (!(cloudInfo instanceof CloudInfoExtended)) {
            return false;
        }
        CloudInfoExtended extendedCloudInfo = (CloudInfoExtended) cloudInfo;
        return extendedCloudInfo.hasTasksSupport();
    }

    private boolean clientSupportsTasks(CloudControllerClient client) {
        return client instanceof ClientExtensions;
    }

}
