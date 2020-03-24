package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;

import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Named("servicesToDeleteVariableGetter")
public class ServicesToDeleteVariableGetter {

    public List<String> getServicesToDelete(DelegateExecution context) {
        return StepsUtil.getServicesToDelete(context);
    }
}
