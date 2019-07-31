package com.sap.cloud.lm.sl.cf.process.util;

import org.flowable.engine.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

public class ClientReleaser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientReleaser.class);

    private final CloudControllerClientProvider clientProvider;

    public ClientReleaser(CloudControllerClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void releaseClientFor(HistoryService historyService, String processInstanceId) {
        String user = HistoricVariablesUtil.getCurrentUser(historyService, processInstanceId);
        String spaceName = (String) HistoricVariablesUtil.getHistoricVarInstanceValue(historyService, processInstanceId,
                                                                                      Constants.VAR_SPACE)
                                                         .getValue();
        String orgName = (String) HistoricVariablesUtil.getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_ORG)
                                                       .getValue();
        String spaceId = (String) HistoricVariablesUtil.getHistoricVarInstanceValue(historyService, processInstanceId,
                                                                                    com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID)
                                                       .getValue();

        try {
            clientProvider.releaseClient(user, orgName, spaceName);
            clientProvider.releaseClient(user, spaceId);
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }
}
