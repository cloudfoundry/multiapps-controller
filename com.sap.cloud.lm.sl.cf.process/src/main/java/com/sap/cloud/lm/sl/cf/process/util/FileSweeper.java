package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;

import org.activiti.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.persistence.services.FileService;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

public class FileSweeper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSweeper.class);

    private final String spaceId;
    private final FileService fileService;

    public FileSweeper(String spaceId, FileService fileService) {
        this.spaceId = spaceId;
        this.fileService = fileService;
    }

    public void sweep(HistoricVariableInstance fileIdsVariable) throws FileStorageException {
        if (fileIdsVariable != null) {
            sweep((String) fileIdsVariable.getValue());
        }
    }

    private void sweepSingle(String fileId) throws FileStorageException {
        LOGGER.info(format(Messages.DELETING_FILE_FROM_SPACE, fileId, this.spaceId));
        fileService.deleteFile(this.spaceId, fileId);
    }

    public void sweep(String fileIds) throws FileStorageException {
        if (fileIds != null) {
            for (String fileId : fileIds.split(",")) {
                sweepSingle(fileId);
            }
        }
    }

}
