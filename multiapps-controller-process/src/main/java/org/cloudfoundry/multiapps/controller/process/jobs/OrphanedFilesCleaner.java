package org.cloudfoundry.multiapps.controller.process.jobs;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Named
public class OrphanedFilesCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedFilesCleaner.class);
    private static final int SELECTED_INSTANCE_FOR_CLEANUP = 1;

    private final FileService fileService;
    private final OperationService operationService;
    private final FlowableFacade flowableFacade;
    private final ApplicationConfiguration configuration;

    @Inject
    public OrphanedFilesCleaner(FileService fileService, OperationService operationService,
                                FlowableFacade flowableFacade, ApplicationConfiguration config) {
        this.fileService = fileService;
        this.operationService = operationService;
        this.flowableFacade = flowableFacade;
        this.configuration = config;
    }

    //this is quite inefficient because the Operation table does not contain any info on which archive
    //it is started with
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void run() {
        if (configuration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEANUP) {
            return;
        }

        var timestamp = LocalDateTime.now();
        var oneHourAgo = timestamp.minusHours(1);
        try {
            LOGGER.debug(MessageFormat.format(Messages.GETTING_FILES_CREATED_AFTER_0, oneHourAgo));
            var files = fileService.listFilesCreatedAfter(oneHourAgo);
            var fileIdsToFiles = files.stream()
                                      .collect(Collectors.toMap(FileEntry::getId, entry -> entry));

            var historicVariables = getHistoricAppArchiveIDs(oneHourAgo);

            filterFilesWithStartedOperations(fileIdsToFiles, historicVariables);

            if (fileIdsToFiles.isEmpty()) {
                LOGGER.info(Messages.NO_ORPHANED_FILES_TO_DELETE);
                return;
            }
            LOGGER.debug(MessageFormat.format(Messages.DELETING_ORPHANED_FILES_0, fileIdsToFiles.size(), fileIdsToFiles.keySet()));
            for (var orphanedFile : fileIdsToFiles.values()) {
                fileService.deleteFile(orphanedFile.getSpace(), orphanedFile.getId());
            }
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_ORPHANED_FILES_MODIFIED_AFTER_0, oneHourAgo);
        }
    }

    private List<String> getHistoricAppArchiveIDs(LocalDateTime startedAfter) {
        LOGGER.debug(MessageFormat.format(Messages.GETTING_OPERATIONS_STARTED_AFTER_0, startedAfter));
        var operations = operationService.createQuery()
                                         .startedAfter(startedAfter)
                                         .list();

        LOGGER.debug(MessageFormat.format(Messages.GETTING_HISTORIC_VARIABLES_FOR_OPERATIONS_STARTED_AFTER_0, startedAfter));
        List<String> result = new LinkedList<>();
        for (var operation : operations) {
            var historicVariable = flowableFacade.getHistoricVariableInstance(operation.getProcessId(),
                                                                              Variables.APP_ARCHIVE_ID.getName());
            result.add(String.valueOf(historicVariable.getValue()));
        }
        return result;
    }

    private void filterFilesWithStartedOperations(Map<String, FileEntry> files, List<String> historicVars) {
        var fileIDs = files.keySet();
        for (var appArchiveId : historicVars) {
            if (fileIDs.contains(appArchiveId)) {
                files.remove(appArchiveId);
            }
        }
    }
}
