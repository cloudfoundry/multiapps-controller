package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolveBindingCredentialReferencesStepTest extends SyncFlowableStepTest<ResolveBindingCredentialReferencesStep> {

    private static final String REFERENCED_RESOURCE = "ztis-config";
    private static final String REFERENCED_SERVICE_INSTANCE = "ztis-config";
    private static final String REFERENCED_KEY = "ztis-key";

    @Test
    void testResolvesCredentialReferenceInBindingParameters() {
        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS,
                            Map.of("issuer",
                                   "{ds/" + REFERENCED_RESOURCE + "/service-keys." + REFERENCED_KEY + ".credentials.issuer}",
                                   "app-identifier", "my-app-identifier"));
        Mockito.when(client.getServiceKey(REFERENCED_SERVICE_INSTANCE, REFERENCED_KEY))
               .thenReturn(buildKey(REFERENCED_KEY, REFERENCED_SERVICE_INSTANCE, Map.of("issuer", "CN=the-issuer")));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        Map<String, Object> resolved = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);
        assertEquals("CN=the-issuer", resolved.get("issuer"));
        assertEquals("my-app-identifier", resolved.get("app-identifier"));
    }

    @Test
    void testNoOpWhenNoReferences() {
        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, Map.of("app-identifier", "my-app-identifier"));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        Map<String, Object> resolved = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);
        assertEquals("my-app-identifier", resolved.get("app-identifier"));
        Mockito.verify(client, Mockito.never())
               .getServiceKey(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testUsesReferencedResourceServiceNameForReadBack() {
        String referencedResourceName = "ztis-resource";
        String referencedServiceInstanceName = "custom-ztis-service-name";
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setId("test-id")
                                                              .setResources(List.of(Resource.createV3()
                                                                                            .setName(referencedResourceName)
                                                                                            .setParameters(Map.of(SupportedParameters.SERVICE_NAME,
                                                                                                                  referencedServiceInstanceName))));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, descriptor);
        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS,
                            Map.of("issuer",
                                   "{ds/" + referencedResourceName + "/service-keys." + REFERENCED_KEY + ".credentials.issuer}"));
        Mockito.when(client.getServiceKey(referencedServiceInstanceName, REFERENCED_KEY))
               .thenReturn(buildKey(REFERENCED_KEY, referencedServiceInstanceName, Map.of("issuer", "CN=the-issuer")));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        Map<String, Object> resolved = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);
        assertEquals("CN=the-issuer", resolved.get("issuer"));
    }

    private CloudServiceKey buildKey(String keyName, String serviceInstanceName, Map<String, Object> credentials) {
        return ImmutableCloudServiceKey.builder()
                                       .name(keyName)
                                       .credentials(credentials)
                                       .serviceInstance(ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name(serviceInstanceName)
                                                                                             .build())
                                       .build();
    }

    @Override
    protected ResolveBindingCredentialReferencesStep createStep() {
        return new ResolveBindingCredentialReferencesStep();
    }
}