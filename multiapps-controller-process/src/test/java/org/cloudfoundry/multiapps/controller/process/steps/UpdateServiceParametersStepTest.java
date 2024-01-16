package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class UpdateServiceParametersStepTest extends SyncFlowableStepTest<UpdateServiceParametersStep> {

    @Test
    void testUpdateServiceParameters() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        step.execute(execution);

        verify(client).updateServiceParameters(serviceToProcess.getName(),
                serviceToProcess.getCredentials());
    }

    @Test
    void testExceptionIsThrownOnMandatoryServiceParametersUpdateBadGateway() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceParametersUpdate(HttpStatus.BAD_GATEWAY);

        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testExceptionIsThrownOnMandatoryServiceParametersUpdateInternalServerError() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceParametersUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThrows(SLException.class, () -> step.execute(execution));
        assertExecutionStepStatus(RETRY_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsNotThrownOnOptionalServiceParametersUpdateBadGateway() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceParametersUpdate(HttpStatus.BAD_GATEWAY);

        step.execute(execution);

        assertExecutionStepStatus(DONE_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsNotThrownOnOptionalServiceParametersUpdateInterServerError() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceParametersUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        step.execute(execution);

        assertExecutionStepStatus(DONE_STEP_EXECUTION_STATUS);
    }

    @Test
    void testOperationType() {
        assertEquals(ServiceOperation.Type.UPDATE, step.getOperationType());
    }

    private void throwExceptionOnServiceParametersUpdate(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred"))
                .when(client)
                .updateServiceParameters(any(), any());
    }

    @Override
    protected UpdateServiceParametersStep createStep() {
        return new UpdateServiceParametersStep();
    }
}
