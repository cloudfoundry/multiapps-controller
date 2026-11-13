package org.cloudfoundry.multiapps.controller.client.facade.domain;

public record LastOperation(String type, String state, String description, String createdAt, String updatedAt) {
}
