package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
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

class UpdateServiceMetadataStepTest extends SyncFlowableStepTest<UpdateServiceMetadataStep> {

    @Test
    void testUpdateServiceMetadata() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        step.execute(execution);

        verify(client).updateServiceInstanceMetadata(serviceToProcess.getMetadata()
                                                                     .getGuid(), serviceToProcess.getV3Metadata());
    }

    @Test
    void testExceptionIsThrownOnMandatoryServiceMetadataUpdateBadGateway() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceMetadataUpdate(HttpStatus.BAD_GATEWAY);

        assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testExceptionIsThrownOnMandatoryServiceMetadataUpdateInternalServerError() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(FALSE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceMetadataUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThrows(SLException.class, () -> step.execute(execution));
        assertExecutionStepStatus(RETRY_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsNotThrownOnOptionalServiceMetadataUpdateBadGateway() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceMetadataUpdate(HttpStatus.BAD_GATEWAY);

        step.execute(execution);

        assertExecutionStepStatus(DONE_STEP_EXECUTION_STATUS);
    }

    @Test
    void testExceptionIsNotThrownOnOptionalServiceMetadataUpdateInternalServerError() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess(TRUE);
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        throwExceptionOnServiceMetadataUpdate(HttpStatus.INTERNAL_SERVER_ERROR);

        step.execute(execution);

        assertExecutionStepStatus(DONE_STEP_EXECUTION_STATUS);
    }

    @Test
    void testOperationType() {
        assertEquals(ServiceOperation.Type.UPDATE, step.getOperationType());
    }

    private void throwExceptionOnServiceMetadataUpdate(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred"))
               .when(client)
               .updateServiceInstanceMetadata(any(), any());
    }

    @Override
    protected UpdateServiceMetadataStep createStep() {
        return new UpdateServiceMetadataStep();
    }

}
