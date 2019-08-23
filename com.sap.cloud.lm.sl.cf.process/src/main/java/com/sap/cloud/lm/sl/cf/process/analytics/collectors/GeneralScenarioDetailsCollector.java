package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.analytics.model.GeneralProcess;
import com.sap.cloud.lm.sl.cf.process.analytics.model.GeneralScenarioDetails;

@Named("generalScenarioDetailsCollector")
public class GeneralScenarioDetailsCollector {
    private static final String LM_PROCESS = "XSA Deploy Service";
    private static final String MODEL_VERSION = "00001";

    @Inject
    private AnalyticsCollector analytics;

    public GeneralScenarioDetails collectDetails(DelegateExecution context, GeneralProcess generalProcess) {
        String processId = context.getProcessInstanceId();
        long startDate = analytics.getStartTime(processId);
        long endDate = analytics.getEndTime();

        return new GeneralScenarioDetails(LM_PROCESS, startDate, endDate, MODEL_VERSION, generalProcess);
    }
}
