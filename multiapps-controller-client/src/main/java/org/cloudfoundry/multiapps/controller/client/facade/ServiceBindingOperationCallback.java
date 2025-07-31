package org.cloudfoundry.multiapps.controller.client.facade;

import java.util.UUID;

public interface ServiceBindingOperationCallback {

    void onError(CloudOperationException e, UUID serviceBindingGuid);
}
