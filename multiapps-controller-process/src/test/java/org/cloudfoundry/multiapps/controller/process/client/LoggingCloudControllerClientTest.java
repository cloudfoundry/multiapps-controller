package org.cloudfoundry.multiapps.controller.process.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.ApplicationServicesUpdateCallback;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.core.security.serialization.DynamicSecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class LoggingCloudControllerClientTest {

    @Mock
    private CloudControllerClient delegate;
    @Mock
    private UserMessageLogger logger;
    @Mock
    private DynamicSecureSerialization dynamicSecureSerialization;

    private LoggingCloudControllerClient client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        client = new LoggingCloudControllerClient(delegate, logger, dynamicSecureSerialization);
    }

    @Test
    void testGetTargetIsAPureDelegateWithoutLogging() {
        client.getTarget();

        Mockito.verify(delegate)
               .getTarget();
        Mockito.verifyNoInteractions(logger);
    }

    @Test
    void testAddDomainLogsAndDelegates() {
        client.addDomain("example.com");

        Mockito.verify(logger)
               .debug(Messages.ADDING_DOMAIN_0, "example.com");
        Mockito.verify(delegate)
               .addDomain("example.com");
    }

    @Test
    void testBindServiceInstanceWithParametersUsesSecureSerialization() {
        Map<String, Object> parameters = Map.of("key", "secret-value");
        ApplicationServicesUpdateCallback callback = Mockito.mock(ApplicationServicesUpdateCallback.class);
        Mockito.when(dynamicSecureSerialization.toJson(parameters))
               .thenReturn("[redacted]");

        client.bindServiceInstance("binding", "app", "service", parameters, callback);

        Mockito.verify(dynamicSecureSerialization)
               .toJson(parameters);
        Mockito.verify(logger)
               .debug(Messages.BINDING_SERVICE_INSTANCE_0_TO_APPLICATION_1_WITH_PARAMETERS_2, "service", "app", "[redacted]");
        Mockito.verify(delegate)
               .bindServiceInstance("binding", "app", "service", parameters, callback);
    }

    @Test
    void testCreateServiceInstanceSerializesPayloadBeforeLogging() {
        CloudServiceInstance instance = Mockito.mock(CloudServiceInstance.class);
        Mockito.when(dynamicSecureSerialization.toJson(instance))
               .thenReturn("[redacted-instance]");

        client.createServiceInstance(instance);

        Mockito.verify(logger)
               .debug(Messages.CREATING_SERVICE_INSTANCE_0, "[redacted-instance]");
        Mockito.verify(delegate)
               .createServiceInstance(instance);
    }

    @Test
    void testCreateServiceBrokerSerializesPayloadBeforeLogging() {
        CloudServiceBroker broker = Mockito.mock(CloudServiceBroker.class);
        Mockito.when(dynamicSecureSerialization.toJson(broker))
               .thenReturn("[redacted-broker]");

        client.createServiceBroker(broker);

        Mockito.verify(logger)
               .debug(Messages.CREATING_SERVICE_BROKER_0, "[redacted-broker]");
        Mockito.verify(delegate)
               .createServiceBroker(broker);
    }

    @Test
    void testCreateServiceKeyByModelSerializesCredentialsNotKeyName() {
        CloudServiceKey keyModel = Mockito.mock(CloudServiceKey.class);
        Map<String, Object> credentials = Map.of("password", "shhh");
        Mockito.when(keyModel.getName())
               .thenReturn("my-key");
        Mockito.when(keyModel.getCredentials())
               .thenReturn(credentials);
        Mockito.when(dynamicSecureSerialization.toJson(credentials))
               .thenReturn("[redacted-credentials]");

        client.createServiceKey(keyModel, "my-service");

        Mockito.verify(dynamicSecureSerialization)
               .toJson(credentials);
        Mockito.verify(logger)
               .debug(Messages.CREATING_SERVICE_KEY_0_FOR_SERVICE_INSTANCE_1_WITH_PARAMETERS_2, "my-key", "my-service", "[redacted-credentials]");
    }

    @Test
    void testRunTaskSerializesTask() {
        CloudTask task = Mockito.mock(CloudTask.class);
        Mockito.when(dynamicSecureSerialization.toJson(task))
               .thenReturn("[redacted-task]");

        client.runTask("my-app", task);

        Mockito.verify(logger)
               .debug(Messages.RUNNING_TASK_1_ON_APPLICATION_0, "my-app", "[redacted-task]");
        Mockito.verify(delegate)
               .runTask("my-app", task);
    }

    @Test
    void testUpdateApplicationStagingSerializesStaging() {
        Staging staging = Mockito.mock(Staging.class);
        Mockito.when(dynamicSecureSerialization.toJson(staging))
               .thenReturn("[redacted-staging]");

        client.updateApplicationStaging("my-app", staging);

        Mockito.verify(logger)
               .debug(Messages.UPDATING_STAGING_OF_APPLICATION_0_TO_1, "my-app", "[redacted-staging]");
    }

    @Test
    void testUpdateApplicationMetadataSerializesMetadata() {
        UUID guid = UUID.randomUUID();
        Metadata metadata = Mockito.mock(Metadata.class);
        Mockito.when(dynamicSecureSerialization.toJson(metadata))
               .thenReturn("[redacted-metadata]");

        client.updateApplicationMetadata(guid, metadata);

        Mockito.verify(logger)
               .debug(Messages.UPDATING_METADATA_OF_APPLICATION_0_TO_1, guid, "[redacted-metadata]");
    }

    @Test
    void testUpdateServiceBrokerSerializesPayload() {
        CloudServiceBroker broker = Mockito.mock(CloudServiceBroker.class);
        Mockito.when(dynamicSecureSerialization.toJson(broker))
               .thenReturn("[redacted-broker]");
        Mockito.when(delegate.updateServiceBroker(broker))
               .thenReturn("job-1");

        Assertions.assertEquals("job-1", client.updateServiceBroker(broker));

        Mockito.verify(logger)
               .debug(Messages.UPDATING_SERVICE_BROKER_TO_0, "[redacted-broker]");
    }

    @Test
    void testGetApplicationByNameAndRequiredFlagDelegatesAndLogsSameMessageAsNoRequiredOverload() {
        CloudApplication app = Mockito.mock(CloudApplication.class);
        Mockito.when(delegate.getApplication("my-app", true))
               .thenReturn(app);

        Assertions.assertSame(app, client.getApplication("my-app", true));

        Mockito.verify(logger)
               .debug(Messages.GETTING_APPLICATION_0, "my-app");
        Mockito.verify(delegate)
               .getApplication("my-app", true);
    }

    @Test
    void testDeleteServiceBindingByGuidLogsBindingGuid() {
        UUID bindingGuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Mockito.when(delegate.deleteServiceBinding(bindingGuid))
               .thenReturn(Optional.of("job-2"));

        Assertions.assertEquals(Optional.of("job-2"), client.deleteServiceBinding(bindingGuid));

        Mockito.verify(logger)
               .debug(Messages.DELETING_SERVICE_BINDING_0, bindingGuid.toString());
    }

    @Test
    void testGetStackPassesRequiredFlagToDelegate() {
        CloudStack stack = Mockito.mock(CloudStack.class);
        Mockito.when(delegate.getStack("cflinuxfs4", true))
               .thenReturn(stack);

        Assertions.assertSame(stack, client.getStack("cflinuxfs4", true));

        Mockito.verify(logger)
               .debug(Messages.GETTING_STACK_0, "cflinuxfs4");
    }

    @Test
    void testRenameLogsBothNamesAndDelegates() {
        client.rename("old-name", "new-name");

        Mockito.verify(logger)
               .debug(Messages.RENAMING_APPLICATION_0_TO_1, "old-name", "new-name");
        Mockito.verify(delegate)
               .rename("old-name", "new-name");
    }
}
