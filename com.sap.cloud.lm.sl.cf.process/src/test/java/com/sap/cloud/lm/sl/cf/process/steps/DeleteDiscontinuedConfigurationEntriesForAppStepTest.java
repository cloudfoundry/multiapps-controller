package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataValidator;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataValidator;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.core.util.MockBuilder;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteDiscontinuedConfigurationEntriesForAppStepTest
    extends SyncFlowableStepTest<DeleteDiscontinuedConfigurationEntriesForAppStep> {

    @Mock
    private ConfigurationEntryService configurationEntryService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ConfigurationEntryQuery configurationEntryQuery;
    private CloudTarget target;
    private MtaMetadataValidator mtaMetadataValidator = new MtaMetadataValidator();
    private EnvMtaMetadataValidator envMtaMetadataValidator = new EnvMtaMetadataValidator();
    @Spy
    private MtaMetadataParser mtaMetadataParser = new MtaMetadataParser(mtaMetadataValidator);
    @Spy
    private EnvMtaMetadataParser envMtaMetadataParser = new EnvMtaMetadataParser(envMtaMetadataValidator);

    private static class StepInput {
        String org;
        String space;
        CloudApplication existingApp;
        String mtaId;
        String mtaVersion;
        final List<ConfigurationEntry> publishedEntries = Collections.emptyList();
        final List<ConfigurationEntry> existingEntries = Collections.emptyList();
    }

    private static class StepOutput {
        @SuppressWarnings("unused")
        List<ConfigurationEntry> deletedEntries;
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

    private final String inputLocation;
    private final String expectedOutputLocation;

    private StepInput input;
    private StepOutput expectedOutput;

    public DeleteDiscontinuedConfigurationEntriesForAppStepTest(String inputLocation, String expectedOutputLocation) {
        this.inputLocation = inputLocation;
        this.expectedOutputLocation = expectedOutputLocation;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareConfigurationEntryService();
    }

    private void loadParameters() {
        String inputJson = TestUtil.getResourceAsString(inputLocation, getClass());
        this.input = JsonUtil.fromJson(inputJson, StepInput.class);
        String expectedOutputJson = TestUtil.getResourceAsString(expectedOutputLocation, getClass());
        this.expectedOutput = JsonUtil.fromJson(expectedOutputJson, StepOutput.class);
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_ORG, input.org);
        context.setVariable(Constants.VAR_SPACE, input.space);
        StepsUtil.setExistingApp(context, input.existingApp);
        context.setVariable(Constants.PARAM_MTA_ID, input.mtaId);
        StepsUtil.setPublishedEntries(context, input.publishedEntries);
    }

    private void prepareConfigurationEntryService() {
        Mockito.when(configurationEntryService.createQuery())
               .thenReturn(configurationEntryQuery);
        ConfigurationEntryQuery queryMock = new MockBuilder<>(configurationEntryQuery).on(query -> query.providerNid(ConfigurationEntriesUtil.PROVIDER_NID))
                                                                                      .on(query -> query.version(input.mtaVersion))
                                                                                      .on(query -> query.target(Mockito.any()),
                                                                                          invocation -> target = (CloudTarget) invocation.getArguments()[0])
                                                                                      .on(query -> query.mtaId(input.mtaId))
                                                                                      .build();
        Mockito.when(queryMock.list())
               .thenAnswer(invocation -> input.existingEntries.stream()
                                                              .filter(entry -> entry.getTargetSpace()
                                                                                    .equals(target))
                                                              .collect(Collectors.toList()));
    }

    @Test
    public void testExecute() {
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

}
