package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.util.concurrent.PriorityBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;

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
