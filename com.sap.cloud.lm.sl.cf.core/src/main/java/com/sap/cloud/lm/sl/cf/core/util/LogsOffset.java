package com.sap.cloud.lm.sl.cf.core.util;

import org.immutables.value.Value;

import java.io.Serializable;
import java.util.Date;

@Value.Immutable
public interface LogsOffset extends Serializable {

    Date getTimestamp();

    String getMessage();
}
