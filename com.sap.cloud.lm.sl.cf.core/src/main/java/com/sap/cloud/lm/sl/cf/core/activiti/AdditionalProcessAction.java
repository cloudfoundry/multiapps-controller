package com.sap.cloud.lm.sl.cf.core.activiti;

public interface AdditionalProcessAction {

    void executeAdditionalProcessAction(String processInstanceId);

    String getApplicableActionId();

}
