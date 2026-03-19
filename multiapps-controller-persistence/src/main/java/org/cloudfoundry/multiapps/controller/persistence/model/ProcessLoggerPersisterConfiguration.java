package org.cloudfoundry.multiapps.controller.persistence.model;

public record ProcessLoggerPersisterConfiguration(String correlationId, String taskId, LoggingConfiguration loggingConfiguration,
                                                  boolean isExternalLoggingConfigurationEnabled) {
}
