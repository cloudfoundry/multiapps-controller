package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.printingAssertEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeletePublishedDependenciesStepTest extends AbstractStepTest<DeletePublishedDependenciesStep> {

    @Mock
    private ConfigurationEntryDao configurationEntryDao;

    private StepInput stepInput;

    public DeletePublishedDependenciesStepTest(String stepInputLocation) throws ParsingException, IOException {
        stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputLocation, DeletePublishedDependenciesStepTest.class),
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

        String newTarget = ConfigurationEntriesUtil.computeTargetSpace(new Pair<>(stepInput.org, stepInput.space));
        String oldTarget = stepInput.spaceId;

        Mockito.when(
            configurationEntryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, newTarget, null, stepInput.mtaId)).thenReturn(
                stepInput.allEntriesForMtaWithNewTarget);
        Mockito.when(
            configurationEntryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, oldTarget, null, stepInput.mtaId)).thenReturn(
                stepInput.allEntriesForMtaWithOldTarget);
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_SPACE, stepInput.space);
        context.setVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID, stepInput.spaceId);
        context.setVariable(Constants.VAR_ORG, stepInput.org);
        StepsUtil.setPublishedEntries(context, stepInput.publishedEntries);
        context.setVariable(Constants.PARAM_MTA_ID, stepInput.mtaId);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        printingAssertEquals(toJson(getExpectedEntriesToDelete()), toJson(StepsUtil.getDeletedEntries(context)));

        for (Long id : (stepInput.idsOfExpectedEntriesToDelete)) {
            verify(configurationEntryDao).remove(id);
        }
    }

    private List<ConfigurationEntry> getExpectedEntriesToDelete() {
        List<ConfigurationEntry> result = new ArrayList<>();
        for (Long id : (stepInput.idsOfExpectedEntriesToDelete)) {
            ListUtil.addNonNull(result, findEntry(stepInput.allEntriesForMtaWithNewTarget, id));
            ListUtil.addNonNull(result, findEntry(stepInput.allEntriesForMtaWithOldTarget, id));
        }
        return result;
    }

    private ConfigurationEntry findEntry(List<ConfigurationEntry> entries, long id) {
        return entries.stream().filter((entry) -> entry.getId() == id).findFirst().orElse(null);
    }

    private String toJson(List<ConfigurationEntry> entries) {
        return JsonUtil.toJson(entries, true, true, false, false);
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
    protected DeletePublishedDependenciesStep createStep() {
        return new DeletePublishedDependenciesStep();
    }

}
