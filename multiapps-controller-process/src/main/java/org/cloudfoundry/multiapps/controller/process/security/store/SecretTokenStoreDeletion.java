package org.cloudfoundry.multiapps.controller.process.security.store;

import java.time.LocalDateTime;

public interface SecretTokenStoreDeletion {

    int deleteByProcessInstanceId(String processInstanceId);

    int deleteOlderThan(LocalDateTime expirationTime);

}

