package com.sap.cloud.lm.sl.cf.process.listeners;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named
public class StartProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessListener.class);

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
    protected void notifyInternal(DelegateExecution context) {
        String correlationId = StepsUtil.getCorrelationId(context);
        ProcessType processType = processTypeParser.getProcessType(context);

        if (getOperation(correlationId) == null) {
            addOperation(context, correlationId, processType);
        }
        getHistoricOperationEventPersister().add(correlationId, EventType.STARTED);
        logProcessEnvironment();
        logProcessVariables(context, processType);
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

    private void logProcessVariables(DelegateExecution context, ProcessType processType) {
        getStepLogger().debug(Messages.CURRENT_USER, StepsUtil.determineCurrentUser(context));
        getStepLogger().debug(Messages.CLIENT_SPACE, StepsUtil.getSpace(context));
        getStepLogger().debug(Messages.CLIENT_ORG, StepsUtil.getOrg(context));
        Map<String, Object> processVariables = findProcessVariables(context, processType);
        getStepLogger().debug(Messages.PROCESS_VARIABLES, JsonUtil.toJson(processVariables, true));
    }

    protected Map<String, Object> findProcessVariables(DelegateExecution context, ProcessType processType) {
        OperationMetadata operationMetadata = operationMetadataMapper.getOperationMetadata(processType);
        Map<String, Object> result = new HashMap<>();
        for (ParameterMetadata parameterMetadata : operationMetadata.getParameters()) {
            if (context.hasVariable(parameterMetadata.getId())) {
                Object variableValue = context.getVariable(parameterMetadata.getId());
                result.put(parameterMetadata.getId(), variableValue);
            }
        }
        return result;
    }

    private void addOperation(DelegateExecution context, String correlationId, ProcessType processType) {
        Operation operation = ImmutableOperation.builder()
                                                .processId(correlationId)
                                                .processType(processType)
                                                .startedAt(currentTimeSupplier.get())
                                                .spaceId(StepsUtil.getSpaceId(context))
                                                .user(StepsUtil.determineCurrentUser(context))
                                                .hasAcquiredLock(false)
                                                .build();
        operationService.add(operation);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
