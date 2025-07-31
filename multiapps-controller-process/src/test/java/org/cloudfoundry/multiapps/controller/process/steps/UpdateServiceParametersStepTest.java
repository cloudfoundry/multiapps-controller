package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class UpdateServiceParametersStepTest extends SyncFlowableStepTest<UpdateServiceParametersStep> {

    @Test
    void testUpdateServiceParameters() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        step.execute(execution);

        verify(client).updateServiceParameters(serviceToProcess.getName(), serviceToProcess.getCredentials());
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
    void testUpdateServiceParametersWithConflictStatusCodeFromController() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        throwExceptionOnServiceParametersUpdate(HttpStatus.CONFLICT);

        assertDoesNotThrow(() -> step.execute(execution));
        assertTrue(context.getVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS));
        assertEquals(context.getVariable(Variables.SERVICE_WITH_BIND_IN_PROGRESS), serviceToProcess.getName());
    }

    @Test
    void testUpdateServiceParametersWithConflictStatusCodeFromConatroller() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        throwExceptionOnServiceParametersUpdate(HttpStatus.CONFLICT);

        assertDoesNotThrow(() -> step.execute(execution));
        assertFalse(context.getVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS));
        assertNull(context.getVariable(Variables.SERVICE_WITH_BIND_IN_PROGRESS));
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

    @Test
    void testFailOnParametersSetToFalse() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        serviceToProcess = ImmutableCloudServiceInstanceExtended.copyOf(serviceToProcess)
                                                                .withShouldFailOnParametersUpdateFailure(false);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceParametersUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        step.execute(execution);

        assertStepFinishedSuccessfully();
    }

    private void throwExceptionOnServiceParametersUpdate(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred"))
               .when(client)
               .updateServiceParameters(any(), any());
    }

    private CloudOperationException createConflictCfException() {
        return new CloudOperationException(HttpStatus.CONFLICT);
    }

    @Override
    protected UpdateServiceParametersStep createStep() {
        return new UpdateServiceParametersStep();
    }
}
