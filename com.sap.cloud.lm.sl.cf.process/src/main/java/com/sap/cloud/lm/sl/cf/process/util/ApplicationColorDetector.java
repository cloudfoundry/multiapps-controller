package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.ConflictException;

@Named("applicationColorDetector")
public class ApplicationColorDetector {

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
                      .getState() != Operation.State.ABORTED) {
            return olderApplicationColor;
        }
        String previousProcessId = operations.get(0)
                                             .getProcessId();

        ApplicationColor latestDeployedColor = getColorFromHistoricProcess(previousProcessId);
        Phase phase = getPhaseFromHistoricProcess(previousProcessId);

        if (latestDeployedColor == null) {
            return olderApplicationColor;
        }
        return phase == Phase.UNDEPLOY ? latestDeployedColor : latestDeployedColor.getAlternativeColor();
    }

    public ApplicationColor detectSingularDeployedApplicationColor(DeployedMta deployedMta) {
        if (deployedMta == null) {
            return null;
        }
        ApplicationColor deployedApplicationColor = null;
        for (DeployedMtaApplication deployedMtaApplication : deployedMta.getApplications()) {
            ApplicationColor applicationColor = CloudModelBuilderUtil.getApplicationColor(deployedMtaApplication);
            if (deployedApplicationColor == null) {
                deployedApplicationColor = (applicationColor);
            }
            if (deployedApplicationColor != applicationColor) {
                throw new ConflictException(Messages.CONFLICTING_APP_COLORS,
                                            deployedMta.getMetadata()
                                                       .getId());
            }
        }
        return deployedApplicationColor;
    }

    private ApplicationColor getOlderApplicationColor(DeployedMta deployedMta) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(application -> application.getMetadata() != null)
                          .min(Comparator.comparing(application -> application.getMetadata()
                                                                              .getCreatedAt()))
                          .map(CloudModelBuilderUtil::getApplicationColor)
                          .orElse(null);
    }

    private ApplicationColor getColorFromHistoricProcess(String processInstanceId) {
        HistoricVariableInstance colorVariableInstance = flowableFacade.getHistoricVariableInstance(processInstanceId,
                                                                                                    Variables.IDLE_MTA_COLOR.getName());

        if (colorVariableInstance == null) {
            return null;
        }

        return ApplicationColor.valueOf((String) colorVariableInstance.getValue());
    }

    private Phase getPhaseFromHistoricProcess(String processInstanceId) {
        HistoricVariableInstance phaseVariableInstance = flowableFacade.getHistoricVariableInstance(processInstanceId,
                                                                                                    Variables.PHASE.getName());
        if (phaseVariableInstance == null) {
            return null;
        }

        return Phase.valueOf((String) phaseVariableInstance.getValue());
    }

}
