package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.google.gson.reflect.TypeToken;
import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ArgumentMatcherProvider;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;

@RunWith(Parameterized.class)
public class PublishProvidedDependenciesStepTest extends AbstractStepTest<PublishProvidedDependenciesStep> {

    private static class StepInput {
        List<ProvidedDependency> providedDependencies;
        String mtaId;
        String newMtaVersion;
        String targetSpace;
        String org;
        String space;
        List<String> expectedAddedDependencies;
    }

    private static List<ConfigurationEntry> exisitingConfigurationEntries;

    private StepInput input;
    private ConfigurationEntryDao configurationEntryDaoMock = Mockito.mock(ConfigurationEntryDao.class);

    public PublishProvidedDependenciesStepTest(String input) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, PublishProvidedDependenciesStepTest.class), StepInput.class);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "publish-provided-dependencies-1.json"
            },
            {
                "publish-provided-dependencies-2.json"
            },
            {
                "publish-provided-dependencies-3.json"
            },
// @formatter:on
        });
    }

    @BeforeClass
    public static void loadConfigurationEntries() throws Exception {
        exisitingConfigurationEntries = JsonUtil.fromJson(
            TestUtil.getResourceAsString("configuration-entries.json", PublishProvidedDependenciesStepTest.class),
            new TypeToken<List<ConfigurationEntry>>() {
            }.getType());
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareDao();
        step.configurationEntryDao = configurationEntryDaoMock;
    }

    public void prepareDao() throws Exception {
        for (ConfigurationEntry entry : exisitingConfigurationEntries) {
            Mockito.when(configurationEntryDaoMock.find(Mockito.matches(entry.getProviderNid()), Mockito.matches(entry.getProviderId()),
                Mockito.matches(entry.getProviderVersion().toString()), Mockito.matches(entry.getTargetSpace()), Mockito.any(), Mockito.eq(null))).thenReturn(
                    Arrays.asList(entry));
        }
    }

    private void prepareContext() {
        StepsUtil.setDependenciesToPublish(context, input.providedDependencies);
        context.setVariable(Constants.PARAM_MTA_ID, input.mtaId);
        context.setVariable(Constants.VAR_ORG, input.org);
        context.setVariable(Constants.VAR_SPACE, input.space);
        StepsUtil.setNewMtaVersion(context, input.newMtaVersion);
    }

    @Test
    public void test() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        validateConfigurationEntryDao();
    }

    private void validateConfigurationEntryDao() throws Exception {
        for (ProvidedDependency dependency : input.providedDependencies) {
            ArgumentMatcher<ConfigurationEntry> matcher = ArgumentMatcherProvider.getConfigurationEntryMatcher(
                ConfigurationEntriesUtil.computeProviderId(input.mtaId, dependency.getName()), input.newMtaVersion, input.targetSpace);
            if (input.expectedAddedDependencies.contains(dependency.getName())) {
                Mockito.verify(configurationEntryDaoMock).add(Mockito.argThat(matcher));
            } else {
                List<ConfigurationEntry> entries = exisitingConfigurationEntries.stream().filter(ce -> matcher.matches(ce)).collect(
                    Collectors.toList());
                for (ConfigurationEntry existing : entries) {
                    Mockito.verify(configurationEntryDaoMock).update(Mockito.eq(existing.getId()), Mockito.argThat(matcher));
                }
            }
        }
    }

    @Override
    protected PublishProvidedDependenciesStep createStep() {
        return new PublishProvidedDependenciesStep();
    }
}
