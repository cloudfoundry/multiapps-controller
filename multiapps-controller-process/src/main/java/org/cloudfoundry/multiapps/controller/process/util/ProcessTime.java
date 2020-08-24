package org.cloudfoundry.multiapps.controller.process.util;

import org.immutables.value.Value;

@Value.Immutable
public interface ProcessTime {

    long getProcessDuration();

    long getDelayBetweenSteps();
}
