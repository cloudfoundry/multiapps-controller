package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.verify;

import org.cloudfoundry.client.lib.domain.CloudApplication;

public class DeleteApplicationStepTest extends UndeployAppStepTest {

    public DeleteApplicationStepTest(String stepInputLocation, String stepOutputLocation) throws Exception {
        super(stepInputLocation, stepOutputLocation);
    }

    @Override
    protected void performValidation(CloudApplication cloudApplication) {
        verify(client).deleteApplication(cloudApplication.getName());
    }

    @Override
    protected UndeployAppStep createStep() {
        return new DeleteApplicationStep();
    }

    @Override
    protected void performAfterUndeploymentValidation() {
    }

}
