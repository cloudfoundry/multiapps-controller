package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.activiti.engine.history.HistoricVariableInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteDiscontinuedConfigurationEntriesStepTest extends SyncActivitiStepTest<DeleteDiscontinuedConfigurationEntriesStep> {

    @Mock
    private ConfigurationEntryDao configurationEntryDao;

    private StepInput stepInput;

    public DeleteDiscontinuedConfigurationEntriesStepTest(String stepInputLocation) throws ParsingException, IOException {
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
            // (1) There are entries with the old 'target' format and all of them should be deleted:
            {
                "delete-published-dependencies-step-input-1.json",
            },
            // (2) There are entries with the new 'target' format and all of them should be deleted:
            {
                "delete-published-dependencies-step-input-2.json",
            },
            // (3) Some of the configuration entries should be deleted:
            {
                "delete-published-dependencies-step-input-3.json",
            },
            // (4) None of the configuration entries should be deleted:
            {
                "delete-published-dependencies-step-input-4.json",
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();

        CloudTarget newTarget = new CloudTarget(stepInput.org, stepInput.space);
        CloudTarget oldTarget = new CloudTarget(null, stepInput.spaceId);

        Mockito.when(configurationEntryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, newTarget, null, stepInput.mtaId))
            .thenReturn(stepInput.allEntriesForMtaWithNewTarget);
        Mockito.when(configurationEntryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, oldTarget, null, stepInput.mtaId))
            .thenReturn(stepInput.allEntriesForMtaWithOldTarget);
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_SPACE, stepInput.space);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, stepInput.spaceId);
        context.setVariable(Constants.VAR_ORG, stepInput.org);
        context.setVariable(Constants.PARAM_MTA_ID, stepInput.mtaId);
        Mockito.when(context.getProcessInstanceId())
            .thenReturn("process-instance-id");
        Mockito.when(activitiFacade.getHistoricSubProcessIds(Mockito.any()))
            .thenReturn(Arrays.asList("test-subprocess-id"));
        HistoricVariableInstance varInstanceMock = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(activitiFacade.getHistoricVariableInstance("test-subprocess-id", Constants.VAR_PUBLISHED_ENTRIES))
            .thenReturn(varInstanceMock);
        Mockito.when(varInstanceMock.getValue())
            .thenReturn(getBytes(stepInput.publishedEntries));
        HistoricVariableInstance varInstanceMockDeletedEntries = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(activitiFacade.getHistoricVariableInstance("process-instance-id", Constants.VAR_DELETED_ENTRIES))
            .thenReturn(varInstanceMockDeletedEntries);
        Mockito.when(varInstanceMockDeletedEntries.getValue())
            .thenReturn(getBytes(getEntriesToDelete()));
    }

    private byte[] getBytes(List<ConfigurationEntry> publishedEntries) {
        return JsonUtil.getAsBinaryJson(publishedEntries.toArray(new ConfigurationEntry[] {}));
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(toJson(getEntriesToDelete()),
            toJson(StepsUtil.getDeletedEntriesFromProcess(activitiFacade, context.getProcessInstanceId())));

        for (Long id : (stepInput.idsOfExpectedEntriesToDelete)) {
            verify(configurationEntryDao).remove(id);
        }
    }

    private List<ConfigurationEntry> getEntriesToDelete() {
        List<ConfigurationEntry> result = new ArrayList<>();
        for (Long id : (stepInput.idsOfExpectedEntriesToDelete)) {
            ListUtil.addNonNull(result, findEntry(stepInput.allEntriesForMtaWithNewTarget, id));
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
        return JsonUtil.toJson(entries, true, true, false);
    }

    private static class StepInput {
        private List<Long> idsOfExpectedEntriesToDelete;
        private List<ConfigurationEntry> allEntriesForMtaWithNewTarget;
        private List<ConfigurationEntry> allEntriesForMtaWithOldTarget;
        private List<ConfigurationEntry> publishedEntries;
        private String mtaId;
        private String space;
        private String org;
        private String spaceId;
    }

    @Override
    protected DeleteDiscontinuedConfigurationEntriesStep createStep() {
        return new DeleteDiscontinuedConfigurationEntriesStep();
    }

}
