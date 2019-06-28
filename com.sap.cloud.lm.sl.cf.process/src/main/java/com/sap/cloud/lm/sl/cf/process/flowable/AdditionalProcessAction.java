package com.sap.cloud.lm.sl.cf.process.flowable;

public interface AdditionalProcessAction {

    void executeAdditionalProcessAction(String processInstanceId);

    String getApplicableActionId();

}
