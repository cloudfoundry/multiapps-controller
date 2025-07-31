package org.cloudfoundry.multiapps.controller.client.lib.domain;

import org.cloudfoundry.multiapps.controller.client.facade.UploadStatusCallback;

public interface UploadStatusCallbackExtended extends UploadStatusCallback {

    void onError(Exception e);

}
