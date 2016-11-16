package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedComponents;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class DetectDeployedMtaStepTest extends AbstractStepTest<DetectDeployedMtaStep> {

    private static final String ORG_NAME = "initial";
    private static final String SPACE_NAME = "initial";

    private static final String USER = "XSMASTER";

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";
    private static final String DEPLOYED_MTA_LOCATION = "deployed-mta-01.json";

    @Mock
    private DeployedComponentsDetector componentsDetector;
    @Mock
    private CloudFoundryClientProvider clientProvider;
    @Mock
    private CloudFoundryOperations client;

    @Test(expected = SLException.class)
    public void testExecute1() throws Exception {
        when(client.getApplications()).thenReturn(Collections.emptyList());
        when(componentsDetector.detectAllDeployedComponents(Collections.emptyList())).thenThrow(new ParsingException("Error!"));

        step.execute(context);
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(client.getApplications()).thenThrow(new CloudFoundryException(HttpStatus.INTERNAL_SERVER_ERROR));

        step.execute(context);
    }

    @Test
    public void testExecute3() throws Exception {
        when(client.getApplications()).thenReturn(Collections.emptyList());

        DeployedMta deployedMta = JsonUtil.fromJson(TestUtil.getResourceAsString(DEPLOYED_MTA_LOCATION, getClass()), DeployedMta.class);
        DeployedComponents deployedComponents = new DeployedComponents(Arrays.asList(deployedMta), Collections.emptyList());

        when(componentsDetector.detectAllDeployedComponents(Collections.emptyList())).thenReturn(deployedComponents);

        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        TestUtil.test(() -> {

            return StepsUtil.getDeployedMta(context);

        } , TestUtil.RESOURCE_PREFIX + ":" + DEPLOYED_MTA_LOCATION, getClass());
    }

    @Test
    public void testExecute4() throws Exception {
        when(client.getApplications()).thenReturn(Collections.emptyList());
        when(componentsDetector.detectAllDeployedComponents(Collections.emptyList())).thenReturn(
            new DeployedComponents(Collections.emptyList(), Collections.emptyList()));

        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        assertNull(StepsUtil.getDeployedMta(context));
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        step.componentsDetector = (deployedApps) -> componentsDetector.detectAllDeployedComponents(deployedApps);
    }

    private void prepareContext() throws Exception {
        when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);

        context.setVariable(Constants.VAR_SPACE, SPACE_NAME);
        context.setVariable(Constants.VAR_ORG, ORG_NAME);

        context.setVariable(Constants.PARAM_MTA_ID, MTA_ID);

        context.setVariable(Constants.VAR_USER, USER);
    }

    @Override
    protected DetectDeployedMtaStep createStep() {
        return new DetectDeployedMtaStep();
    }

}
