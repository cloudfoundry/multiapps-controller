package org.cloudfoundry.multiapps.controller.client.facade.domain;

/**
 * Project-owned view of a Cloud Controller v3 resource's {@code last_operation} (raw string fields), replacing
 * {@code org.cloudfoundry.client.v3.LastOperation} so the domain no longer depends on the OSS cf-java-client. All fields are the raw CF
 * wire values; any may be {@code null}.
 */
public record LastOperation(String type, String state, String description, String createdAt, String updatedAt) {
}
