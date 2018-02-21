package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;

public interface PortAllocatorFactory {

    PortAllocator createPortAllocator(ClientExtensions client, String domain);

}
