package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.printingAssertEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateSubscribersStepTest extends AbstractStepTest<UpdateSubscribersStep> {

    private static final String NO_USER_ROLES_DEFINED_FOR_ORG_AND_SPACE = "No user roles defined for org [{0}] and space [{1}]";
    private static final String USER = "XSMASTER";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) A subscriber should be updated, because there are new published entries (there are no existing entries):
            {
                "update-subscribers-step-input-00.json", "update-subscribers-step-output-00.json", 2, null,
            },
            // (1) A subscriber should be updated, because there are new published entries (there are no existing entries):
            {
                "update-subscribers-step-input-00.json", "update-subscribers-step-output-00.json", 1, null,
            },
            // (2) A subscriber should be updated:
            {
                "update-subscribers-step-input-01.json", "update-subscribers-step-output-01.json", 2, null,
            },
            // (3) A subscriber should be updated, but the user does not have the necessary permissions for the org and space of the subscriber:
            {
                "update-subscribers-step-input-02.json", "update-subscribers-step-output-02.json", 2, null,
            },
            // (4) A subscriber should be updated, but the user does not have the necessary permissions for the org and space of the subscriber:
            {
                "update-subscribers-step-input-03.json", "update-subscribers-step-output-02.json", 2, null,
            },
            // (5) A subscriber should be updated, because there are deleted entries (there are no existing entries):
            {
                "update-subscribers-step-input-04.json", "update-subscribers-step-output-04.json", 2, null,
            },
            // (6) A subscriber should be updated, and there are additional environment variables that should be updated, other than the list variable:
            {
                "update-subscribers-step-input-05.json", "update-subscribers-step-output-05.json", 2, null,
            },
            // (7) A subscriber should be updated, because there are new published entries (there are no existing entries) and the subscriber requires just one entry:
            {
                "update-subscribers-step-input-06.json", "update-subscribers-step-output-06.json", 2, null,
            },
// @formatter:on
        });
    }

    @Mock
    private ConfigurationSubscriptionDao subscriptionsDao;
    @Mock
    private ConfigurationEntryDao entriesDao;

    @Mock
    private CloudFoundryOperations clientForCurrentSpace;
    @Mock
    private CloudFoundryClientProvider clientProvider;

    private String expectedExceptionMessage;

    private int majorSchemaVersion;
    private String expectedOutputLocation;
    private StepOutput expectedOutput;
    private Map<CloudLocation, CloudFoundryOperations> clients;
    private String inputLocation;
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
        prepareDaos();
    }

    private void loadParameters() throws Exception {
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
        }

        String outputString = TestUtil.getResourceAsString(expectedOutputLocation, getClass());
        expectedOutput = JsonUtil.fromJson(outputString, StepOutput.class);

        String inputString = TestUtil.getResourceAsString(inputLocation, getClass());
        input = JsonUtil.fromJson(inputString, StepInput.class);
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_SPACE, input.currentLocation.space);
        context.setVariable(Constants.VAR_ORG, input.currentLocation.org);

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, majorSchemaVersion);

        StepsUtil.setPublishedEntries(context, getPublishedEntries());
        StepsUtil.setDeletedEntries(context, getDeletedEntries());

        context.setVariable(Constants.VAR_USER, USER);
    }

    private void prepareClients() throws Exception {
        prepareClient(input.currentLocation, clientForCurrentSpace);
        clients = createClients();
        for (CloudLocation location : clients.keySet()) {
            prepareClient(location, clients.get(location));
        }
    }

    private void prepareClient(CloudLocation location, CloudFoundryOperations clientMock) throws Exception {
        when(clientProvider.getCloudFoundryClient(eq(USER), eq(location.org), eq(location.space), anyString())).thenReturn((clientMock));
    }

    private Map<CloudLocation, CloudFoundryOperations> createClients() {
        Map<CloudLocation, CloudFoundryOperations> result = new HashMap<>();
        for (SubscriberToUpdate subscriber : input.subscribersToUpdate) {
            result.put(subscriber.location, createClient(subscriber));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private CloudFoundryOperations createClient(SubscriberToUpdate subscriber) {
        CloudFoundryOperations client = mock(CloudFoundryOperations.class);
        if (userHasPermissions(subscriber.location, UserPermission.READ)) {
            when(client.getApplication(subscriber.subscription.getAppName())).thenReturn(subscriber.app);
        } else {
            when(client.getApplication(subscriber.subscription.getAppName())).thenThrow(new CloudFoundryException(HttpStatus.FORBIDDEN));
        }
        if (!userHasPermissions(subscriber.location, UserPermission.WRITE)) {
            doThrow(new CloudFoundryException(HttpStatus.FORBIDDEN)).when(client).updateApplicationEnv(
                eq(subscriber.subscription.getAppName()), any(Map.class));
        }
        return client;
    }

    private void prepareDaos() {
        when(subscriptionsDao.findAll(any())).thenReturn(getSubscriptions());

        for (SubscriberToUpdate subscriber : input.subscribersToUpdate) {
            ConfigurationFilter filter = subscriber.subscription.getFilter();
            when(entriesDao.find(filter.getProviderNid(), filter.getProviderId(), filter.getProviderVersion(), filter.getTargetSpace(),
                filter.getRequiredContent(), null)).thenReturn(getAllEntries(subscriber));
        }
    }

    private List<ConfigurationEntry> getDeletedEntries() {
        return input.subscribersToUpdate.stream().flatMap((subscriber) -> subscriber.relevantDeletedEntries.stream()).collect(
            Collectors.toList());
    }

    private List<ConfigurationEntry> getPublishedEntries() {
        return input.subscribersToUpdate.stream().flatMap((subscriber) -> subscriber.relevantPublishedEntries.stream()).collect(
            Collectors.toList());
    }

    private List<ConfigurationSubscription> getSubscriptions() {
        return input.subscribersToUpdate.stream().map((subscriber) -> subscriber.subscription).collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getAllEntries(SubscriberToUpdate subscriber) {
        List<ConfigurationEntry> allEntries = new ArrayList<>();
        allEntries.addAll(subscriber.relevantExistinggEntries);
        allEntries.addAll(subscriber.relevantPublishedEntries);
        return allEntries;
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        StepOutput actualOutput = captureStepOutput();
        printingAssertEquals(JsonUtil.toJson(expectedOutput, true, false, true, false),
            JsonUtil.toJson(actualOutput, true, false, true, false));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private StepOutput captureStepOutput() {
        StepOutput result = new StepOutput();
        result.expectedUpdatedApps = new HashMap<>();

        ArgumentCaptor<Map> appEnvCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> appNameCaptor = ArgumentCaptor.forClass(String.class);

        for (CloudLocation location : clients.keySet()) {
            if (userHasPermissions(location, UserPermission.READ, UserPermission.WRITE)) {
                CloudFoundryOperations client = clients.get(location);
                verify(client).updateApplicationEnv(appNameCaptor.capture(), appEnvCaptor.capture());
                Map<Object, Object> appEnv = (Map<Object, Object>) appEnvCaptor.getValue();
                String appName = appNameCaptor.getValue();
                result.expectedUpdatedApps.put(location.space, createApp(appName, appEnv));
                verify(client).stopApplication(appName);
                if (client instanceof ClientExtensions) {
                    verify((ClientExtensions) client).startApplication(appName, false);
                } else {
                    verify(client).startApplication(appName);
                }
            }
        }
        return result;
    }

    private boolean userHasPermissions(CloudLocation location, UserPermission... permissions) {
        UserRole userRole = getUserRole(location);
        if (userRole == null) {
            throw new IllegalStateException(MessageFormat.format(NO_USER_ROLES_DEFINED_FOR_ORG_AND_SPACE, location.org, location.space));
        }
        return userRole.permissions.containsAll(Arrays.asList(permissions));
    }

    private UserRole getUserRole(CloudLocation location) {
        return input.userRoles.stream().filter((role) -> role.location.equals(location)).findFirst().orElse(null);
    }

    private CloudApplication createApp(String name, Map<Object, Object> env) {
        CloudApplication app = new CloudApplication(null, name);
        app.setEnv(env);
        return app;
    }

    private static class SubscriberToUpdate {

        public CloudLocation location;
        public List<ConfigurationEntry> relevantPublishedEntries;
        public ConfigurationSubscription subscription;
        public List<ConfigurationEntry> relevantExistinggEntries;
        public CloudApplication app;
        public List<ConfigurationEntry> relevantDeletedEntries;

    }

    private static class CloudLocation {

        public String org;
        public String space;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((org == null) ? 0 : org.hashCode());
            result = prime * result + ((space == null) ? 0 : space.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null) {
                return false;
            }
            if (getClass() != other.getClass()) {
                return false;
            }
            CloudLocation otherLocation = (CloudLocation) other;
            return Objects.equals(org, otherLocation.org) && Objects.equals(space, otherLocation.space);
        }

    }

    private static enum UserPermission {

        READ, WRITE,

    }

    private static class UserRole {

        public CloudLocation location;
        public List<UserPermission> permissions;

    }

    private static class StepInput {

        public CloudLocation currentLocation;
        public List<SubscriberToUpdate> subscribersToUpdate;
        public List<UserRole> userRoles;

    }

    private static class StepOutput {

        public Map<String, CloudApplication> expectedUpdatedApps;

    }

    private static class UpdateSubscribersStepMock extends UpdateSubscribersStep {

        @Override
        protected Pair<String, String> computeOrgAndSpace(String spaceId, CloudFoundryOperations client) {
            return new Pair<>(spaceId, spaceId);
        }

        @Override
        protected boolean shouldUsePretttyPrinting() {
            return false;
        }

    }

    @Override
    protected UpdateSubscribersStep createStep() {
        return new UpdateSubscribersStepMock();
    }

}
