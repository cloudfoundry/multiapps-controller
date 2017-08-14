package com.sap.cloud.lm.sl.cf.process.listeners;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToServiceMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;

@Component("startProcessListener")
public class StartProcessListener extends AbstractXS2ProcessExecutionListener {

    private static final long serialVersionUID = -447062578903384602L;

    private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessListener.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    @Override
    protected void notifyInternal(DelegateExecution context) throws SLException {
        String correlationId = StepsUtil.getCorrelationId(context);
        if (correlationId == null) {
            correlationId = context.getProcessInstanceId();
            context.setVariable(Constants.VAR_CORRELATION_ID, correlationId);
        }
        ProcessType processType = StepsUtil.getProcessType(context);
        if (ongoingOperationDao.find(correlationId) == null) {
            addOngoingOperation(context, correlationId, processType);
        }
        logProcessEnvironment(context);
        logProcessVariables(context, processType);
    }

    private void logProcessEnvironment(DelegateExecution context) {
        Map<String, String> environment = ConfigurationUtil.getFilteredEnv();
        debug(context, MessageFormat.format(Messages.PROCESS_ENVIRONMENT, JsonUtil.toJson(environment, true)), LOGGER);
    }

    private void logProcessVariables(DelegateExecution context, ProcessType processType) {
        ServiceMetadata serviceMetadata = ProcessTypeToServiceMetadataMapper.getServiceMetadata(processType);
        Map<String, Object> nonSensitiveVariables = StepsUtil.getNonSensitiveVariables(context, serviceMetadata);
        debug(context, MessageFormat.format(Messages.PROCESS_VARIABLES, JsonUtil.toJson(nonSensitiveVariables, true)), LOGGER);
    }

    private void addOngoingOperation(DelegateExecution context, String correlationId, ProcessType processType) {
        String startedAt = FORMATTER.format(ZonedDateTime.now());
        String user = StepsUtil.determineCurrentUser(context, LOGGER, processLoggerProviderFactory);
        String spaceId = StepsUtil.getSpaceId(context);
        OngoingOperation process = new OngoingOperation(correlationId, processType, startedAt, spaceId, null, user, false, null);
        ongoingOperationDao.add(process);
    }

}
