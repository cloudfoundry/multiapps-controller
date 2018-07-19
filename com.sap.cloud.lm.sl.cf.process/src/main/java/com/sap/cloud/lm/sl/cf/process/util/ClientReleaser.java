package com.sap.cloud.lm.sl.cf.process.util;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

public class ClientReleaser {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientReleaser.class);

	private final ActivitiEvent event;
	private final CloudControllerClientProvider clientProvider;

	public ClientReleaser(ActivitiEvent event, CloudControllerClientProvider clientProvider) {
		this.event = event;
		this.clientProvider = clientProvider;
	}

	public void releaseClient() {
        HistoryService historyService = event.getEngineServices()
            .getHistoryService();
        String processInstanceId = event.getProcessInstanceId();

        String user = getCurrentUser(historyService, processInstanceId);
        String spaceName = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_SPACE).getValue();
        String orgName = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_ORG).getValue();
        String spaceId = (String) getHistoricVarInstanceValue(historyService, processInstanceId, 
        		com.sap.cloud.lm.sl.persistence.message.Constants.VARIABLE_NAME_SPACE_ID).getValue();

        try {
            clientProvider.releaseClient(user, orgName, spaceName);
            clientProvider.releaseClient(user, spaceId);
        } catch (SLException e) {
            LOGGER.warn(e.getMessage());
        }
    }

	protected String getCurrentUser(HistoryService historyService, String processInstanceId) {
		String user = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_USER)
				.getValue();
		if (user == null) {
			user = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.PARAM_INITIATOR)
					.getValue();
		}
		return user;
	}

	public HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
			String parameter) {
		return historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId)
				.variableName(parameter).singleResult();
	}
}
