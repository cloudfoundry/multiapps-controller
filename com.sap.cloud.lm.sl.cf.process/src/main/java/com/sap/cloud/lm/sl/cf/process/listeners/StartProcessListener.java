package com.sap.cloud.lm.sl.cf.process.listeners;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class StartProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessListener.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Inject
    private OperationDao ongoingOperationDao;
    @Inject
    private ProcessTypeParser processTypeParser;
    @Autowired(required = false)
    private ProcessTypeToOperationMetadataMapper processTypeToServiceMetadataMapper;
    @Inject
    private Configuration configuration;

    @Override
    protected void notifyInternal(DelegateExecution context) throws SLException {
        String correlationId = StepsUtil.getCorrelationId(context);
        if (correlationId == null) {
            correlationId = context.getProcessInstanceId();
            context.setVariable(Constants.VAR_CORRELATION_ID, correlationId);
        }
        ProcessType processType = processTypeParser.getProcessType(context);

        if (ongoingOperationDao.find(correlationId) == null) {
            addOngoingOperation(context, correlationId, processType);
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
        OperationMetadata operationMetadata = processTypeToServiceMetadataMapper.getOperationMetadata(processType);
        Map<String, Object> result = new HashMap<>();
        for (ParameterMetadata parameterMetadata : operationMetadata.getParameters()) {
            if (context.hasVariable(parameterMetadata.getId())) {
                Object variableValue = context.getVariable(parameterMetadata.getId());
                result.put(parameterMetadata.getId(), variableValue);
            }
        }
        return result;
    }

    private void addOngoingOperation(DelegateExecution context, String correlationId, ProcessType processType) {
        String startedAt = FORMATTER.format(ZonedDateTime.now());
        String user = StepsUtil.determineCurrentUser(context, getStepLogger());
        String spaceId = StepsUtil.getSpaceId(context);
        Operation process = new Operation(correlationId, processType, startedAt, spaceId, null, user, false, null);
        ongoingOperationDao.add(process);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
