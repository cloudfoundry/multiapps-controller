package com.sap.cloud.lm.sl.cf.process.util;

import org.flowable.variable.api.history.HistoricVariableInstance;

import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

public class FileSweeper {

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
