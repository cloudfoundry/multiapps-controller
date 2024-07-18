package org.cloudfoundry.multiapps.controller.web.monitoring;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.concurrent.PriorityBlockingQueue;

@Named
public class FileUploadThreadPoolInformation {

    private final PriorityBlockingQueue<Runnable> fileUploadPriorityBlockingQueue;

    @Inject
    public FileUploadThreadPoolInformation(PriorityBlockingQueue<Runnable> fileUploadPriorityBlockingQueue) {
        this.fileUploadPriorityBlockingQueue = fileUploadPriorityBlockingQueue;
    }

    public int getFileUploadPriorityBlockingQueueSize() {
        return fileUploadPriorityBlockingQueue.size();
    }
}
