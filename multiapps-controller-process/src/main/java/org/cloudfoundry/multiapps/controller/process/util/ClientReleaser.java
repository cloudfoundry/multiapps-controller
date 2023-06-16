package org.cloudfoundry.multiapps.controller.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.HistoryService;

@Named
public class ClientReleaser {

    private final CloudControllerClientProvider clientProvider;

    @Inject
    public ClientReleaser(CloudControllerClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void releaseClientFor(HistoryService historyService, String processInstanceId) {
        String user = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.USER.getName());
        String spaceGuid = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.SPACE_GUID.getName());

        clientProvider.releaseClient(user, spaceGuid);
    }
}
