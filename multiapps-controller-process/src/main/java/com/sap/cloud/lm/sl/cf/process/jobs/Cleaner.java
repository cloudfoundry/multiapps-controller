package com.sap.cloud.lm.sl.cf.process.jobs;

import java.util.Date;

public interface Cleaner {

    void execute(Date expirationTime);

}
