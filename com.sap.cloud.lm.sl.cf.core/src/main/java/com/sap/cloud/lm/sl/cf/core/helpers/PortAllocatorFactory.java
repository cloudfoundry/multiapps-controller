package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;

public interface PortAllocatorFactory {

    PortAllocator createPortAllocator(XsCloudControllerClient client, String domain);

}
