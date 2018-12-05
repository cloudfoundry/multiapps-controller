package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.common.util.TestUtil.JsonSerializationOptions;

@RunWith(Parameterized.class)
public class BuildCloudUndeployModelStepTest extends SyncFlowableStepTest<BuildCloudUndeployModelStep> {

    private static final String SPACE_ID = "sap";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) No previously deployed MTA:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(), "deployed-apps-01.json", new TreeSet<>(Arrays.asList("a", "b", "c")), null, "empty-list.json", "empty-list.json"),
                new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (1) There are obsolete modules:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(), "deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-03.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (2) All modules are obsolete:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(), "deployed-apps-05.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-04.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (3) There are obsolete services:
            {
                new StepInput("apps-to-deploy-01.json", Arrays.asList("s-1", "s-2"), "deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-05.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Arrays.asList("s-3", "s-4"), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (4) All services are obsolete:
            {
                new StepInput("apps-to-deploy-01.json", Arrays.asList("s-3", "s-4"), "deployed-apps-05.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-06.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Arrays.asList("s-1", "s-2"), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (5) There are renamed applications:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(),"deployed-apps-06.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-07.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("namespace.a", "namespace.b", "namespace.c"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (6) There are no obsolete services:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(), "deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-08.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (7) There are no obsolete modules:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(), "deployed-apps-07.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-09.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Collections.emptyList(), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (8) There are no obsolete published provided dependencies:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(),"deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-10.json", "empty-list.json", "empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (9) There are no obsolete subscriptions:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(),"deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-03.json", "subscriptions-to-create-01.json","empty-list.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "empty-list.json")),
            },
            // (10) There are obsolete subscriptions:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(),"deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-03.json", "subscriptions-to-create-01.json", "existing-subscriptions-01.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "subscriptions-to-delete-01.json")),
            },
            // (11) There are obsolete subscriptions:
            {
                new StepInput("apps-to-deploy-01.json", Collections.emptyList(),"deployed-apps-04.json", new TreeSet<>(Arrays.asList("a", "b", "c")), "deployed-mta-03.json", "empty-list.json",  "existing-subscriptions-01.json"),
                new StepOutput(Arrays.asList("d", "e"), Collections.emptyList(), new Expectation(Expectation.Type.RESOURCE, "subscriptions-to-delete-02.json")),
            },
// @formatter:on
        });
    }

    @Mock
    private ConfigurationSubscriptionDao dao;

    private StepInput input;
    private StepOutput output;

    private List<CloudApplicationExtended> deployedApps;
    private List<CloudApplicationExtended> appsToDeploy;
    private List<ConfigurationSubscription> subscriptionsToCreate;
    private List<ConfigurationSubscription> existingSubscriptions;
    private DeployedMta deployedMta;

    private List<CloudApplicationExtended> prepareApplicationServices(List<CloudApplicationExtended> appsToDeploy, List<String> services) {
        for (CloudApplicationExtended app : appsToDeploy) {
            app.setServices(services);
        }
        return appsToDeploy;
    }

    public BuildCloudUndeployModelStepTest(StepInput input, StepOutput output) {
        this.input = input;
        this.output = output;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareDao();
    }

    private void loadParameters() throws Exception {
        String appsToDeployString = TestUtil.getResourceAsString(input.appsToDeployLocation, getClass());
        String deployedAppsString = TestUtil.getResourceAsString(input.deployedAppsLocation, getClass());
        String subscriptionsToCreateString = TestUtil.getResourceAsString(input.subscriptionsToCreateLocation, getClass());
        String existingSubscriptionsString = TestUtil.getResourceAsString(input.existingSubscriptionsLocation, getClass());

        deployedApps = JsonUtil.fromJson(deployedAppsString, new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());
        appsToDeploy = JsonUtil.fromJson(appsToDeployString, new TypeToken<List<CloudApplicationExtended>>() {
        }.getType());
        appsToDeploy = prepareApplicationServices(appsToDeploy, input.services);
        subscriptionsToCreate = JsonUtil.fromJson(subscriptionsToCreateString, new TypeToken<List<ConfigurationSubscription>>() {
        }.getType());
        existingSubscriptions = JsonUtil.fromJson(existingSubscriptionsString, new TypeToken<List<ConfigurationSubscription>>() {
        }.getType());

        if (input.deployedMtaLocation != null) {
            String deployedMtaString = TestUtil.getResourceAsString(input.deployedMtaLocation, getClass());
            deployedMta = JsonUtil.fromJson(deployedMtaString, DeployedMta.class);
        }
    }

    private void prepareContext() {
        StepsUtil.setDeployedMta(context, deployedMta);
        StepsUtil.setDeployedApps(context, ListUtil.upcastUnmodifiable(deployedApps));
        StepsUtil.setAppsToDeploy(context, ListUtil.upcastUnmodifiable(appsToDeploy));
        StepsUtil.setSubscriptionsToCreate(context, subscriptionsToCreate);
        StepsUtil.setSpaceId(context, SPACE_ID);
        StepsUtil.setMtaModules(context, input.mtaModules);
    }

    private void prepareDao() {
        if (deployedMta != null) {
            when(dao.findAll(deployedMta.getMetadata()
                .getId(), null, SPACE_ID, null)).thenReturn(filter(existingSubscriptions));
        }
    }

    private List<ConfigurationSubscription> filter(List<ConfigurationSubscription> existingSubscriptions) {
        return existingSubscriptions.stream()
            .filter((subscription) -> SPACE_ID.equals(subscription.getSpaceId()))
            .collect(Collectors.toList());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(output.servicesToDelete, StepsUtil.getServicesToDelete(context));

        assertEquals(output.appsToUndeployNames, getNames(StepsUtil.getAppsToUndeploy(context)));

        TestUtil.test(() -> StepsUtil.getSubscriptionsToDelete(context), output.subscriptionsToDeleteExpectation, getClass(),
            new JsonSerializationOptions(true, false));
    }

    private List<String> getNames(List<CloudApplication> appsToUndeploy) {
        if (appsToUndeploy == null) {
            return null;
        }
        return appsToUndeploy.stream()
            .map((app) -> app.getName())
            .collect(Collectors.toList());
    }

    private static class StepInput {

        public String appsToDeployLocation;
        public List<String> services;
        public String deployedAppsLocation;
        public Set<String> mtaModules;
        public String subscriptionsToCreateLocation;
        public String deployedMtaLocation;
        public String existingSubscriptionsLocation;

        public StepInput(String appsToDeployLocation, List<String> services, String deployedAppsLocation, Set<String> mtaModules,
            String deployedMtaLocation, String subscriptionsToCreateLocation, String existingSubscriptionsLocation) {
            this.appsToDeployLocation = appsToDeployLocation;
            this.services = services;
            this.deployedAppsLocation = deployedAppsLocation;
            this.mtaModules = mtaModules;
            this.subscriptionsToCreateLocation = subscriptionsToCreateLocation;
            this.deployedMtaLocation = deployedMtaLocation;
            this.existingSubscriptionsLocation = existingSubscriptionsLocation;
        }

    }

    private static class StepOutput {

        public List<String> appsToUndeployNames;
        public List<String> servicesToDelete;
        public Expectation subscriptionsToDeleteExpectation;

        public StepOutput(List<String> appsToUndeployNames, List<String> servicesToDelete, Expectation subscriptionsToDeleteExpectation) {
            this.appsToUndeployNames = appsToUndeployNames;
            this.servicesToDelete = servicesToDelete;
            this.subscriptionsToDeleteExpectation = subscriptionsToDeleteExpectation;
        }

    }

    @Override
    protected BuildCloudUndeployModelStep createStep() {
        return new BuildCloudUndeployModelStep();
    }

}
