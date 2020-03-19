package com.sap.cloud.lm.sl.cf.core.util;

import java.io.Serializable;
import java.util.Date;

import org.immutables.value.Value;

@Value.Immutable
public interface LogsOffset extends Serializable {

    Date getTimestamp();

    String getMessage();
}
