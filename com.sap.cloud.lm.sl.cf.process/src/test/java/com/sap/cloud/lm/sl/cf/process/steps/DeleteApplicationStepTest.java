package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.process.util.HooksExecutor;
import com.sap.cloud.lm.sl.cf.process.util.HooksPhaseGetter;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

public class DeleteApplicationStepTest extends UndeployAppStepTest {

    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;
    @Mock
    private HooksExecutor hooksExecutor;
    @Mock
    private ProcessTypeParser processTypeParser;

    @BeforeEach
    void setUp() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
    }

    @Test
    public void testApplicationNotFoundExceptionThrown() {
        Mockito.doThrow(new CloudOperationException(HttpStatus.NOT_FOUND))
               .when(client)
               .deleteApplication(anyString());
        context.setVariable(Variables.APP_TO_PROCESS, createCloudApplication("test-app"));
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    @Test
    public void testBadGatewayExceptionThrown() {
        Mockito.doThrow(new CloudOperationException(HttpStatus.BAD_GATEWAY))
               .when(client)
               .deleteApplication(anyString());
        context.setVariable(Variables.APP_TO_PROCESS, createCloudApplication("test-app"));
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
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

    private CloudApplicationExtended createCloudApplication(String name) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(name)
                                                .build();
    }

}
