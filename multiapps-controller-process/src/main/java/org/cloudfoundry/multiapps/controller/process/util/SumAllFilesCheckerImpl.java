package org.cloudfoundry.multiapps.controller.process.util;

import org.flowable.engine.delegate.DelegateExecution;

import javax.inject.Named;

@Named
public class SumAllFilesCheckerImpl implements SumAllFilesChecker {

    @Override
    public boolean shouldSumAllTheFiles(DelegateExecution execution) {
        return true;
    }
}
