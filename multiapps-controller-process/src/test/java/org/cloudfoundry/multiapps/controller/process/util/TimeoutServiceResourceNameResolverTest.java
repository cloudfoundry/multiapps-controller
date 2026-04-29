package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeoutServiceResourceNameResolverTest {

    private TimeoutServiceResourceNameResolver resolver;

    @Mock
    private ProcessContext context;

    @Mock
    private StepLogger logger;

    @BeforeEach
    void setUp() {
        try (var closeable = MockitoAnnotations.openMocks(this)) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        resolver = new TimeoutServiceResourceNameResolver();
    }

    @Test
    void testResolveResourceFromCloudServiceInstanceExtended() {
        String serviceName = "test-service";
        String resourceName = "test-resource";
        CloudServiceInstanceExtended service = ImmutableCloudServiceInstanceExtended.builder()
                                                                                    .name(serviceName)
                                                                                    .resourceName(resourceName)
                                                                                    .build();

        DeploymentDescriptor descriptor = createDescriptorWithResource(resourceName);

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(service);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertEquals(resourceName, result.getName());
    }

    @Test
    void testResolveResourceFromServiceName() {
        String serviceName = "test-service";
        String resourceName = "test-resource";
        CloudServiceInstanceExtended service = ImmutableCloudServiceInstanceExtended.builder()
                                                                                    .name(serviceName)
                                                                                    .build();

        DeploymentDescriptor descriptor = createDescriptorWithResource(resourceName);
        List<CloudServiceInstanceExtended> servicesToCreate = new ArrayList<>();
        servicesToCreate.add(ImmutableCloudServiceInstanceExtended.builder()
                                                                 .name(serviceName)
                                                                 .resourceName(resourceName)
                                                                 .build());

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(service);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(servicesToCreate);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertEquals(resourceName, result.getName());
    }

    @Test
    void testResolveResourceFromServicesToBind() {
        String serviceName = "test-service";
        String resourceName = "test-resource";
        DeploymentDescriptor descriptor = createDescriptorWithResource(resourceName);
        List<CloudServiceInstanceExtended> servicesToBind = new ArrayList<>();
        servicesToBind.add(ImmutableCloudServiceInstanceExtended.builder()
                                                               .name(serviceName)
                                                               .resourceName(resourceName)
                                                               .build());

        when(context.getVariableIfSet(Variables.SERVICE_TO_UNBIND_BIND)).thenReturn(serviceName);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(servicesToBind);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_POLL)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(null);

        Resource result = resolver.resolveResource(context, TimeoutType.BIND_SERVICE, descriptor, logger);

        assertNotNull(result);
        assertEquals(resourceName, result.getName());
    }

    @Test
    void testResolveResourceFromServicesToPoll() {
        String serviceName = "test-service";
        String resourceName = "test-resource";

        DeploymentDescriptor descriptor = createDescriptorWithResource(resourceName);
        List<CloudServiceInstanceExtended> servicesToPoll = new ArrayList<>();
        servicesToPoll.add(ImmutableCloudServiceInstanceExtended.builder()
                                                               .name(serviceName)
                                                               .resourceName(resourceName)
                                                               .build());

        CloudServiceInstanceExtended serviceToProcess = ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name(serviceName)
                                                                                             .build();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(serviceToProcess);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_POLL)).thenReturn(servicesToPoll);
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(null);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNotNull(result);
        assertEquals(resourceName, result.getName());
    }

    @Test
    void testResolveResourceFromDeployedMta() {
        String serviceName = "test-service";
        String resourceName = "test-resource";

        DeploymentDescriptor descriptor = createDescriptorWithResource(resourceName);
        
        DeployedMtaService deployedService = ImmutableDeployedMtaService.builder()
                                                                       .name(serviceName)
                                                                       .resourceName(resourceName)
                                                                       .build();
        DeployedMta deployedMta = ImmutableDeployedMta.builder()
                                                     .addServices(deployedService)
                                                     .metadata(ImmutableMtaMetadata.builder()
                                                                                   .id("test-mta")
                                                                                   .build())
                                                     .build();

        CloudServiceInstanceExtended serviceToProcess = ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name(serviceName)
                                                                                             .build();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(serviceToProcess);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_POLL)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(deployedMta);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNotNull(result);
        assertEquals(resourceName, result.getName());
    }

    @Test
    void testResolveResourceFromServiceKeyToProcess() {
        String serviceName = "test-service";
        String resourceName = "test-resource";

        DeploymentDescriptor descriptor = createDescriptorWithResource(resourceName);
        
        List<CloudServiceInstanceExtended> servicesToCreate = new ArrayList<>();
        servicesToCreate.add(ImmutableCloudServiceInstanceExtended.builder()
                                                                 .name(serviceName)
                                                                 .resourceName(resourceName)
                                                                 .build());

        CloudServiceInstanceExtended serviceInstance = ImmutableCloudServiceInstanceExtended.builder()
                                                                                           .name(serviceName)
                                                                                           .build();
        CloudServiceKey serviceKey = ImmutableCloudServiceKey.builder()
                                                            .name("test-key")
                                                            .serviceInstance(serviceInstance)
                                                            .build();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(servicesToCreate);
        when(context.getVariableIfSet(Variables.SERVICES_TO_POLL)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICE_KEY_TO_PROCESS)).thenReturn(serviceKey);
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(null);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNotNull(result);
        assertEquals(resourceName, result.getName());
    }

    @Test
    void testResolveResourceReturnsNullWhenResourceNotFound() {
        String serviceName = "test-service";
        String resourceName = "non-existent-resource";

        DeploymentDescriptor descriptor = createDescriptorWithResource("different-resource");
        CloudServiceInstanceExtended service = ImmutableCloudServiceInstanceExtended.builder()
                                                                                    .name(serviceName)
                                                                                    .resourceName(resourceName)
                                                                                    .build();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(service);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNull(result);
    }

    @Test
    void testResolveResourceReturnsNullWhenResourceNameNotResolved() {
        DeploymentDescriptor descriptor = createDescriptorWithResource("test-resource");

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_POLL)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(null);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNull(result);
    }

    @Test
    void testResolveResourceWithNullDescriptor() {
        CloudServiceInstanceExtended service = ImmutableCloudServiceInstanceExtended.builder()
                                                                                    .name("test-service")
                                                                                    .resourceName("test-resource")
                                                                                    .build();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(service);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, null, logger);

        assertNull(result);
    }

    @Test
    void testResolveResourceWithEmptyServicesList() {
        DeploymentDescriptor descriptor = createDescriptorWithResource("test-resource");
        List<CloudServiceInstanceExtended> emptyList = new ArrayList<>();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(emptyList);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNull(result);
    }

    @Test
    void testResolveResourceWithMultipleServices() {
        String targetServiceName = "target-service";
        String resourceName = "target-resource";
        DeploymentDescriptor descriptor = createDescriptorWithResources(
            "other-resource",
            resourceName,
            "another-resource"
        );

        List<CloudServiceInstanceExtended> servicesToCreate = new ArrayList<>();
        servicesToCreate.add(ImmutableCloudServiceInstanceExtended.builder()
                                                                 .name("other-service")
                                                                 .resourceName("other-resource")
                                                                 .build());
        servicesToCreate.add(ImmutableCloudServiceInstanceExtended.builder()
                                                                 .name(targetServiceName)
                                                                 .resourceName(resourceName)
                                                                 .build());

        CloudServiceInstanceExtended serviceToProcess = ImmutableCloudServiceInstanceExtended.builder()
                                                                                             .name(targetServiceName)
                                                                                             .build();

        when(context.getVariableIfSet(Variables.SERVICE_TO_PROCESS)).thenReturn(serviceToProcess);
        when(context.getVariableIfSet(Variables.SERVICES_TO_BIND)).thenReturn(null);
        when(context.getVariableIfSet(Variables.SERVICES_TO_CREATE)).thenReturn(servicesToCreate);
        when(context.getVariableIfSet(Variables.SERVICES_TO_POLL)).thenReturn(null);
        when(context.getVariable(Variables.DEPLOYED_MTA)).thenReturn(null);

        Resource result = resolver.resolveResource(context, TimeoutType.CREATE_SERVICE, descriptor, logger);

        assertNotNull(result);
        assertEquals(resourceName, result.getName());
    }

    private DeploymentDescriptor createDescriptorWithResource(String resourceName) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        List<Resource> resources = new ArrayList<>();
        Resource resource = mock(Resource.class);
        when(resource.getName()).thenReturn(resourceName);
        resources.add(resource);
        descriptor.setResources(resources);
        return descriptor;
    }

    private DeploymentDescriptor createDescriptorWithResources(String... resourceNames) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        List<Resource> resources = new ArrayList<>();
        for (String name : resourceNames) {
            Resource resource = mock(Resource.class);
            when(resource.getName()).thenReturn(name);
            resources.add(resource);
        }
        descriptor.setResources(resources);
        return descriptor;
    }

}








