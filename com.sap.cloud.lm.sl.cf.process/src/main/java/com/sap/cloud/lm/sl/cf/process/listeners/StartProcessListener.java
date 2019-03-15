package com.sap.cloud.lm.sl.cf.process.listeners;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class StartProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessListener.class);

    @Inject
    private OperationDao operationDao;
    @Inject
    private ProcessTypeParser processTypeParser;
    @Autowired(required = false)
    private ProcessTypeToOperationMetadataMapper operationMetadataMapper;
    @Inject
    private ApplicationConfiguration configuration;

    Supplier<ZonedDateTime> currentTimeSupplier = ZonedDateTime::now;

    @Override
    protected void notifyInternal(DelegateExecution context) {
        String correlationId = StepsUtil.getCorrelationId(context);
        getStepLogger().info(Messages.STARTING_OPERATION, correlationId);
        ProcessType processType = processTypeParser.getProcessType(context);

        if (operationDao.find(correlationId) == null) {
            addOperation(context, correlationId, processType);
        }
        logProcessEnvironment();
        logProcessVariables(context, processType);
    }

    private void logProcessEnvironment() {
        Map<String, String> environment = configuration.getFilteredEnv();
        getStepLogger().debug(Messages.PROCESS_ENVIRONMENT, JsonUtil.toJson(environment, true));
    }

    private void logProcessVariables(DelegateExecution context, ProcessType processType) {
        getStepLogger().debug(Messages.CURRENT_USER, StepsUtil.determineCurrentUser(context, getStepLogger()));
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
        Operation operation = new Operation().processId(correlationId)
            .processType(processType)
            .startedAt(currentTimeSupplier.get())
            .spaceId(StepsUtil.getSpaceId(context))
            .user(StepsUtil.determineCurrentUser(context, getStepLogger()))
            .acquiredLock(false);
        operationDao.add(operation);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
