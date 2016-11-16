package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.cts.CtsProcessExtensionsBuilder;
import com.sap.cloud.lm.sl.cts.CtsReturnCode;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.Constants;

public class CtsProcessExtensionsSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CtsProcessExtensionsSetter.class);

    private ProgressMessageService taskExtensionService;
    private Supplier<Date> timestampSupplier;

    public CtsProcessExtensionsSetter(ProgressMessageService taskExtensionService) {
        this(taskExtensionService, null);
    }

    public CtsProcessExtensionsSetter(ProgressMessageService taskExtensionService, Supplier<Date> timestampSupplier) {
        this.taskExtensionService = taskExtensionService;
        this.timestampSupplier = timestampSupplier;
    }

    public void set(DelegateExecution context) throws SLException {
        String logsPath = com.sap.cloud.lm.sl.cf.core.Constants.CTS_LOGS_ENDPOINT + StepsUtil.CTS_LOG_NAME;
        String ctsProcessId = (String) context.getVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_CTS_PROCESS_ID);
        CtsReturnCode ctsErrorCode = StepsUtil.getCtsReturnCode(context);

        CtsProcessExtensionsBuilder extensionsBuilder = new CtsProcessExtensionsBuilder();
        extensionsBuilder.ctsErrorCode(ctsErrorCode);
        extensionsBuilder.ctsProcessId(ctsProcessId);
        extensionsBuilder.logsPath(logsPath);
        Map<String, Object> processExtensions = extensionsBuilder.build();

        Date processStartTime = CommonUtil.getOrDefault(timestampSupplier, () -> getProcessStartTime(context)).get();
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_START_TIME, processStartTime));

        ProgressMessageType progressMessageType = ProgressMessageType.EXT;
        String progressMessageText = JsonUtil.toJson(processExtensions);
        String progressMessageTask = (String) context.getVariable(Constants.VARIABLE_NAME_SERVICE_ID);

// @formatter:off
        taskExtensionService.add(new ProgressMessage(
            context.getProcessInstanceId(),
            progressMessageTask,
            progressMessageType,
            progressMessageText,
            processStartTime)
            );
// @formatter:on
    }

    private Date getProcessStartTime(DelegateExecution context) {
        HistoryService historyService = context.getEngineServices().getHistoryService();
        return (historyService.createHistoricProcessInstanceQuery().processInstanceId(
            context.getProcessInstanceId()).singleResult().getStartTime());
    }

}
