package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;

import java.io.InputStream;
import java.util.Map;

public class ExternalFileProcessor {

    private ContentLengthTracker sizeTracker;
    private final FileService fileService;
    private long maxMtaFileSize;

    public ExternalFileProcessor(ContentLengthTracker sizeTracker, long maxMtaFileSize, FileService fileService) {
        this.sizeTracker = sizeTracker;
        this.maxMtaFileSize = maxMtaFileSize;
        this.fileService = fileService;
    }

    public Map<String, Object> processFileContent(String space, String appArchiveId, String fileName) {
        try {
            return fileService.processFileContent(space, appArchiveId, input -> getFileContent(input, fileName));
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_GET_FILE_CONTENT_FOR_0, fileName);
        }
    }

    private Map<String, Object> getFileContent(InputStream appArchiveStream, String fileName) {
        byte[] fileContent = ArchiveHandler.getFileContent(appArchiveStream, fileName, maxMtaFileSize);
        sizeTracker.setFileSize(fileContent.length);
        return JsonUtil.convertJsonToMap(new String(fileContent));
    }
}
