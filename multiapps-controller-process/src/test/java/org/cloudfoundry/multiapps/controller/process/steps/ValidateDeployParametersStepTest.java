package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.FileMimeTypeValidator;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.FilePartsMerger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.VersionRule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ValidateDeployParametersStepTest extends SyncFlowableStepTest<ValidateDeployParametersStep> {

    private static final String MERGED_ARCHIVE_TEST_MTAR = "merged-archive-test.mtar";
    private static final String EXISTING_FILE_ID = "existingFileId";
    private static final String EXISTING_BIGGER_FILE_ID = "existingBiggerFileId";
    private static final String NOT_EXISTING_FILE_ID = "notExistingFileId";
    private static final String MERGED_ARCHIVE_NAME = "test-merged";
    private static final String EXCEEDING_FILE_SIZE_ID = "exceedingFileSizeId";
    private static final String EXCEPTION_START_MESSAGE = "Error validating parameters: ";

    private StepInput stepInput;
    private boolean isArchiveChunked;

    private static Stream<Arguments> testExecution() {
        return Stream.of(
            // [1] No file associated with the specified file id
            Arguments.of(new StepInput(EXISTING_FILE_ID, NOT_EXISTING_FILE_ID + "," + EXISTING_FILE_ID, 1, null),
                         MessageFormat.format(Messages.ERROR_NO_FILE_ASSOCIATED_WITH_THE_SPECIFIED_FILE_ID_0_IN_SPACE_1,
                                              "notExistingFileId", "space-id"),
                         false, ""),

            // [2] Valid parameters
            Arguments.of(new StepInput(EXISTING_FILE_ID,
                                       EXISTING_FILE_ID + "," + EXISTING_FILE_ID,
                                       1,
                                       VersionRule.HIGHER.toString()),
                         null, false, ""),

            // [3] Max descriptor size exceeded
            Arguments.of(new StepInput(EXISTING_FILE_ID, EXISTING_BIGGER_FILE_ID, 1, VersionRule.HIGHER.toString()),
                         MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.ERROR_SIZE_OF_FILE_EXCEEDS_CONFIGURED_MAX_SIZE_LIMIT,
                                              "1048577", "extDescriptorFile", "1048576"),
                         false, ""),

            // [4] Process chunked file
            Arguments.of(new StepInput(MERGED_ARCHIVE_NAME + ".part.0," + MERGED_ARCHIVE_NAME + ".part.1,"
                                           + MERGED_ARCHIVE_NAME + ".part.2", null, 1, VersionRule.HIGHER.toString()), null, true, ""),

            // [5] Max size of entries exceeded
            Arguments.of(new StepInput(EXCEEDING_FILE_SIZE_ID + ".part.0," + EXCEEDING_FILE_SIZE_ID + ".part.1,"
                                           + EXCEEDING_FILE_SIZE_ID + ".part.2," + EXCEEDING_FILE_SIZE_ID + ".part.3,"
                                           + EXCEEDING_FILE_SIZE_ID
                                           + ".part.4", null, 1, VersionRule.HIGHER.toString()),
                         MessageFormat.format(Messages.SIZE_OF_ALL_OPERATIONS_FILES_0_EXCEEDS_MAX_UPLOAD_SIZE_1, 5368709120L,
                                              4294967296L),
                         true, ""));
    }

    private static FileEntry createFileEntry(String id, String name, long size) {
        return ImmutableFileEntry.builder()
                                 .id(id)
                                 .name(name)
                                 .size(BigInteger.valueOf(size))
                                 .operationId("test")
                                 .build();
    }

    @MethodSource
    @ParameterizedTest
    void testExecution(StepInput stepInput, String expectedExceptionMessage, boolean isArchiveChunked, String versionOutput)
        throws Exception {
        initializeComponents(stepInput, isArchiveChunked);
        if (expectedExceptionMessage != null) {
            SLException exception = assertThrows(SLException.class, () -> step.execute(execution));
            assertEquals(EXCEPTION_START_MESSAGE + expectedExceptionMessage + versionOutput, exception.getMessage()
                                                                                                      .trim());
            return;
        }
        step.execute(execution);
        validate();
    }

    private void initializeComponents(StepInput stepInput, boolean isArchiveChunked) throws FileStorageException {
        this.stepInput = stepInput;
        this.isArchiveChunked = isArchiveChunked;
        prepareContext();
        prepareFileService(stepInput.appArchiveId);
        prepareArchiveMerger();
        prepareConfiguration();
    }

    private void prepareContext() {
        context.setVariable(Variables.APP_ARCHIVE_ID, stepInput.appArchiveId);
        context.setVariable(Variables.EXT_DESCRIPTOR_FILE_ID, stepInput.extDescriptorId);
        context.setVariable(Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE, stepInput.startTimeout);
        execution.setVariable(Variables.VERSION_RULE.getName(), stepInput.versionRule);
        context.setVariable(Variables.SPACE_GUID, "space-id");
        context.setVariable(Variables.SERVICE_ID, "service-id");
        context.setVariable(Variables.MTA_NAMESPACE, "namespace");
    }

    private void prepareFileService(String appArchiveId) throws FileStorageException {
        when(fileService.getFile("space-id", EXISTING_FILE_ID))
            .thenReturn(createFileEntry(EXISTING_FILE_ID, "some-file-entry-name", 1024 * 1024L));
        when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.0"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.0", MERGED_ARCHIVE_NAME + ".part.0", 1024 * 1024L));

        when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.1"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.1", MERGED_ARCHIVE_NAME + ".part.1", 1024 * 1024L));

        when(fileService.getFile("space-id", MERGED_ARCHIVE_NAME + ".part.2"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.2", MERGED_ARCHIVE_NAME + ".part.2", 1024 * 1024L));

        when(fileService.getFile("space-id", EXCEEDING_FILE_SIZE_ID + ".part.0"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.0", MERGED_ARCHIVE_NAME + ".part.0", 1024 * 1024 * 1024));

        when(fileService.getFile("space-id", EXCEEDING_FILE_SIZE_ID + ".part.1"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.1", MERGED_ARCHIVE_NAME + ".part.1", 1024 * 1024 * 1024));

        when(fileService.getFile("space-id", EXCEEDING_FILE_SIZE_ID + ".part.2"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.2", MERGED_ARCHIVE_NAME + ".part.2", 1024 * 1024 * 1024));

        when(fileService.getFile("space-id", EXCEEDING_FILE_SIZE_ID + ".part.3"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.3", MERGED_ARCHIVE_NAME + ".part.3", 1024 * 1024 * 1024));

        when(fileService.getFile("space-id", EXCEEDING_FILE_SIZE_ID + ".part.4"))
            .thenReturn(createFileEntry(MERGED_ARCHIVE_NAME + ".part.4", MERGED_ARCHIVE_NAME + ".part.4", 1024 * 1024 * 1024));

        when(fileService.getFile("space-id", EXISTING_BIGGER_FILE_ID))
            .thenReturn(createFileEntry(EXISTING_BIGGER_FILE_ID, "extDescriptorFile", 1024 * 1024L + 1));
        when(fileService.getFile("space-id", NOT_EXISTING_FILE_ID))
            .thenReturn(null);
        when(fileService.addFile(any(FileEntry.class), any(InputStream.class)))
            .thenReturn(createFileEntry(EXISTING_FILE_ID, MERGED_ARCHIVE_TEST_MTAR, 1024 * 1024 * 1024L));
        if (appArchiveId.contains(EXCEEDING_FILE_SIZE_ID)) {
            List<FileEntry> fileEntries = List.of(createFileEntry(EXCEEDING_FILE_SIZE_ID + ".part.0", EXCEEDING_FILE_SIZE_ID + ".part.0",
                                                                  1024 * 1024 * 1024),
                                                  createFileEntry(EXCEEDING_FILE_SIZE_ID + ".part.1", EXCEEDING_FILE_SIZE_ID + ".part.1",
                                                                  1024 * 1024 * 1024),
                                                  createFileEntry(EXCEEDING_FILE_SIZE_ID + ".part.2", EXCEEDING_FILE_SIZE_ID + ".part.2",
                                                                  1024 * 1024 * 1024),
                                                  createFileEntry(EXCEEDING_FILE_SIZE_ID + ".part.3", EXCEEDING_FILE_SIZE_ID + ".part.3",
                                                                  1024 * 1024 * 1024),
                                                  createFileEntry(EXCEEDING_FILE_SIZE_ID + ".part.4", EXCEEDING_FILE_SIZE_ID + ".part.4",
                                                                  1024 * 1024 * 1024));
            when(fileService.listFilesBySpaceAndOperationId(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(fileEntries);
        }
    }

    private void prepareArchiveMerger() {
        FilePartsMerger merger = Mockito.mock(FilePartsMerger.class);
        when(merger.getMergedFilePath()).thenReturn(Paths.get(MERGED_ARCHIVE_TEST_MTAR));
    }

    private void prepareConfiguration() {
        when(configuration.getMaxMtaDescriptorSize())
            .thenReturn(ApplicationConfiguration.DEFAULT_MAX_MTA_DESCRIPTOR_SIZE);
        when(configuration.getMaxUploadSize())
            .thenReturn(ApplicationConfiguration.DEFAULT_MAX_UPLOAD_SIZE);
    }

    private void validate() {
        assertStepFinishedSuccessfully();
        if (isArchiveChunked) {
            Path mergedArchiveAbsolutePath = Paths.get(MERGED_ARCHIVE_NAME)
                                                  .toAbsolutePath();
            assertFalse(Files.exists(mergedArchiveAbsolutePath));
        }
        Mockito.verify(execution, Mockito.atLeastOnce())
               .setVariable(Variables.APP_ARCHIVE_ID.getName(), stepInput.appArchiveId);
    }

    @Override
    protected ValidateDeployParametersStep createStep() {
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        when(executorService.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            FutureTask<?> futureTask = new FutureTask<>(callable);
            futureTask.run();
            return futureTask;
        });
        return new ValidateDeployParametersStep(executorService, new FileMimeTypeValidator());
    }

    private static class StepInput {

        private final String appArchiveId;
        private final String extDescriptorId;
        private final Duration startTimeout;
        private final String versionRule;

        public StepInput(String appArchiveId, String extDescriptorId, int startTimeout, String versionRule) {
            this.appArchiveId = appArchiveId;
            this.extDescriptorId = extDescriptorId;
            this.startTimeout = Duration.ofSeconds(startTimeout);
            this.versionRule = versionRule;
        }
    }

}
