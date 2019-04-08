package com.sap.cloud.lm.sl.cf.process.helpers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;

@Named("applicationColorDetector")
public class ApplicationColorDetector {

    private static final ApplicationColor COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX = ApplicationColor.BLUE;

    @Inject
    private OperationDao operationDao;

    @Inject
    private FlowableFacade flowableFacade;

    public ApplicationColor detectLiveApplicationColor(DeployedMta deployedMta, String correlationId) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor olderApplicationColor = getOlderApplicationColor(deployedMta);
        Operation currentOperation = operationDao.find(correlationId);

        Operation lastOperation = operationDao.find(createLatestOperationFilter(currentOperation.getMtaId(), currentOperation.getSpaceId()))
            .get(0);

        if (lastOperation.getState() != State.ABORTED) {
            return olderApplicationColor;
        }
        String xs2BlueGreenDeployHistoricProcessInstanceId = flowableFacade
            .findHistoricProcessInstanceIdByProcessDefinitionKey(lastOperation.getProcessId(), Constants.BLUE_GREEN_DEPLOY_SERVICE_ID);

        ApplicationColor latestDeployedColor = getColorFromHistoricProcess(xs2BlueGreenDeployHistoricProcessInstanceId);
        Phase phase = getPhaseFromHistoricProcess(xs2BlueGreenDeployHistoricProcessInstanceId);

        if (latestDeployedColor == null || phase == null) {
            return olderApplicationColor;
        }
        if (phase == Phase.UNDEPLOY) {
            return latestDeployedColor;
        }
        return olderApplicationColor;
    }

    public ApplicationColor detectSingularDeployedApplicationColor(DeployedMta deployedMta) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor deployedApplicationColor = null;
        for (DeployedMtaModule module : deployedMta.getModules()) {
            ApplicationColor moduleApplicationColor = getApplicationColor(module);
            if (deployedApplicationColor == null) {
                deployedApplicationColor = (moduleApplicationColor);
            }
            if (deployedApplicationColor != moduleApplicationColor) {
                throw new ConflictException(Messages.CONFLICTING_APP_COLORS, deployedMta.getMetadata()
                    .getId());
            }
        }
        return deployedApplicationColor;
    }

    private ApplicationColor getOlderApplicationColor(DeployedMta deployedMta) {
        return deployedMta.getModules()
            .stream()
            .min(Comparator.comparing(DeployedMtaModule::getCreatedOn))
            .map(this::getApplicationColor)
            .orElse(null);
    }

    private ApplicationColor getApplicationColor(DeployedMtaModule deployedMtaModule) {
        return Arrays.stream(ApplicationColor.values())
            .filter(color -> deployedMtaModule.getAppName()
                .endsWith(color.asSuffix()))
            .findFirst()
            .orElse(COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX);
    }

    private OperationFilter createLatestOperationFilter(String mtaId, String spaceId) {
        return new OperationFilter.Builder().mtaId(mtaId)
            .processType(ProcessType.BLUE_GREEN_DEPLOY)
            .spaceId(spaceId)
            .inFinalState()
            .orderByEndTime()
            .descending()
            .maxResults(1)
            .build();
    }

    private Phase getPhaseFromHistoricProcess(String processInstanceId) {
        HistoricVariableInstance phaseVariableInstance = flowableFacade.getHistoricVariableInstance(processInstanceId, Constants.VAR_PHASE);
        if (phaseVariableInstance == null) {
            return null;
        }

        return Phase.valueOf((String) phaseVariableInstance.getValue());
    }

    private ApplicationColor getColorFromHistoricProcess(String processInstanceId) {
        HistoricVariableInstance colorVariableInstance = flowableFacade.getHistoricVariableInstance(processInstanceId,
            Constants.VAR_MTA_COLOR);
        if (colorVariableInstance == null) {
            return null;
        }

        return ApplicationColor.valueOf((String) colorVariableInstance.getValue());
    }

}
