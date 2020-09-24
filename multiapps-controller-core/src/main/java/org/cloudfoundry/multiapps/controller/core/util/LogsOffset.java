package org.cloudfoundry.multiapps.controller.core.util;

import java.io.Serializable;
import java.util.Date;

import org.immutables.value.Value;

@Value.Immutable
public abstract class LogsOffset implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract Date getTimestamp();

    public abstract String getMessage();
}
