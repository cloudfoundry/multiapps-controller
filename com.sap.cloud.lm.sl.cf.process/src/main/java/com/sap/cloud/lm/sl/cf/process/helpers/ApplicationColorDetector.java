package com.sap.cloud.lm.sl.cf.process.helpers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.ConflictException;

@Named("applicationColorDetector")
public class ApplicationColorDetector {

    private static final ApplicationColor COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX = ApplicationColor.BLUE;

    @Inject
    private OperationService operationService;

    @Inject
    private FlowableFacade flowableFacade;

    public ApplicationColor detectLiveApplicationColor(DeployedMta deployedMta, String correlationId) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor olderApplicationColor = getOlderApplicationColor(deployedMta);
        Operation currentOperation = operationService.createQuery()
                                                     .processId(correlationId)
                                                     .singleResult();

        List<Operation> operations = operationService.createQuery()
                                                     .mtaId(currentOperation.getMtaId())
                                                     .processType(ProcessType.BLUE_GREEN_DEPLOY)
                                                     .spaceId(currentOperation.getSpaceId())
                                                     .inFinalState()
                                                     .orderByEndTime(OrderDirection.DESCENDING)
                                                     .limitOnSelect(1)
                                                     .list();
        if (CollectionUtils.isEmpty(operations)) {
            return olderApplicationColor;
        }

        if (operations.get(0)
                      .getState() != State.ABORTED) {
            return olderApplicationColor;
        }
        String xs2BlueGreenDeployHistoricProcessInstanceId = flowableFacade.findHistoricProcessInstanceIdByProcessDefinitionKey(operations.get(0)
                                                                                                                                          .getProcessId(),
                                                                                                                                Constants.BLUE_GREEN_DEPLOY_SERVICE_ID);

        ApplicationColor latestDeployedColor = getColorFromHistoricProcess(xs2BlueGreenDeployHistoricProcessInstanceId);
        Phase phase = getPhaseFromHistoricProcess(xs2BlueGreenDeployHistoricProcessInstanceId);

        if (latestDeployedColor == null) {
            return olderApplicationColor;
        }
        return phase == Phase.UNDEPLOY ? latestDeployedColor : olderApplicationColor;
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
                throw new ConflictException(Messages.CONFLICTING_APP_COLORS,
                                            deployedMta.getMetadata()
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
