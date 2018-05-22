package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.files.FilePartsMerger;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.persistence.util.DefaultConfiguration;

@RunWith(Parameterized.class)
public class ValidateDeployParametersStepTest extends SyncActivitiStepTest<ValidateDeployParametersStep> {

    private static final String MERGED_ARCHIVE_TEST_MTAR = "merged-archive-test.mtar";
    private static final String EXISTING_FILE_ID = "existingFileId";
    private static final String EXISTING_BIGGER_FILE_ID = "existingBiggerFileId";
    private static final String NOT_EXISTING_FILE_ID = "notExistingFileId";
    private static final String MERGED_ARCHIVE_NAME = "test-merged";

    private final StepInput stepInput;
    private final String expectedExceptionMessage;
    private final boolean isArchiveChunked;
    private FilePartsMerger merger;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Object[][] getParameters() {
        return new Object[][] {
            new Object[] { new StepInput(null, null, -1, null),
                MessageFormat.format(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, "-1", "startTimeout"), false },
            new Object[] { new StepInput(NOT_EXISTING_FILE_ID, null, 1, null),
                MessageFormat.format(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0, "notExistingFileId"), false },
            new Object[] { new StepInput(EXISTING_FILE_ID, NOT_EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, null),
                MessageFormat.format(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0, "notExistingFileId"), false },
            new Object[] { new StepInput(EXISTING_FILE_ID, EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, "asd"),
                MessageFormat.format(Messages.ERROR_PARAMETER_1_IS_NOT_VALID_VALID_VALUES_ARE_2, "asd", "versionRule", ""), false },
            new Object[] { new StepInput(EXISTING_FILE_ID, EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, VersionRule.HIGHER.toString()),
                null, false },
            new Object[] { new StepInput(EXISTING_FILE_ID, EXISTING_BIGGER_FILE_ID, 1, VersionRule.HIGHER.toString()),
                MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                    "1048577", "extDescriptorFile", "1048576"),
                false },
            new Object[] {
                new StepInput(MERGED_ARCHIVE_NAME + ".part.0," + MERGED_ARCHIVE_NAME + ".part.1," + MERGED_ARCHIVE_NAME + ".part.2", null,
                    1, VersionRule.HIGHER.toString()),
                null, true } };
    }

    public ValidateDeployParametersStepTest(StepInput stepInput, String expectedExceptionMessage, boolean isArchiveChunked)
        throws FileStorageException {
        this.stepInput = stepInput;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.isArchiveChunked = isArchiveChunked;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareExpectedException();
        prepareFileService();
        prepareArchiveMerger();
        prepareConfiguration();
    }

    private void prepareConfiguration() {
        Mockito.when(configuration.getMaxMtaDescriptorSize()).thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        Mockito.when(configuration.getFileConfiguration()).thenReturn(
            new DefaultConfiguration(ApplicationConfiguration.DEFAULT_MAX_UPLOAD_SIZE, ApplicationConfiguration.DEFAULT_SCAN_UPLOADS));

    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);
        validate();
    }

    private void validate() throws IOException {
        assertStepFinishedSuccessfully();
        if (isArchiveChunked) {
            Path mergedArchiveAbsolutePath = Paths.get(MERGED_ARCHIVE_NAME).toAbsolutePath();
            assertFalse(Files.exists(mergedArchiveAbsolutePath));
        }
    }

    private void prepareContext() {
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_APP_ARCHIVE_ID, stepInput.appArchiveId);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_EXT_DESCRIPTOR_FILE_ID, stepInput.extDescriptorId);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_START_TIMEOUT, stepInput.startTimeout);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_VERSION_RULE, stepInput.versionRule);
        context.setVariable(com.sap.cloud.lm.sl.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, "space-id");
        context.setVariable(com.sap.cloud.lm.sl.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID, "service-id");
    }

    private void prepareExpectedException() {
        if (expectedExceptionMessage != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareFileService() throws FileStorageException {
        Mockito.when(fileService.getFile("space-id", EXISTING_FILE_ID)).thenReturn(createFileEntry(EXISTING_FILE_ID, null, 1024 * 1024l));
        Mockito.when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.0")).thenReturn(
            createFileEntry(MERGED_ARCHIVE_NAME + ".part.0", MERGED_ARCHIVE_NAME + ".part.0", 1024 * 1024l));

        Mockito.when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.1")).thenReturn(
            createFileEntry(MERGED_ARCHIVE_NAME + ".part.1", MERGED_ARCHIVE_NAME + ".part.1", 1024 * 1024l));

        Mockito.when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.2")).thenReturn(
            createFileEntry(MERGED_ARCHIVE_NAME + ".part.2", MERGED_ARCHIVE_NAME + ".part.2", 1024 * 1024l));

        Mockito.when(fileService.getFile("space-id", EXISTING_BIGGER_FILE_ID)).thenReturn(
            createFileEntry(EXISTING_BIGGER_FILE_ID, "extDescriptorFile", 1024 * 1024l + 1));
        Mockito.when(fileService.getFile("space-id", NOT_EXISTING_FILE_ID)).thenReturn(null);
        Mockito.when(fileService.addFile(Mockito.eq("space-id"), Mockito.eq("service-id"), Mockito.any(), Mockito.any(),
            Mockito.<File> any())).thenReturn(createFileEntry(EXISTING_FILE_ID, MERGED_ARCHIVE_TEST_MTAR, 1024 * 1024 * 1024l));
    }

    private void prepareArchiveMerger() throws IOException {
        merger = Mockito.mock(FilePartsMerger.class);
        Mockito.when(merger.getMergedFilePath()).thenReturn(Paths.get(MERGED_ARCHIVE_TEST_MTAR));
    }

    private static FileEntry createFileEntry(String id, String name, long size) {
        FileEntry fe = new FileEntry();
        fe.setId(id);
        fe.setName(name);
        fe.setSize(BigInteger.valueOf(size));
        return fe;
    }

    private static class StepInput {

        public String appArchiveId;
        public String extDescriptorId;
        public int startTimeout;
        public String versionRule;

        public StepInput(String appArchiveId, String extDescriptorId, int startTimeout, String versionRule) {
            this.appArchiveId = appArchiveId;
            this.extDescriptorId = extDescriptorId;
            this.startTimeout = startTimeout;
            this.versionRule = versionRule;
        }
    }

    @Override
    protected ValidateDeployParametersStep createStep() {
        return new ValidateDeployParametersStep();
    }

}
