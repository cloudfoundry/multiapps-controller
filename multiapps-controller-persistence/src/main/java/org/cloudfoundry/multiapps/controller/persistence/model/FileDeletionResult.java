package org.cloudfoundry.multiapps.controller.persistence.model;

public record FileDeletionResult(int deletedFromObjectStore, int deletedFromDatabase) {
}
