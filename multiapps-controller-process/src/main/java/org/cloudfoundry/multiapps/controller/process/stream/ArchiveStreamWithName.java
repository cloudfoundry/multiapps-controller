package org.cloudfoundry.multiapps.controller.process.stream;

import java.io.InputStream;

import org.immutables.value.Value;

@Value.Immutable
public interface ArchiveStreamWithName {

    String getArchiveName();

    InputStream getArchiveStream();
}
