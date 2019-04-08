package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Component("setUndeployPhase")
public class SetUndeployPhaseListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;
    
    @Override
    public void notify(DelegateExecution execution) {
        StepsUtil.setPhase(execution, Phase.UNDEPLOY);
    }

}
