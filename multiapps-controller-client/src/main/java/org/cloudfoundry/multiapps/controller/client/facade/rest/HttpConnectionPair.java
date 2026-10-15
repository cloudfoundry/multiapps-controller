package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URL;

import reactor.netty.http.client.HttpClient;

public record HttpConnectionPair(HttpClient httpClient, URL v3ApiUrl, URL logCacheUrl) {
}
