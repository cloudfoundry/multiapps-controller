package org.cloudfoundry.multiapps.controller.process.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExtractBatchedServicesStepTest extends SyncFlowableStepTest<ExtractBatchedServicesStep> {

    private List<Resource> batchOfResources;
    private List<CloudServiceInstanceExtended> servicesToBind;

    @Mock
    protected ServicesCloudModelBuilder servicesCloudModelBuilder;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
                Arguments.of(new ExtractBatchedServicesStepTest.StepInput("services-to-create-01.json","batches-to-process-01.json"),
                             new ExtractBatchedServicesStepTest.StepInput("services-to-create-02.json","batches-to-process-02.json"))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(ExtractBatchedServicesStepTest.StepInput input) {
        loadParameters(input);
        step.execute(execution);
        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.BATCH_TO_PROCESS),
                    new Tester.Expectation(Tester.Expectation.Type.JSON, input.batchToProcessLocation));
        tester.test(() -> context.getVariable(Variables.SERVICES_TO_BIND),
                    new Tester.Expectation(Tester.Expectation.Type.JSON, input.servicesToBindLocation));
        assertEquals(batchOfResources.size(), context.getVariable(Variables.SERVICES_TO_CREATE_COUNT));
    }

    private void loadParameters(StepInput input) {
        String batchToProcessString = TestUtil.getResourceAsString(input.batchToProcessLocation, getClass());
        batchOfResources = JsonUtil.fromJson(batchToProcessString, new TypeReference<>() {
        });

        String servicesToBindString = TestUtil.getResourceAsString(input.servicesToBindLocation, getClass());
        servicesToBind = JsonUtil.fromJson(servicesToBindString, new TypeReference<>() {
        });

        context.setVariable(Variables.BATCH_TO_PROCESS, batchOfResources);
        context.setVariable(Variables.SERVICES_TO_BIND, servicesToBind);
        when(servicesCloudModelBuilder.build(any())).thenReturn(servicesToBind);
    }

    @Override
    protected ExtractBatchedServicesStep createStep() {
        return new ExtractBatchedServicesStepMock();
    }

    protected static class StepInput {
        public final String servicesToBindLocation;
        public final String batchToProcessLocation;

        public StepInput(String servicesToBindLocation, String batchToProcessLocation) {
            this.servicesToBindLocation = servicesToBindLocation;
            this.batchToProcessLocation = batchToProcessLocation;
        }
    }

    private class ExtractBatchedServicesStepMock extends ExtractBatchedServicesStep {
        @Override
        protected ServicesCloudModelBuilder getServicesCloudModelBuilder(ProcessContext context) {
            return servicesCloudModelBuilder;
        }
    }
}