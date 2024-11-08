package org.cloudfoundry.multiapps.controller.persistence.query.options;

public record StreamFetchingOptions(long startOffset, long endOffset) {
}
