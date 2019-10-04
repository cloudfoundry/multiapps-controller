package com.sap.cloud.lm.sl.cf.client.lib.domain;

import org.cloudfoundry.client.lib.UploadStatusCallback;

public interface UploadStatusCallbackExtended extends UploadStatusCallback {

    void onError(Exception e);

}
