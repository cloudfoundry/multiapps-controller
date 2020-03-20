package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteDiscontinuedConfigurationEntriesStepTest extends SyncFlowableStepTest<DeleteDiscontinuedConfigurationEntriesStep> {

    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationEntryQuery configurationEntryQuery;

    private final StepInput stepInput;

    public DeleteDiscontinuedConfigurationEntriesStepTest(String stepInputLocation) throws ParsingException {
        stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputLocation, DeleteDiscontinuedConfigurationEntriesStepTest.class),
                                      StepInput.class);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are no old entries:
            {
                "delete-published-dependencies-step-input-0.json",
            },
            // (1) All of the configuration entries should be deleted:
            {
                "delete-published-dependencies-step-input-1.json",
            },
            // (2) Some of the configuration entries should be deleted:
            {
                "delete-published-dependencies-step-input-2.json",
            },
            // (3) None of the configuration entries should be deleted:
            {
                "delete-published-dependencies-step-input-3.json",
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() {
        prepareContext();

        CloudTarget target = new CloudTarget(stepInput.org, stepInput.space);

        doReturn(configurationEntryQuery).when(configurationEntryService)
                                         .createQuery();
        ConfigurationEntryQuery queryMock = new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(ConfigurationEntriesUtil.PROVIDER_NID))
                                                                                      .on(query -> query.target(target))
                                                                                      .on(query -> query.mtaId(stepInput.mtaId))
                                                                                      .build();
        doReturn(stepInput.entriesForMta).when(queryMock)
                                         .list();
    }

    private void prepareContext() {
        context.setVariable(Variables.SPACE, stepInput.space);
        context.setVariable(Variables.ORG, stepInput.org);
        execution.setVariable(Constants.PARAM_MTA_ID, stepInput.mtaId);
        Mockito.when(execution.getProcessInstanceId())
               .thenReturn("process-instance-id");
        Mockito.when(flowableFacadeFacade.getHistoricSubProcessIds(Mockito.any()))
               .thenReturn(Collections.singletonList("test-subprocess-id"));
        HistoricVariableInstance varInstanceMock = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(flowableFacadeFacade.getHistoricVariableInstance("test-subprocess-id", Constants.VAR_PUBLISHED_ENTRIES))
               .thenReturn(varInstanceMock);
        Mockito.when(varInstanceMock.getValue())
               .thenReturn(getBytes(stepInput.publishedEntries));
        HistoricVariableInstance varInstanceMockDeletedEntries = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(flowableFacadeFacade.getHistoricVariableInstance("process-instance-id", Constants.VAR_DELETED_ENTRIES))
               .thenReturn(varInstanceMockDeletedEntries);
        Mockito.when(varInstanceMockDeletedEntries.getValue())
               .thenReturn(getBytes(getEntriesToDelete()));
    }

    private byte[] getBytes(List<ConfigurationEntry> publishedEntries) {
        return JsonUtil.toJsonBinary(publishedEntries.toArray(new ConfigurationEntry[] {}));
    }

    @Test
    public void testExecute() {
        List<ConfigurationEntryQuery> queriesToExecuteDeleteOn = initEntryQueries();

        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals(toJson(getEntriesToDelete()),
                     toJson(StepsUtil.getDeletedEntriesFromProcess(flowableFacadeFacade, execution.getProcessInstanceId())));

        for (ConfigurationEntryQuery query : queriesToExecuteDeleteOn) {
            verify(query).delete();
        }
    }

    private List<ConfigurationEntryQuery> initEntryQueries() {
        return stepInput.idsOfExpectedEntriesToDelete.stream()
                                                     .map(id -> new MockBuilder<>(configurationEntryQuery).on(query -> query.id(id))
                                                                                                          .build())
                                                     .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getEntriesToDelete() {
        List<ConfigurationEntry> result = new ArrayList<>();
        for (Long id : stepInput.idsOfExpectedEntriesToDelete) {
            CollectionUtils.addIgnoreNull(result, findEntry(stepInput.entriesForMta, id));
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
        private String space;
        private String org;
    }

    @Override
    protected DeleteDiscontinuedConfigurationEntriesStep createStep() {
        return new DeleteDiscontinuedConfigurationEntriesStep();
    }

}
