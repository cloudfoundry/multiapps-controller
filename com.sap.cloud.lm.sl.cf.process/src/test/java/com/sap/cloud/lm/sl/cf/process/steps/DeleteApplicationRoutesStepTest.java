package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.cloudfoundry.client.lib.domain.CloudApplication;

public class DeleteApplicationRoutesStepTest extends UndeployAppStepTest {

    public DeleteApplicationRoutesStepTest(String stepInputLocation, String stepOutputLocation) throws Exception {
        super(stepInputLocation, stepOutputLocation);
    }

    @Override
    protected void performValidation(CloudApplication cloudApplication) {
        if (!cloudApplication.getUris()
            .isEmpty()) {
            verify(client).updateApplicationUris(cloudApplication.getName(), Collections.emptyList());
        }
    }

    @Override
    protected void performAfterUndeploymentValidation() {
        assertRoutesWereDeleted();
    }

    private void assertRoutesWereDeleted() {
        int routesToDeleteCount = stepOutput.expectedRoutesToDelete.size();
        verify(client, times(routesToDeleteCount)).deleteRoute(anyString(), anyString());
        for (Route route : stepOutput.expectedRoutesToDelete) {
            verify(client).deleteRoute(route.host, route.domain);
            routesToDeleteCount--;
        }
        assertEquals("A number of routes were not deleted: ", 0, routesToDeleteCount);
    }

    @Override
    protected UndeployAppStep createStep() {
        return new DeleteApplicationRoutesStep(applicationRoutesGetter);
    }

}
