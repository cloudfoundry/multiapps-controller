package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.concurrent.LinkedBlockingQueue;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class FileUploadFromUrlThreadPoolInformation {

    private final LinkedBlockingQueue<Runnable> fileUploadFromUrlQueue;

    @Inject
    public FileUploadFromUrlThreadPoolInformation(LinkedBlockingQueue<Runnable> fileUploadFromUrlQueue) {
        this.fileUploadFromUrlQueue = fileUploadFromUrlQueue;
    }

    public int getFileUploadFromUrlQueueSize() {
        return fileUploadFromUrlQueue.size();
    }

}
