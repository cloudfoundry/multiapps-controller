package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateSubscribersStepTest extends SyncFlowableStepTest<UpdateSubscribersStep> {

    private static final String NO_USER_ROLES_DEFINED_FOR_ORG_AND_SPACE = "No user roles defined for org [{0}] and space [{1}]";
    private static final String USER = "XSMASTER";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) A subscriber should be updated, because there are new published entries (there are no existing entries):
            {
                "update-subscribers-step-input-00.json", "update-subscribers-step-output-00.json", 2, null,
            },
            // (1) A subscriber should be updated:
            {
                "update-subscribers-step-input-01.json", "update-subscribers-step-output-01.json", 2, null,
            },
            // (2) A subscriber should be updated, but the user does not have the necessary permissions for the org and space of the subscriber:
            {
                "update-subscribers-step-input-02.json", "update-subscribers-step-output-02.json", 2, null,
            },
            // (3) A subscriber should be updated, but the user does not have the necessary permissions for the org and space of the subscriber:
            {
                "update-subscribers-step-input-03.json", "update-subscribers-step-output-02.json", 2, null,
            },
            // (4) A subscriber should be updated, because there are deleted entries (there are no existing entries):
            {
                "update-subscribers-step-input-04.json", "update-subscribers-step-output-04.json", 2, null,
            },
            // (5) A subscriber should be updated, and there are additional environment variables that should be updated, other than the list variable:
            {
                "update-subscribers-step-input-05.json", "update-subscribers-step-output-05.json", 2, null,
            },
            // (6) A subscriber should be updated, because there are new published entries (there are no existing entries) and the subscriber requires just one entry:
            {
                "update-subscribers-step-input-06.json", "update-subscribers-step-output-06.json", 2, null,
            },
            // (7) There are multiple subscribers that should be updated:
            {
                "update-subscribers-step-input-07.json", "update-subscribers-step-output-07.json", 2, null,
            },
            // (8) One application has two subscriptions:
            {
                "update-subscribers-step-input-08.json", "update-subscribers-step-output-08.json", 2, null,
            },
            // (9) There's no need to update a subscriber:
            {
                "update-subscribers-step-input-09.json", "update-subscribers-step-output-09.json", 2, null,
            },
// @formatter:on
        });
    }

    @Mock
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationSubscriptionQuery configurationSubscriptionQuery;
    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationEntryQuery configurationEntryQuery;

    @Mock
    private CloudControllerClient clientForCurrentSpace;

    @Mock
    protected ModuleToDeployHelper moduleToDeployHelper;

    private final String expectedExceptionMessage;

    private final int majorSchemaVersion;
    private final String expectedOutputLocation;
    private StepOutput expectedOutput;
    private Map<CloudSpace, CloudControllerClient> clients;
    private final String inputLocation;
    private StepInput input;

    public UpdateSubscribersStepTest(String inputLocation, String expectedOutputLocation, int majorSchemaVersion,
                                     String expectedExceptionMessage) {
        this.expectedOutputLocation = expectedOutputLocation;
        this.majorSchemaVersion = majorSchemaVersion;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.inputLocation = inputLocation;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClients();
        prepareConfigurationServices();
    }

    private void loadParameters() {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }

        String outputString = TestUtil.getResourceAsString(expectedOutputLocation, getClass());
        expectedOutput = JsonUtil.fromJson(outputString, StepOutput.class);

        String inputString = TestUtil.getResourceAsString(inputLocation, getClass());
        input = JsonUtil.fromJson(inputString, StepInput.class);
    }

    private void prepareContext() {
        execution.setVariable(Constants.VAR_SPACE, input.currentSpace.getName());
        execution.setVariable(Constants.VAR_ORG, input.currentSpace.getOrganization()
                                                                   .getName());

        execution.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, majorSchemaVersion);
        execution.setVariable(Constants.PARAM_USE_NAMESPACES, false);
        execution.setVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);

        context.setVariable(Variables.PUBLISHED_ENTRIES, getPublishedEntries());
        context.setVariable(Variables.DELETED_ENTRIES, getDeletedEntries());

        execution.setVariable(Constants.VAR_USER, USER);
        step.targetCalculator = (client, spaceId) -> new CloudTarget(spaceId, spaceId);
        Mockito.when(flowableFacadeFacade.getHistoricSubProcessIds(Mockito.any()))
               .thenReturn(Collections.singletonList("test-subprocess-id"));
        HistoricVariableInstance varInstanceMock = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(flowableFacadeFacade.getHistoricVariableInstance("test-subprocess-id", Constants.VAR_PUBLISHED_ENTRIES))
               .thenReturn(varInstanceMock);
        Mockito.when(varInstanceMock.getValue())
               .thenReturn(getBytes(getPublishedEntries()));
        Mockito.when(moduleToDeployHelper.isApplication(any()))
               .thenReturn(true);
    }

    private byte[] getBytes(List<ConfigurationEntry> publishedEntries) {
        return JsonUtil.toJsonBinary(publishedEntries.toArray(new ConfigurationEntry[] {}));
    }

    private List<ConfigurationEntry> getDeletedEntries() {
        return input.subscribersToUpdate.stream()
                                        .flatMap((subscriber) -> subscriber.relevantDeletedEntries.stream())
                                        .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getPublishedEntries() {
        return input.subscribersToUpdate.stream()
                                        .flatMap((subscriber) -> subscriber.relevantPublishedEntries.stream())
                                        .collect(Collectors.toList());
    }

    private void prepareClients() throws Exception {
        prepareClientProvider(input.currentSpace, clientForCurrentSpace);
        clients = createClientsForSpacesOfSubscribedApps();
        for (CloudSpace space : clients.keySet()) {
            prepareClientProvider(space, clients.get(space));
        }
    }

    private void prepareClientProvider(CloudSpace space, CloudControllerClient clientMock) {
        String orgName = space.getOrganization()
                              .getName();
        String spaceName = space.getName();
        when(clientProvider.getControllerClient(eq(USER), eq(orgName), eq(spaceName), anyString())).thenReturn(clientMock);
    }

    private Map<CloudSpace, CloudControllerClient> createClientsForSpacesOfSubscribedApps() {
        Map<CloudSpace, CloudControllerClient> result = new HashMap<>();
        for (SubscriberToUpdate subscriber : input.subscribersToUpdate) {
            CloudControllerClient client = getOrCreateClientForSpace(result, subscriber.app.getSpace());
            mockClientInvocations(subscriber, client);
        }
        return result;
    }

    private CloudControllerClient getOrCreateClientForSpace(Map<CloudSpace, CloudControllerClient> clients, CloudSpace space) {
        for (CloudSpace existingSpace : clients.keySet()) {
            if (isSameSpace(space, existingSpace)) {
                return clients.get(existingSpace);
            }
        }
        clients.put(space, client);
        return client;
    }

    private boolean isSameSpace(CloudSpace space1, CloudSpace space2) {
        return space1.getName()
                     .equals(space2.getName())
            && space1.getOrganization()
                     .getName()
                     .equals(space2.getOrganization()
                                   .getName());
    }

    @SuppressWarnings("unchecked")
    private void mockClientInvocations(SubscriberToUpdate subscriber, CloudControllerClient client) {
        if (userHasPermissions(subscriber.app.getSpace(), UserPermission.READ)) {
            when(client.getApplication(subscriber.subscription.getAppName())).thenReturn(subscriber.app);
        } else {
            when(client.getApplication(subscriber.subscription.getAppName())).thenThrow(new CloudOperationException(HttpStatus.FORBIDDEN));
        }
        if (!userHasPermissions(subscriber.app.getSpace(), UserPermission.WRITE)) {
            doThrow(new CloudOperationException(HttpStatus.FORBIDDEN)).when(client)
                                                                      .updateApplicationEnv(eq(subscriber.subscription.getAppName()),
                                                                                            any(Map.class));
        }
    }

    private void prepareConfigurationServices() {
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        when(configurationSubscriptionService.createQuery()).thenReturn(configurationSubscriptionQuery);
        doReturn(getSubscriptions()).when(configurationSubscriptionQuery)
                                    .list();

        for (SubscriberToUpdate subscriber : input.subscribersToUpdate) {
            ConfigurationFilter filter = subscriber.subscription.getFilter();
            List<CloudTarget> targets = Collections.singletonList(new CloudTarget(input.currentSpace.getOrganization()
                                                                                                    .getName(),
                                                                                  input.currentSpace.getName()));
            ConfigurationEntryQuery entryQueryMock = new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(filter.getProviderNid()))
                                                                                               .on(query -> query.providerId(filter.getProviderId()))
                                                                                               .on(query -> query.version(filter.getProviderVersion()))
                                                                                               .on(query -> query.target(filter.getTargetSpace()))
                                                                                               .on(query -> query.requiredProperties(filter.getRequiredContent()))
                                                                                               .on(query -> query.visibilityTargets(targets))
                                                                                               .build();
            doReturn(getAllEntries(subscriber)).when(entryQueryMock)
                                               .list();
        }
    }

    private List<ConfigurationSubscription> getSubscriptions() {
        return input.subscribersToUpdate.stream()
                                        .map((subscriber) -> subscriber.subscription)
                                        .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getAllEntries(SubscriberToUpdate subscriber) {
        List<ConfigurationEntry> allEntries = new ArrayList<>();
        allEntries.addAll(subscriber.relevantExistingEntries);
        allEntries.addAll(subscriber.relevantPublishedEntries);
        return allEntries;
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        StepOutput actualOutput = captureStepOutput();
        assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));
    }

    private StepOutput captureStepOutput() {
        StepOutput result = new StepOutput();
        result.callArgumentsOfUpdateApplicationEnvMethod = new ArrayList<>();

        for (CloudSpace space : clients.keySet()) {
            if (userHasPermissions(space, UserPermission.READ, UserPermission.WRITE)) {
                List<CloudApplication> callArgumentsOfUpdateApplicationEnvMethod = getCallArgumentsOfUpdateApplicationEnvMethod(space,
                                                                                                                                clients.get(space));
                result.callArgumentsOfUpdateApplicationEnvMethod.addAll(callArgumentsOfUpdateApplicationEnvMethod);
            }
        }
        result.updatedSubscribers = context.getVariable(Variables.UPDATED_SUBSCRIBERS);
        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<CloudApplication> getCallArgumentsOfUpdateApplicationEnvMethod(CloudSpace space, CloudControllerClient client) {
        ArgumentCaptor<Map> appEnvCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> appNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, Mockito.atLeast(0)).updateApplicationEnv(appNameCaptor.capture(), appEnvCaptor.capture());

        List<Map> appEnvs = appEnvCaptor.getAllValues();
        List<String> appNames = appNameCaptor.getAllValues();
        List<CloudApplication> result = new ArrayList<>();
        for (int i = 0; i < appNames.size(); i++) {
            result.add(createApp(appNames.get(i), space, appEnvs.get(i)));
        }
        return result;
    }

    private boolean userHasPermissions(CloudSpace space, UserPermission... permissions) {
        UserRole userRole = getUserRole(space);
        if (userRole == null) {
            throw new IllegalStateException(MessageFormat.format(NO_USER_ROLES_DEFINED_FOR_ORG_AND_SPACE, space.getOrganization()
                                                                                                               .getName(),
                                                                 space.getName()));
        }
        return userRole.permissions.containsAll(Arrays.asList(permissions));
    }

    private UserRole getUserRole(CloudSpace space) {
        return input.userRoles.stream()
                              .filter((role) -> isSameSpace(role.space, space))
                              .findFirst()
                              .orElse(null);
    }

    private CloudApplication createApp(String name, CloudSpace space, Map<String, String> env) {
        return ImmutableCloudApplication.builder()
                                        .name(name)
                                        .space(space)
                                        .env(env)
                                        .build();
    }

    private static class SubscriberToUpdate {

        public List<ConfigurationEntry> relevantPublishedEntries;
        public ConfigurationSubscription subscription;
        public List<ConfigurationEntry> relevantExistingEntries;
        public CloudApplication app;
        public List<ConfigurationEntry> relevantDeletedEntries;

    }

    private enum UserPermission {

        READ, WRITE,

    }

    private static class UserRole {

        public CloudSpace space;
        public List<UserPermission> permissions;

    }

    private static class StepInput {

        public CloudSpace currentSpace;
        public List<SubscriberToUpdate> subscribersToUpdate;
        public List<UserRole> userRoles;

    }

    private static class StepOutput {

        public List<CloudApplication> callArgumentsOfUpdateApplicationEnvMethod;
        @SuppressWarnings("unused")
        public List<CloudApplication> updatedSubscribers;

    }

    private static class UpdateSubscribersStepMock extends UpdateSubscribersStep {

        @Override
        protected boolean shouldUsePrettyPrinting() {
            return false;
        }

    }

    @Override
    protected UpdateSubscribersStep createStep() {
        return new UpdateSubscribersStepMock();
    }

}
