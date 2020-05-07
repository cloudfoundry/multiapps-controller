package com.sap.cloud.lm.sl.cf.process.listeners;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerialization;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named
public class StartProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Inject
    private OperationService operationService;
    @Inject
    protected ProcessTypeParser processTypeParser;
    @Autowired(required = false)
    private ProcessTypeToOperationMetadataMapper operationMetadataMapper;
    @Inject
    private ApplicationConfiguration configuration;

    Supplier<ZonedDateTime> currentTimeSupplier = ZonedDateTime::now;

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        if (!isRootProcess(execution)) {
            return;
        }
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        ProcessType processType = processTypeParser.getProcessType(execution);

        if (getOperation(correlationId) == null) {
            addOperation(execution, correlationId, processType);
        }
        getHistoricOperationEventPersister().add(correlationId, EventType.STARTED);
        logProcessEnvironment();
        logProcessVariables(execution, processType);
    }

    private Operation getOperation(String correlationId) {
        try {
            return operationService.createQuery()
                                   .processId(correlationId)
                                   .singleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private void logProcessEnvironment() {
        Map<String, String> environment = configuration.getNotSensitiveVariables();
        getStepLogger().debug(Messages.PROCESS_ENVIRONMENT, JsonUtil.toJson(environment, true));
    }

    private void logProcessVariables(DelegateExecution execution, ProcessType processType) {
        getStepLogger().debug(Messages.CURRENT_USER, StepsUtil.determineCurrentUser(execution));
        getStepLogger().debug(Messages.CLIENT_SPACE, VariableHandling.get(execution, Variables.SPACE_NAME));
        getStepLogger().debug(Messages.CLIENT_ORGANIZATION, VariableHandling.get(execution, Variables.ORGANIZATION_NAME));
        Map<String, Object> processVariables = findProcessVariables(execution, processType);
        getStepLogger().debug(Messages.PROCESS_VARIABLES, SecureSerialization.toJson(processVariables));
    }

    protected Map<String, Object> findProcessVariables(DelegateExecution execution, ProcessType processType) {
        OperationMetadata operationMetadata = operationMetadataMapper.getOperationMetadata(processType);
        Map<String, Object> result = new HashMap<>();
        for (ParameterMetadata parameterMetadata : operationMetadata.getParameters()) {
            if (execution.hasVariable(parameterMetadata.getId())) {
                Object variableValue = execution.getVariable(parameterMetadata.getId());
                result.put(parameterMetadata.getId(), variableValue);
            }
        }
        return result;
    }

    private void addOperation(DelegateExecution execution, String correlationId, ProcessType processType) {
        Operation operation = ImmutableOperation.builder()
                                                .processId(correlationId)
                                                .processType(processType)
                                                .startedAt(currentTimeSupplier.get())
                                                .spaceId(VariableHandling.get(execution, Variables.SPACE_GUID))
                                                .user(StepsUtil.determineCurrentUser(execution))
                                                .hasAcquiredLock(false)
                                                .namespace(VariableHandling.get(execution, Variables.MTA_NAMESPACE))
                                                .build();
        operationService.add(operation);
    }

}
