package org.cloudfoundry.multiapps.controller.client.lib.domain;

import com.sap.cloudfoundry.client.facade.UploadStatusCallback;

public interface UploadStatusCallbackExtended extends UploadStatusCallback {

    void onError(Exception e);

}
