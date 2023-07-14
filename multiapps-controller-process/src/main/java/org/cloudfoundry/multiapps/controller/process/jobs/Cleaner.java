package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.LocalDateTime;

public interface Cleaner {

    void execute(LocalDateTime expirationTime);

}
