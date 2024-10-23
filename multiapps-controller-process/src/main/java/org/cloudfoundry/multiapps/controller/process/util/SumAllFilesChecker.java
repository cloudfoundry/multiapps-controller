package org.cloudfoundry.multiapps.controller.process.util;

import org.flowable.engine.delegate.DelegateExecution;

public interface SumAllFilesChecker {

    boolean shouldSumAllTheFiles(DelegateExecution execution);
}
