package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

@Component("collectBlueGreenSystemParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectBlueGreenSystemParametersStep extends CollectSystemParametersStep {

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        // Temporary routes should only be used for testing. If the user does not want to be asked for a confirmation,
        // then he does not want to test the new apps. If that is the case - temporary routes are not needed:
        boolean reserveTemporaryRoute = !(boolean) context.getVariable(Constants.PARAM_NO_CONFIRM);
        return executeStepInternal(context, reserveTemporaryRoute);
    }

}
