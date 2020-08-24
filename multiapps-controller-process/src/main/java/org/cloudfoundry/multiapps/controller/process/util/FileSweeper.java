package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;

public class FileSweeper {

    private final String spaceId;
    private final FileService fileService;

    public FileSweeper(String spaceId, FileService fileService) {
        this.spaceId = spaceId;
        this.fileService = fileService;
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
