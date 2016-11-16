package com.sap.cloud.lm.sl.cf.core.helpers;

import org.cloudfoundry.client.lib.CloudFoundryOperations;

public interface PortAllocatorFactory {
    
    PortAllocator createPortAllocator(CloudFoundryOperations client, String domain);

}
