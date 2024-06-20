package org.cloudfoundry.multiapps.controller.client.uaa;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URL;

public class UAAClientFactory {
    public UAAClient createClient(URL uaaUrl) {
       return new UAAClient(uaaUrl, buildWebClientWith(true));
    }

    private WebClient buildWebClientWith(Boolean shouldSkipSslValidation) {
        if (!shouldSkipSslValidation) {
            return WebClient.create();
        }
        final SslContext sslContext;
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException("Failed to create insecure SSL context", e);
        }
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
