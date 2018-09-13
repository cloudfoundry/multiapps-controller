package com.sap.cloud.lm.sl.cf.process.util;

import org.activiti.engine.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

public class FileSweeper {

    private final String spaceId;
    private final AbstractFileService fileService;

    public FileSweeper(String spaceId, AbstractFileService fileService) {
        this.spaceId = spaceId;
        this.fileService = fileService;
    }

    public void sweep(HistoricVariableInstance fileIdsVariable) throws FileStorageException {
        if (fileIdsVariable != null) {
            sweep((String) fileIdsVariable.getValue());
        }
    }

    private void sweepSingle(String fileId) throws FileStorageException {
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
