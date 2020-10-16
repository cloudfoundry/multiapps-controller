package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

class DeleteDiscontinuedConfigurationEntriesStepTest extends SyncFlowableStepTest<DeleteDiscontinuedConfigurationEntriesStep> {

    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationEntryQuery configurationEntryQuery;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (0) There are no old entries:
            Arguments.of("delete-published-dependencies-step-input-0.json"),
            // (1) All of the configuration entries should be deleted:
            Arguments.of("delete-published-dependencies-step-input-1.json"),
            // (2) Some of the configuration entries should be deleted:
            Arguments.of("delete-published-dependencies-step-input-2.json"),
            // (3) None of the configuration entries should be deleted:
            Arguments.of("delete-published-dependencies-step-input-3.json")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String stepInputLocation) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputLocation,
                                                                         DeleteDiscontinuedConfigurationEntriesStepTest.class),
                                            StepInput.class);
        initializeParameters(input);
        List<ConfigurationEntryQuery> queriesToExecuteDeleteOn = initEntryQueries(input);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals(toJson(getEntriesToDelete(input)),
                     toJson(StepsUtil.getDeletedEntriesFromProcess(flowableFacadeFacade, execution.getProcessInstanceId())));

        for (ConfigurationEntryQuery query : queriesToExecuteDeleteOn) {
            verify(query).delete();
        }
    }

    public void initializeParameters(StepInput input) {
        prepareContext(input);

        doReturn(configurationEntryQuery).when(configurationEntryService)
                                         .createQuery();
        ConfigurationEntryQuery queryMock = new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(ConfigurationEntriesUtil.PROVIDER_NID))
                                                                                      .on(query -> query.spaceId(input.spaceId))
                                                                                      .on(query -> query.mtaId(input.mtaId))
                                                                                      .build();
        doReturn(input.entriesForMta).when(queryMock)
                                     .list();
    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.MTA_ID, input.mtaId);
        context.setVariable(Variables.SPACE_GUID, input.spaceId);
        Mockito.when(execution.getProcessInstanceId())
               .thenReturn("process-instance-id");
        Mockito.when(flowableFacadeFacade.getHistoricSubProcessIds(Mockito.any()))
               .thenReturn(List.of("test-subprocess-id"));
        HistoricVariableInstance varInstanceMock = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(flowableFacadeFacade.getHistoricVariableInstance("test-subprocess-id", Variables.PUBLISHED_ENTRIES.getName()))
               .thenReturn(varInstanceMock);
        Mockito.when(varInstanceMock.getValue())
               .thenReturn(getBytes(input.publishedEntries));
        HistoricVariableInstance varInstanceMockDeletedEntries = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(flowableFacadeFacade.getHistoricVariableInstance("process-instance-id", Variables.DELETED_ENTRIES.getName()))
               .thenReturn(varInstanceMockDeletedEntries);
        Mockito.when(varInstanceMockDeletedEntries.getValue())
               .thenReturn(getBytes(getEntriesToDelete(input)));
    }

    private byte[] getBytes(List<ConfigurationEntry> publishedEntries) {
        return JsonUtil.toJsonBinary(publishedEntries.toArray(new ConfigurationEntry[] {}));
    }

    private List<ConfigurationEntryQuery> initEntryQueries(StepInput input) {
        return input.idsOfExpectedEntriesToDelete.stream()
                                                 .map(id -> new MockBuilder<>(configurationEntryQuery).on(query -> query.id(id))
                                                                                                      .build())
                                                 .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getEntriesToDelete(StepInput input) {
        List<ConfigurationEntry> result = new ArrayList<>();
        for (Long id : input.idsOfExpectedEntriesToDelete) {
            CollectionUtils.addIgnoreNull(result, findEntry(input.entriesForMta, id));
        }
        return result;
    }

    private ConfigurationEntry findEntry(List<ConfigurationEntry> entries, long id) {
        return entries.stream()
                      .filter((entry) -> entry.getId() == id)
                      .findFirst()
                      .orElse(null);
    }

    private String toJson(List<ConfigurationEntry> entries) {
        return JsonUtil.toJson(entries, true);
    }

    private static class StepInput {
        private List<Long> idsOfExpectedEntriesToDelete;
        private List<ConfigurationEntry> entriesForMta;
        private List<ConfigurationEntry> publishedEntries;
        private String mtaId;
        private String spaceId;
    }

    @Override
    protected DeleteDiscontinuedConfigurationEntriesStep createStep() {
        return new DeleteDiscontinuedConfigurationEntriesStep();
    }

}
