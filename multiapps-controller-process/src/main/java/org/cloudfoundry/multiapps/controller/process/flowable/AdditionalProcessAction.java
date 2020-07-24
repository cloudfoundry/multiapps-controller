package org.cloudfoundry.multiapps.controller.process.flowable;

public interface AdditionalProcessAction {

    void executeAdditionalProcessAction(String processInstanceId);

    String getApplicableActionId();

}
