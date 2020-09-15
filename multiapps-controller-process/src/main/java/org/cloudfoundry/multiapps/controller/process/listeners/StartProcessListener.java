package org.cloudfoundry.multiapps.controller.process.listeners;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ParameterMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.persistence.service.OperationService;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

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
        getHistoricOperationEventService().add(ImmutableHistoricOperationEvent.of(correlationId, EventType.STARTED));
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
