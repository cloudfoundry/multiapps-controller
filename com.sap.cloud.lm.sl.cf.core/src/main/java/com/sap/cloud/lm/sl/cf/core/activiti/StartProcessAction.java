package com.sap.cloud.lm.sl.cf.core.activiti;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StartProcessAction extends ResumeProcessAction {

    public static final String ACTION_ID_START = "start";

    @Inject
    public StartProcessAction(FlowableFacade activitiFacade, List<AdditionalProcessAction> additionalProcessActions) {
        super(activitiFacade, additionalProcessActions);
    }

    @Override
    public String getActionId() {
        return ACTION_ID_START;
    }
}
