package org.cloudfoundry.multiapps.controller.process.security.store;

import java.time.LocalDateTime;

public interface SecretTokenStoreDeletion {

    void deleteByProcessInstanceId(String processInstanceId);

    int deleteOlderThan(LocalDateTime expirationTime);

}

