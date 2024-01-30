package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class OrphanedFilesCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedFilesCleaner.class);
    private static final int SELECTED_INSTANCE_FOR_CLEANUP = 1;

    private final FileService fileService;
    private final OperationService operationService;
    private final FlowableFacade flowableFacade;
    private final ApplicationConfiguration configuration;

    @Inject
    public OrphanedFilesCleaner(FileService fileService, OperationService operationService, FlowableFacade flowableFacade,
                                ApplicationConfiguration config) {
        this.fileService = fileService;
        this.operationService = operationService;
        this.flowableFacade = flowableFacade;
        this.configuration = config;
    }

    // this is quite inefficient because the Operation table does not contain any info on which archive
    // it is started with
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void run() {
        if (configuration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEANUP) {
            return;
        }

        var timestamp = LocalDateTime.now();
        var twoHoursAgo = timestamp.minusHours(2);
        var oneHourAgo = timestamp.minusHours(1);
        try {
            LOGGER.info(MessageFormat.format(Messages.GETTING_FILES_CREATED_AFTER_0_AND_BEFORE_1, twoHoursAgo, oneHourAgo));
            var files = fileService.listFilesCreatedAfterAndBefore(twoHoursAgo, oneHourAgo);
            var fileIdsToFiles = files.stream()
                                      .collect(Collectors.toMap(FileEntry::getId, Function.identity()));

            var archiveIdsWithAssociatedOperation = getHistoricAppArchiveIDs(twoHoursAgo);
            filterFilesWithStartedOperations(fileIdsToFiles, archiveIdsWithAssociatedOperation);

            if (fileIdsToFiles.isEmpty()) {
                LOGGER.info(Messages.NO_ORPHANED_FILES_TO_DELETE);
                return;
            }
            LOGGER.info(MessageFormat.format(Messages.DELETING_ORPHANED_FILES_0, fileIdsToFiles.size(), fileIdsToFiles.keySet()));
            fileIdsToFiles.forEach((fileId, file) -> deleteFileSafely(file));
        } catch (Exception e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_ORPHANED_FILES_MODIFIED_AFTER_0_AND_BEFORE_1, twoHoursAgo, oneHourAgo);
        }
    }

    private List<String> getHistoricAppArchiveIDs(LocalDateTime startedAfter) {
        LOGGER.debug(MessageFormat.format(Messages.GETTING_OPERATIONS_STARTED_AFTER_0, startedAfter));
        var operations = operationService.createQuery()
                                         .startedAfter(startedAfter)
                                         .list();
        LOGGER.debug(MessageFormat.format(Messages.GETTING_HISTORIC_VARIABLES_FOR_OPERATIONS_STARTED_AFTER_0, startedAfter));
        return operations.stream()
                         .map(operation -> flowableFacade.getHistoricVariableInstance(operation.getProcessId(),
                                                                                      Variables.APP_ARCHIVE_ID.getName()))
                         .map(HistoricVariableInstance::getValue)
                         .map(String::valueOf)
                         .flatMap(appArchiveId -> Arrays.stream(appArchiveId.split(",")))
                         .collect(Collectors.toList());
    }

    private void filterFilesWithStartedOperations(Map<String, FileEntry> files, List<String> historicAppArchiveIds) {
        for (var appArchiveId : historicAppArchiveIds) {
            files.remove(appArchiveId);
        }
    }

    private void deleteFileSafely(FileEntry orphanedFile) {
        try {
            fileService.deleteFile(orphanedFile.getSpace(), orphanedFile.getId());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
