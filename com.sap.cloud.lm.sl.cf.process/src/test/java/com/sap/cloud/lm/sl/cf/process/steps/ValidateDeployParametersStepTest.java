package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.files.FilePartsMerger;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.util.DefaultConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.JarSignatureOperations;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

public class ValidateDeployParametersStepTest extends SyncFlowableStepTest<ValidateDeployParametersStep> {

    private static final String MERGED_ARCHIVE_TEST_MTAR = "merged-archive-test.mtar";
    private static final String EXISTING_FILE_ID = "existingFileId";
    private static final String EXISTING_BIGGER_FILE_ID = "existingBiggerFileId";
    private static final String NOT_EXISTING_FILE_ID = "notExistingFileId";
    private static final String MERGED_ARCHIVE_NAME = "test-merged";
    private static final String EXCEPTION_START_MESSAGE = "Error validating parameters: ";

    private StepInput stepInput;
    private boolean isArchiveChunked;

    private FilePartsMerger merger;

    @Mock
    private JarSignatureOperations jarSignatureOperations;

    // @formatter:off
    private static Stream<Arguments> testExecution() {
        return Stream.of(
                // startTimeout parameter is negative
                Arguments.of(new StepInput(null, null, -1, null, false),
                        MessageFormat.format(Messages.ERROR_PARAMETER_1_MUST_NOT_BE_NEGATIVE, "-1", "startTimeout"), false, ""),

                // No file associated with the specified file id
                Arguments.of(new StepInput(EXISTING_FILE_ID, NOT_EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, null, false),
                        MessageFormat.format(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0_IN_SPACE_1, "notExistingFileId",
                                     "space-id"), false, ""),

                // Invalid version rule
                Arguments.of(new StepInput(EXISTING_FILE_ID, EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, "asd", false),
                        MessageFormat.format(Messages.ERROR_PARAMETER_1_IS_NOT_VALID_VALID_VALUES_ARE_2, "asd", "versionRule", ""), false, "[SAME_HIGHER, HIGHER, ALL]"),

                // Valid parameters
                Arguments.of(new StepInput(EXISTING_FILE_ID, EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, VersionRule.HIGHER.toString(), false),
                        null, false, ""),

                // Max descriptor size exceeded
                Arguments.of(new StepInput(EXISTING_FILE_ID, EXISTING_BIGGER_FILE_ID, 1, VersionRule.HIGHER.toString(), false),
                        MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                     "1048577", "extDescriptorFile", "1048576"), false, ""),

                // Process chunked file
                Arguments.of(new StepInput(MERGED_ARCHIVE_NAME + ".part.0," + MERGED_ARCHIVE_NAME + ".part.1," + MERGED_ARCHIVE_NAME
                          + ".part.2", null, 1, VersionRule.HIGHER.toString(), false), null, true, ""),

                // Verify archive signature with default certificate CN
                Arguments.of(new StepInput(EXISTING_FILE_ID, null, 1, VersionRule.HIGHER.toString(), true),
                        null, false, ""),

                // Verify archive signature with custom certificate CN
                Arguments.of(new StepInput(EXISTING_FILE_ID, null, 1, VersionRule.HIGHER.toString(), true),
                        null, false, "")
                );
    }
    // @formatter:on

    @MethodSource
    @ParameterizedTest
    public void testExecution(StepInput stepInput, String expectedExceptionMessage, boolean isArchiveChunked, String versionOutput)
        throws Exception {
        initializeComponents(stepInput, isArchiveChunked);
        if (expectedExceptionMessage != null) {
            SLException exception = Assertions.assertThrows(SLException.class, () -> step.execute(context));
            Assertions.assertEquals(EXCEPTION_START_MESSAGE + expectedExceptionMessage + versionOutput, exception.getMessage()
                                                                                                                 .trim());
            return;
        }
        step.execute(context);
        validate();
    }

    private void initializeComponents(StepInput stepInput, boolean isArchiveChunked) throws FileStorageException {
        this.stepInput = stepInput;
        this.isArchiveChunked = isArchiveChunked;
        prepareContext();
        prepareFileService();
        prepareArchiveMerger();
        prepareConfiguration();
    }

    private void prepareContext() {
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_APP_ARCHIVE_ID, stepInput.appArchiveId);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_EXT_DESCRIPTOR_FILE_ID, stepInput.extDescriptorId);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_START_TIMEOUT, stepInput.startTimeout);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_VERSION_RULE, stepInput.versionRule);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, "space-id");
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SERVICE_ID, "service-id");
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_VERIFY_ARCHIVE_SIGNATURE, stepInput.shouldVerifyArchive);
    }

    private void prepareFileService() throws FileStorageException {
        Mockito.when(fileService.getFile("space-id", EXISTING_FILE_ID))
               .thenReturn(createFileEntry(EXISTING_FILE_ID, "some-file-entry-name", 1024 * 1024L));
        Mockito.when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.0"))
               .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.0", MERGED_ARCHIVE_NAME + ".part.0", 1024 * 1024L));

        Mockito.when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.1"))
               .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.1", MERGED_ARCHIVE_NAME + ".part.1", 1024 * 1024L));

        Mockito.when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.2"))
               .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.2", MERGED_ARCHIVE_NAME + ".part.2", 1024 * 1024L));

        Mockito.when(fileService.getFile("space-id", EXISTING_BIGGER_FILE_ID))
               .thenReturn(createFileEntry(EXISTING_BIGGER_FILE_ID, "extDescriptorFile", 1024 * 1024L + 1));
        Mockito.when(fileService.getFile("space-id", NOT_EXISTING_FILE_ID))
               .thenReturn(null);
        Mockito.when(fileService.addFile(Mockito.eq("space-id"), Mockito.eq("service-id"), Mockito.anyString(), any()))
               .thenReturn(createFileEntry(EXISTING_FILE_ID, MERGED_ARCHIVE_TEST_MTAR, 1024 * 1024 * 1024L));
    }

    private static FileEntry createFileEntry(String id, String name, long size) {
        FileEntry fe = new FileEntry();
        fe.setId(id);
        fe.setName(name);
        fe.setSize(BigInteger.valueOf(size));
        return fe;
    }

    private void prepareArchiveMerger() {
        merger = Mockito.mock(FilePartsMerger.class);
        Mockito.when(merger.getMergedFilePath())
               .thenReturn(Paths.get(MERGED_ARCHIVE_TEST_MTAR));
    }

    private void prepareConfiguration() {
        Mockito.when(configuration.getMaxMtaDescriptorSize())
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        Mockito.when(configuration.getFileConfiguration())
               .thenReturn(new DefaultConfiguration(ApplicationConfiguration.DEFAULT_MAX_UPLOAD_SIZE));
    }

    private void validate() {
        assertStepFinishedSuccessfully();
        if (isArchiveChunked) {
            Path mergedArchiveAbsolutePath = Paths.get(MERGED_ARCHIVE_NAME)
                                                  .toAbsolutePath();
            Assertions.assertFalse(Files.exists(mergedArchiveAbsolutePath));
        }
        if (stepInput.shouldVerifyArchive) {
            List<X509Certificate> certificates = jarSignatureOperations.readCertificates(Constants.SYMANTEC_CERTIFICATE_FILE);
            Mockito.verify(jarSignatureOperations)
                   .checkCertificates(any(), eq(certificates), any());
        }
        Mockito.verify(context, Mockito.atLeastOnce())
               .setVariable(Constants.PARAM_APP_ARCHIVE_ID, stepInput.appArchiveId);
    }

    @Override
    protected ValidateDeployParametersStep createStep() {
        return new ValidateDeployParametersStep();
    }

    private static class StepInput {

        private String appArchiveId;
        private String extDescriptorId;
        private int startTimeout;
        private String versionRule;
        private boolean shouldVerifyArchive;

        public StepInput(String appArchiveId, String extDescriptorId, int startTimeout, String versionRule, boolean shouldVerifyArchive) {
            this.appArchiveId = appArchiveId;
            this.extDescriptorId = extDescriptorId;
            this.startTimeout = startTimeout;
            this.versionRule = versionRule;
            this.shouldVerifyArchive = shouldVerifyArchive;
        }
    }

}
