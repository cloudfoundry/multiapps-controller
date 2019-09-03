package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.verify;

import org.cloudfoundry.client.lib.domain.CloudApplication;

public class StopApplicationUndeploymentStepTest extends UndeployAppStepTest {

    @Override
    protected void performValidation(CloudApplication cloudApplication) {
        verify(client).stopApplication(cloudApplication.getName());
    }

    @Override
    protected UndeployAppStep createStep() {
        return new StopApplicationUndeploymentStep();
    }

    @Override
    protected void performAfterUndeploymentValidation() {
    }

}
