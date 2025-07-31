package org.cloudfoundry.multiapps.controller.client.facade;

import java.util.Set;

/**
 * Reports status information when uploading an application.
 */
public interface UploadStatusCallback {

    /**
     * Empty implementation
     */
    UploadStatusCallback NONE = new UploadStatusCallback() {
        @Override
        public void onCheckResources() {
        }

        @Override
        public void onMatchedFileNames(Set<String> matchedFileNames) {
        }

        @Override
        public void onProcessMatchedResources(int length) {
        }

        @Override
        public boolean onProgress(String status) {
            return false;
        }

        @Override
        public void onError(String description) {
        }
    };

    /**
     * Called after the /resources call is made.
     */
    void onCheckResources();

    /**
     * Called after the files to be uploaded have been identified.
     *
     * @param matchedFileNames the files to be uploaded
     */
    void onMatchedFileNames(Set<String> matchedFileNames);

    /**
     * Called after the data to be uploaded has been processed
     *
     * @param length the size of the upload data (before compression)
     */
    void onProcessMatchedResources(int length);

    /**
     * Called during asynchronous upload process.
     *
     * Implementation can return true to unsubscribe from progress update reports. This is useful if the caller want to unblock the thread
     * that initiated the upload. Note, however, that the upload job that has been asynchronously started will continue to execute on the
     * server.
     *
     * @param status string such as "queued", "finished"
     * @return true to unsubscribe from update report
     */
    boolean onProgress(String status);

    void onError(String description);
}
