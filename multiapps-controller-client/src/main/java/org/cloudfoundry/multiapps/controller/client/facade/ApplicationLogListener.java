package org.cloudfoundry.multiapps.controller.client.facade;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ApplicationLog;

public interface ApplicationLogListener {

    void onComplete();

    void onError(Throwable exception);

    void onMessage(ApplicationLog log);

}
