package com.sap.cloud.lm.sl.cf.shutdown.client;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.http.CsrfHttpClient;

interface CsrfHttpClientFactory {

    CsrfHttpClient create(Map<String, String> defaultHttpHeaders);

}