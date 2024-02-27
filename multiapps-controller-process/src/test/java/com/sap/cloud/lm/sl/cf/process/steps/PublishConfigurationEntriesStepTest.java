package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class PublishConfigurationEntriesStepTest extends SyncFlowableStepTest<PublishConfigurationEntriesStep> {

    private static List<ConfigurationEntry> exisitingConfigurationEntries;
    private StepInput input;
    private ConfigurationEntryDao configurationEntryDaoMock = Mockito.mock(ConfigurationEntryDao.class);
    public PublishConfigurationEntriesStepTest(String input) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, PublishConfigurationEntriesStepTest.class), StepInput.class);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "publish-configuration-entries-step-input-1.json"
            },
            {
                "publish-configuration-entries-step-input-2.json"
            },
            {
                "publish-configuration-entries-step-input-3.json"
            },
            {
                "publish-configuration-entries-step-input-4.json"
            },
// @formatter:on
        });
    }

    @BeforeClass
    public static void loadConfigurationEntries() throws Exception {
        exisitingConfigurationEntries = JsonUtil.fromJson(TestUtil.getResourceAsString("configuration-entries.json",
                                                                                       PublishConfigurationEntriesStepTest.class),
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
                                                        Mockito.matches(entry.getProviderVersion()
                                                                             .toString()),
                                                        Mockito.eq(entry.getTargetSpace()), Mockito.any(), Mockito.eq(null)))
                   .thenReturn(Arrays.asList(entry));
        }
    }

    private void prepareContext() {
        StepsUtil.setConfigurationEntriesToPublish(context, input.entriesToPublish);
        Mockito.when(context.getVariable(Constants.VAR_APP_TO_DEPLOY))
               .thenReturn(JsonUtil.toJson(new CloudApplicationExtended(Meta.defaultMeta(), "test-app-name")));
    }

    @Test
    public void test() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        validateConfigurationEntryDao();
    }

    private void validateConfigurationEntryDao() throws Exception {
        if (CollectionUtils.isEmpty(input.entriesToPublish)) {
            Mockito.verify(configurationEntryDaoMock, Mockito.never())
                   .add(Mockito.any());
            Mockito.verify(configurationEntryDaoMock, Mockito.never())
                   .update(Mockito.anyLong(), Mockito.any());
        }
        List<ConfigurationEntry> createdEntries = getCreatedEntries();
        List<ConfigurationEntry> updatedEntries = getUpdatedEntries();
        assertContainsEntries(input.expectedCreatedEntries, createdEntries);
        assertContainsEntries(input.expectedUpdatedEntries, updatedEntries);
    }

    private void assertContainsEntries(List<ConfigurationEntry> entries, List<ConfigurationEntry> expectedEntries) {
        for (ConfigurationEntry entry : entries) {
            assertContainsEntry(expectedEntries, entry);
        }

    }

    private List<ConfigurationEntry> getCreatedEntries() {
        ArgumentCaptor<ConfigurationEntry> configurationEntryCaptor = ArgumentCaptor.forClass(ConfigurationEntry.class);
        Mockito.verify(configurationEntryDaoMock, Mockito.times(input.expectedCreatedEntries.size()))
               .add(configurationEntryCaptor.capture());
        return configurationEntryCaptor.getAllValues();
    }

    private List<ConfigurationEntry> getUpdatedEntries() {
        ArgumentCaptor<ConfigurationEntry> configurationEntryCaptor = ArgumentCaptor.forClass(ConfigurationEntry.class);
        Mockito.verify(configurationEntryDaoMock, Mockito.times(input.expectedUpdatedEntries.size()))
               .update(Mockito.anyLong(), configurationEntryCaptor.capture());
        return configurationEntryCaptor.getAllValues();
    }

    private void assertContainsEntry(List<ConfigurationEntry> entries, ConfigurationEntry expectedEntry) {
        for (ConfigurationEntry entry : entries) {
            if (areEqual(entry, expectedEntry)) {
                return;
            }
        }
        fail("Could not an entry that matches: " + JsonUtil.toJson(expectedEntry, true));
    }

    private boolean areEqual(ConfigurationEntry entry1, ConfigurationEntry entry2) {
        // @formatter:off
        return Objects.equals(entry1.getProviderId(), entry2.getProviderId())
            && Objects.equals(entry1.getProviderNid(), entry2.getProviderNid())
            && Objects.equals(entry1.getProviderVersion(), entry2.getProviderVersion())
            && Objects.equals(entry1.getTargetSpace(), entry2.getTargetSpace())
            && Objects.equals(entry1.getContent(), entry2.getContent());
        // @formatter:on
    }

    @Override
    protected PublishConfigurationEntriesStep createStep() {
        return new PublishConfigurationEntriesStep();
    }

    private static class StepInput {

        List<ConfigurationEntry> entriesToPublish;
        List<ConfigurationEntry> expectedCreatedEntries = Collections.emptyList();
        List<ConfigurationEntry> expectedUpdatedEntries = Collections.emptyList();

    }
}
