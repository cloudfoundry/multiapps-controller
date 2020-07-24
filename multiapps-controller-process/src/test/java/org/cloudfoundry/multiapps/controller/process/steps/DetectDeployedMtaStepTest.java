package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DetectDeployedMtaStepTest extends SyncFlowableStepTest<DetectDeployedMtaStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";
    private static final String DEPLOYED_MTA_LOCATION = "deployed-mta-01.json";

    @Mock
    private DeployedMtaDetector deployedMtaDetector;

    @Test
    public void testExecute3() {
        when(client.getApplications()).thenReturn(Collections.emptyList());

        DeployedMta deployedMta = JsonUtil.fromJson(TestUtil.getResourceAsString(DEPLOYED_MTA_LOCATION, getClass()), DeployedMta.class);
        List<DeployedMta> deployedComponents = Arrays.asList(deployedMta);

        when(deployedMtaDetector.detectDeployedMtas(Mockito.any(CloudControllerClient.class))).thenReturn(deployedComponents);
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(Mockito.eq(MTA_ID), Mockito.eq(null), Mockito
                                                                                                                  .any(CloudControllerClient.class),
                                                                     Mockito.eq(true))).thenReturn(Optional.of(deployedMta));

        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYED_MTA), new Expectation(Expectation.Type.JSON, DEPLOYED_MTA_LOCATION));
    }

    @Test
    public void testExecute4() {
        when(client.getApplications()).thenReturn(Collections.emptyList());
        when(deployedMtaDetector.detectDeployedMtas(client)).thenReturn(Collections.emptyList());
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(MTA_ID, null, client, false)).thenReturn(Optional.empty());
        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertNull(context.getVariable(Variables.DEPLOYED_MTA));
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() {
        context.setVariable(Variables.MTA_ID, MTA_ID);
    }

    @Override
    protected DetectDeployedMtaStep createStep() {
        return new DetectDeployedMtaStep();
    }

}
