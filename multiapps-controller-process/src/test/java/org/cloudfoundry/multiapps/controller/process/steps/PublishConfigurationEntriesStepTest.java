package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;

class PublishConfigurationEntriesStepTest extends SyncFlowableStepTest<PublishConfigurationEntriesStep> {

    private static class StepInput {

        List<ConfigurationEntry> entriesToPublish;
        final List<ConfigurationEntry> expectedCreatedEntries = Collections.emptyList();
        final List<ConfigurationEntry> expectedUpdatedEntries = Collections.emptyList();

    }

    private static List<ConfigurationEntry> existingConfigurationEntries;

    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationEntryQuery configurationEntryQuery;

    public static Stream<Arguments> test() {
        return Stream.of(
// @formatter:off
                Arguments.of("publish-configuration-entries-step-input-1.json"),
                Arguments.of("publish-configuration-entries-step-input-2.json"),
                Arguments.of("publish-configuration-entries-step-input-3.json"),
                Arguments.of("publish-configuration-entries-step-input-4.json")
// @formatter:on
        );
    }

    @BeforeAll
    public static void loadConfigurationEntries() {
        existingConfigurationEntries = JsonUtil.fromJson(TestUtil.getResourceAsString("configuration-entries.json",
                                                                                      PublishConfigurationEntriesStepTest.class),
                                                         new TypeReference<>() {
                                                         });
    }

    @ParameterizedTest
    @MethodSource
    void test(String inputFilename) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputFilename, PublishConfigurationEntriesStepTest.class),
                                            StepInput.class);
        initializeParameters(input);
        step.execute(execution);

        assertStepFinishedSuccessfully();

        validateConfigurationEntryService(input);
    }

    public void initializeParameters(StepInput input) {
        prepareContext(input);
        prepareConfigurationEntryService();
    }

    public void prepareConfigurationEntryService() {
        when(configurationEntryService.createQuery()).thenReturn(configurationEntryQuery);
        for (ConfigurationEntry entry : existingConfigurationEntries) {
            ConfigurationEntryQuery entryQueryMock = new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(entry.getProviderNid()))
                                                                                               .on(query -> query.providerId(entry.getProviderId()))
                                                                                               .on(query -> query.version(entry.getProviderVersion()
                                                                                                                               .toString()))
                                                                                               .on(query -> query.target(Mockito.eq(entry.getTargetSpace())))
                                                                                               .build();
            doReturn(List.of(entry)).when(entryQueryMock)
                                                      .list();
        }
    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.CONFIGURATION_ENTRIES_TO_PUBLISH, input.entriesToPublish);
        CloudApplicationExtended appToProcess = ImmutableCloudApplicationExtended.builder()
                                                                                 .metadata(CloudMetadata.defaultMetadata())
                                                                                 .name("test-app-name")
                                                                                 .build();
        context.setVariable(Variables.APP_TO_PROCESS, appToProcess);
    }

    private void validateConfigurationEntryService(StepInput input) {
        if (CollectionUtils.isEmpty(input.entriesToPublish)) {
            Mockito.verify(configurationEntryService, Mockito.never())
                   .add(Mockito.any());
            Mockito.verify(configurationEntryService, Mockito.never())
                   .update(Mockito.any(), Mockito.any());
        }
        List<ConfigurationEntry> createdEntries = getCreatedEntries(input);
        List<ConfigurationEntry> updatedEntries = getUpdatedEntries(input);
        assertContainsEntries(input.expectedCreatedEntries, createdEntries);
        assertContainsEntries(input.expectedUpdatedEntries, updatedEntries);
    }

    private void assertContainsEntries(List<ConfigurationEntry> entries, List<ConfigurationEntry> expectedEntries) {
        for (ConfigurationEntry entry : entries) {
            assertContainsEntry(expectedEntries, entry);
        }
    }

    private List<ConfigurationEntry> getCreatedEntries(StepInput input) {
        ArgumentCaptor<ConfigurationEntry> configurationEntryCaptor = ArgumentCaptor.forClass(ConfigurationEntry.class);
        Mockito.verify(configurationEntryService, Mockito.times(input.expectedCreatedEntries.size()))
               .add(configurationEntryCaptor.capture());
        return configurationEntryCaptor.getAllValues();
    }

    private List<ConfigurationEntry> getUpdatedEntries(StepInput input) {
        ArgumentCaptor<ConfigurationEntry> configurationEntryCaptor = ArgumentCaptor.forClass(ConfigurationEntry.class);
        Mockito.verify(configurationEntryService, Mockito.times(input.expectedUpdatedEntries.size()))
               .update(Mockito.any(), configurationEntryCaptor.capture());
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
}
