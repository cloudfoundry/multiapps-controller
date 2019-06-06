package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class DetectDeployedMtaStepTest extends SyncFlowableStepTest<DetectDeployedMtaStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";
    private static final String DEPLOYED_MTA_LOCATION = "deployed-mta-01.json";

    @Mock
    private DeployedComponentsDetector componentsDetector;

    @Test(expected = SLException.class)
    public void testExecute1() throws Exception {
        when(client.getApplications()).thenReturn(Collections.emptyList());
        when(componentsDetector.getAllDeployedMta(client)).thenThrow(new ParsingException("Error!"));
        step.execute(context);
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(client.getApplications(false)).thenThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));

        step.execute(context);
    }

    @Test
    public void testExecute3() throws Exception {
        when(client.getApplications()).thenReturn(Collections.emptyList());

        DeployedMta deployedMta = JsonUtil.fromJson(TestUtil.getResourceAsString(DEPLOYED_MTA_LOCATION, getClass()), DeployedMta.class);
        List<DeployedMta> deployedComponents = Arrays.asList(deployedMta);

        when(componentsDetector.getAllDeployedMta(client)).thenReturn(Optional.of(deployedComponents));

        step.execute(context);

        assertStepFinishedSuccessfully();

        tester.test(() -> StepsUtil.getDeployedMta(context), new Expectation(Expectation.Type.JSON, DEPLOYED_MTA_LOCATION));
    }

    @Test
    public void testExecute4() throws Exception {
        when(client.getApplications()).thenReturn(Collections.emptyList());
        when(componentsDetector.getAllDeployedMta(client))
            .thenReturn(Optional.of(Collections.emptyList()));

        step.execute(context);

        assertStepFinishedSuccessfully();

        assertNull(StepsUtil.getDeployedMta(context));
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() throws Exception {
        context.setVariable(Constants.PARAM_MTA_ID, MTA_ID);
    }

    @Override
    protected DetectDeployedMtaStep createStep() {
        return new DetectDeployedMtaStep();
    }

}
