package com.sap.cloud.lm.sl.cf.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.flowable.engine.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named
public class ClientReleaser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientReleaser.class);

    private final CloudControllerClientProvider clientProvider;

    @Inject
    public ClientReleaser(CloudControllerClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void releaseClientFor(HistoryService historyService, String processInstanceId) {
        String user = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.USER.getName());
        String organizationName = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.ORGANIZATION_NAME.getName());
        String spaceName = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.SPACE_NAME.getName());
        String spaceGuid = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.SPACE_GUID.getName());

        try {
            clientProvider.releaseClient(user, organizationName, spaceName);
            clientProvider.releaseClient(user, spaceGuid);
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
