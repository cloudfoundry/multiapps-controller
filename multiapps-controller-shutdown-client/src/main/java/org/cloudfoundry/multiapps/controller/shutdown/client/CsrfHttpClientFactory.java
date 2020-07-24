package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.http.CsrfHttpClient;

interface CsrfHttpClientFactory {

    CsrfHttpClient create(Map<String, String> defaultHttpHeaders);

}