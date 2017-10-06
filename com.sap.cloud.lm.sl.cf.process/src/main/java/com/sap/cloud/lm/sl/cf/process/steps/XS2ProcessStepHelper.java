package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.message.Messages;
import com.sap.cloud.lm.sl.slp.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.slp.steps.SLProcessStepHelper;
import com.sap.cloud.lm.sl.slp.steps.StepIndexProvider;
import com.sap.cloud.lm.sl.slp.util.AbstractProcessComponentUtil;

public class XS2ProcessStepHelper extends SLProcessStepHelper {

    public XS2ProcessStepHelper(ProgressMessageService progressMessageService, ProcessLoggerProviderFactory processLoggerProviderFactory,
        StepIndexProvider stepIndexProvider) {
        super(progressMessageService, processLoggerProviderFactory, stepIndexProvider);
    }

    @Override
    protected void postExecuteStep(DelegateExecution context, ExecutionStatus status) {
        logDebug(context, MessageFormat.format(Messages.STEP_FINISHED, context.getCurrentActivityName()));

        AbstractProcessComponentUtil.appendLogs(context, processLoggerProviderFactory);
    }
}
