package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSweeper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSweeper.class);

    private final String spaceId;
    private final FileService fileService;
    private final String operationId;

    public FileSweeper(String spaceId, FileService fileService, String operationId) {
        this.spaceId = spaceId;
        this.fileService = fileService;
        this.operationId = operationId;
    }

    public void sweep(String fileIds) throws FileStorageException {
        if (fileIds != null) {
            for (String fileId : fileIds.split(",")) {
                sweepSingle(fileId);
            }
        }
    }

    public void sweep(List<FileEntry> fileEntries) throws FileStorageException {
        for (FileEntry fileEntry : fileEntries) {
            sweepFileEntry(fileEntry);
        }
    }

    private void sweepSingle(String fileId) throws FileStorageException {
        FileEntry fileEntry = fileService.getFile(spaceId, fileId);
        sweepFileEntry(fileEntry);
    }

    private void sweepFileEntry(FileEntry fileEntry) throws FileStorageException {
        if (operationId.equals(fileEntry.getOperationId())) {
            fileService.deleteFile(spaceId, fileEntry.getId());
            LOGGER.info(MessageFormat.format(Messages.FILE_WITH_ID_0_WAS_DELETED, fileEntry.getId()));
            return;
        }
        LOGGER.warn(MessageFormat.format(Messages.FILE_WITH_ID_0_OPERATION_OWNERSHIP_CHANGED_FROM_0_TO_1, fileEntry.getId(), operationId,
                                         fileEntry.getOperationId()));
    }

}
