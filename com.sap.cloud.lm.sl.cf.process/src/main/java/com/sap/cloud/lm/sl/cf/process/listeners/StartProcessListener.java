package com.sap.cloud.lm.sl.cf.process.listeners;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.listener.AbstractSLProcessExecutionListener;

@Component("startProcessListener")
public class StartProcessListener extends AbstractSLProcessExecutionListener {

    private static final long serialVersionUID = -447062578903384602L;

    private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessListener.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    @Override
    protected void notifyInternal(DelegateExecution context) throws SLException {
        String processId = context.getProcessInstanceId();
        ProcessType processType = getProcessType((String) context.getVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SERVICE_ID));
        if (processType.equals(ProcessType.CTS_DEPLOY)) {
            StepsUtil.initCtsLog(context, processLoggerProviderFactory);
        }
        String startedAt = FORMATTER.format(ZonedDateTime.now());
        String user = StepsUtil.determineCurrentUser(context, LOGGER, processLoggerProviderFactory);
        String spaceId = StepsUtil.getSpaceId(context);
        OngoingOperation process = new OngoingOperation(processId, processType, startedAt, spaceId, null, user, false, null);
        ongoingOperationDao.add(process);
    }

    private ProcessType getProcessType(String serviceId) throws SLException {
        switch (serviceId) {
            case Constants.CTS_DEPLOY_SERVICE_ID:
                return ProcessType.CTS_DEPLOY;
            case Constants.UNDEPLOY_SERVICE_ID:
                return ProcessType.UNDEPLOY;
            case Constants.DEPLOY_SERVICE_ID:
                return ProcessType.DEPLOY;
            case Constants.BLUE_GREEN_DEPLOY_SERVICE_ID:
                return ProcessType.BLUE_GREEN_DEPLOY;
            default:
                throw new SLException(Messages.UNKNOWN_SERVICE_ID, serviceId);
        }
    }

}
