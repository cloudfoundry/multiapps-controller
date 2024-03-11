package com.sap.cloud.lm.sl.cf.core.flowable;

public interface AdditionalProcessAction {

    void executeAdditionalProcessAction(String processInstanceId);

    String getApplicableActionId();

}
