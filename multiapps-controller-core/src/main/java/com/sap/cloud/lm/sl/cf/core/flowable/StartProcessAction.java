package com.sap.cloud.lm.sl.cf.core.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StartProcessAction extends ResumeProcessAction {

    public static final String ACTION_ID_START = "start";

    @Inject
    public StartProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions) {
        super(flowableFacade, additionalProcessActions);
    }

    @Override
    public String getActionId() {
        return ACTION_ID_START;
    }
}
