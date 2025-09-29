package org.cloudfoundry.multiapps.controller.web.upload.client;

import java.io.InputStream;
import java.net.URI;

public record FileFromUrlData(InputStream fileInputStream, URI uri, long fileSize) {
}
