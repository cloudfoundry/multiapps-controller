package org.cloudfoundry.multiapps.controller.process.jobs;

import java.util.Date;

public interface Cleaner {

    void execute(Date expirationTime);

}
