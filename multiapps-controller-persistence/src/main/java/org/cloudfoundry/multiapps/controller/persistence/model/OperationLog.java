package org.cloudfoundry.multiapps.controller.persistence.model;

import java.time.LocalDateTime;

public record OperationLog(String log, LocalDateTime dateTime) {

}
