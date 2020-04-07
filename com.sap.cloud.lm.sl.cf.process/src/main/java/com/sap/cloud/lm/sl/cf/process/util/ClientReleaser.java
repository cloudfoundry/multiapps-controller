package com.sap.cloud.lm.sl.cf.process.util;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;

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
        String spaceName = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.SPACE.getName());
        String orgName = HistoryUtil.getVariableValue(historyService, processInstanceId, Variables.ORG.getName());
        String spaceId = HistoryUtil.getVariableValue(historyService, processInstanceId,
                                                      com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID);

        try {
            clientProvider.releaseClient(user, orgName, spaceName);
            clientProvider.releaseClient(user, spaceId);
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
