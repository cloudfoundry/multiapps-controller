package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

@Component("collectBlueGreenSystemParametersStep")
public class CollectBlueGreenSystemParametersStep extends CollectSystemParametersStep {

    @Override
    protected ExecutionStatus executeStep(DelegateExecution context) throws SLException {
        // Temporary routes should only be used for testing. If the user does not want to be asked for a confirmation,
        // then he does not want to test the new apps. If that is the case - temporary routes are not needed:
        boolean reserveTemporaryRoute = !ContextUtil.getVariable(context, Constants.PARAM_NO_CONFIRM, false);
        return executeStepInternal(context, reserveTemporaryRoute);
    }

}
