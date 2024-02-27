package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
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
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteDiscontinuedConfigurationEntriesForAppStepTest
    extends SyncFlowableStepTest<DeleteDiscontinuedConfigurationEntriesForAppStep> {

    @Mock
    private ConfigurationEntryDao dao;
    private String inputLocation;
    private String expectedOutputLocation;
    private StepInput input;
    private StepOutput expectedOutput;
    public DeleteDiscontinuedConfigurationEntriesForAppStepTest(String inputLocation, String expectedOutputLocation) {
        this.inputLocation = inputLocation;
        this.expectedOutputLocation = expectedOutputLocation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There is no existing app:
            {
                "delete-discontinued-configuration-entries-for-app-step-input-00.json", "delete-discontinued-configuration-entries-for-app-step-output-00.json"
            },
            // (1) There is an existing app, but it hasn't provided any dependencies:
            {
                "delete-discontinued-configuration-entries-for-app-step-input-01.json", "delete-discontinued-configuration-entries-for-app-step-output-00.json"
            },
            // (2) There is an existing app, it has provided dependencies, but the new version doesn't:
            {
                "delete-discontinued-configuration-entries-for-app-step-input-02.json", "delete-discontinued-configuration-entries-for-app-step-output-02.json"
            },
            // (3) There is an existing app, it has no provided dependencies, but the new version does:
            {
                "delete-discontinued-configuration-entries-for-app-step-input-03.json", "delete-discontinued-configuration-entries-for-app-step-output-00.json"
            },
            // (4) There is an existing app, it has provided dependencies, the new version also does and they are the same:
            {
                "delete-discontinued-configuration-entries-for-app-step-input-04.json", "delete-discontinued-configuration-entries-for-app-step-output-00.json"
            },
            // (5) There is an existing app, it has provided dependencies, the new version also does, but they are not the same:
            {
                "delete-discontinued-configuration-entries-for-app-step-input-05.json", "delete-discontinued-configuration-entries-for-app-step-output-05.json"
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareDao();
    }

    private void loadParameters() throws IOException {
        String inputJson = TestUtil.getResourceAsString(inputLocation, getClass());
        this.input = JsonUtil.fromJson(inputJson, StepInput.class);
        String expectedOutputJson = TestUtil.getResourceAsString(expectedOutputLocation, getClass());
        this.expectedOutput = JsonUtil.fromJson(expectedOutputJson, StepOutput.class);
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_ORG, input.org);
        context.setVariable(Constants.VAR_SPACE, input.space);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID, input.spaceId);
        StepsUtil.setExistingApp(context, input.existingApp);
        context.setVariable(Constants.PARAM_MTA_ID, input.mtaId);
        StepsUtil.setPublishedEntries(context, input.publishedEntries);
    }

    private void prepareDao() {
        Mockito.when(dao.find(Mockito.eq(ConfigurationEntriesUtil.PROVIDER_NID), Mockito.eq(null), Mockito.eq(input.mtaVersion),
                              Mockito.any(), Mockito.eq(null), Mockito.eq(input.mtaId)))
               .thenAnswer((invocation) -> {
                   CloudTarget target = (CloudTarget) invocation.getArguments()[3];
                   return input.existingEntries.stream()
                                               .filter(entry -> entry.getTargetSpace()
                                                                     .equals(target))
                                               .collect(Collectors.toList());
               });
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();
        StepOutput actualOutput = getActualOutput();
        assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));
    }

    private StepOutput getActualOutput() {
        StepOutput actualOutput = new StepOutput();
        actualOutput.deletedEntries = StepsUtil.getDeletedEntries(context);
        return actualOutput;
    }

    @Override
    protected DeleteDiscontinuedConfigurationEntriesForAppStep createStep() {
        return new DeleteDiscontinuedConfigurationEntriesForAppStep();
    }

    private static class StepInput {
        String org;
        String space;
        String spaceId;
        CloudApplication existingApp;
        String mtaId;
        String mtaVersion;
        List<ConfigurationEntry> publishedEntries = Collections.emptyList();
        List<ConfigurationEntry> existingEntries = Collections.emptyList();
    }

    private static class StepOutput {
        @SuppressWarnings("unused")
        List<ConfigurationEntry> deletedEntries;
    }

}
