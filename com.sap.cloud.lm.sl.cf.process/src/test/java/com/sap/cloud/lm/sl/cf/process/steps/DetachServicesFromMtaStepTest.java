/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;

public class DetachServicesFromMtaStepTest extends SyncFlowableStepTest<DetachServicesFromMtaStep> {

    static Stream<List<SimpleServiceInstance>> testExecute() {
        List<SimpleServiceInstance> servicesToDetach = Arrays.asList(createSimpleServiceInstance("service-1"),
                                                                     createSimpleServiceInstance("service-2"));
        return Stream.of(servicesToDetach);
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(List<SimpleServiceInstance> servicesToDetach) {
        setUp(servicesToDetach);

        step.execute(context);
        assertStepFinishedSuccessfully();
        validateServicesDetached(servicesToDetach);
    }

    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    private void validateServicesDetached(List<SimpleServiceInstance> servicesToDetach) {
        for (SimpleServiceInstance serviceToDetach : servicesToDetach) {
            Mockito.verify(client)
                   .updateServiceMetadata(eq(serviceToDetach.guid), eq(getMetadataWithEmptyMtaFields()));
        }
    }

    private static Metadata getMetadataWithEmptyMtaFields() {
        return Metadata.builder()
                       .label(MtaMetadataLabels.MTA_ID, StringUtils.EMPTY)
                       .label(MtaMetadataLabels.MTA_VERSION, StringUtils.EMPTY)
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, StringUtils.EMPTY)
                       .build();
    }

    private void setUp(List<SimpleServiceInstance> servicesToDetach) {
        List<String> serviceNamesToDetach = servicesToDetach.stream()
                                                            .map(SimpleServiceInstance::getName)
                                                            .collect(Collectors.toList());
        prepareContext(serviceNamesToDetach);
        prepareClient(servicesToDetach);
    }

    private void prepareContext(List<String> servicesToDelete) {
        StepsUtil.setServicesToDelete(context, servicesToDelete);
    }

    private void prepareClient(List<SimpleServiceInstance> servicesToDelete) {
        for (SimpleServiceInstance serviceInstance : servicesToDelete) {
            Mockito.when(client.getServiceInstance(serviceInstance.name))
                   .thenReturn(createServiceInstance(serviceInstance));
        }
    }

    private CloudServiceInstance createServiceInstance(SimpleServiceInstance serviceInstance) {
        Metadata v3Metadata = Metadata.builder()
                                      .label(MtaMetadataLabels.MTA_ID, "test")
                                      .label(MtaMetadataLabels.MTA_VERSION, "1")
                                      .annotation(MtaMetadataAnnotations.MTA_RESOURCE, "test")
                                      .build();
        CloudMetadata metadata = ImmutableCloudMetadata.builder()
                                                       .guid(serviceInstance.guid)
                                                       .build();
        return ImmutableCloudServiceInstance.builder()
                                            .name(serviceInstance.name)
                                            .metadata(metadata)
                                            .v3Metadata(v3Metadata)
                                            .build();
    }

    private static SimpleServiceInstance createSimpleServiceInstance(String name) {
        return new SimpleServiceInstance(name, UUID.randomUUID());
    }

    private static class SimpleServiceInstance {
        String name;
        UUID guid;

        public SimpleServiceInstance(String name, UUID guid) {
            this.name = name;
            this.guid = guid;
        }

        public String getName() {
            return this.name;
        }
    }

    @Override
    protected DetachServicesFromMtaStep createStep() {
        return new DetachServicesFromMtaStep();
    }

}
