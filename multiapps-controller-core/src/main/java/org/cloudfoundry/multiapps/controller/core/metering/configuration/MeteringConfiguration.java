package org.cloudfoundry.multiapps.controller.core.metering.configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.http.ssl.SSLContexts;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.metering.client.MeteringClient;
import org.cloudfoundry.multiapps.controller.core.metering.model.Credentials;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.PemCertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeteringConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringConfiguration.class);
    private static final String PKCS12 = "PKCS12";
    private static final String CREDENTIALS_FIELD_NAME = "credentials";

    @Bean
    public MeteringClient buildMeteringClient(ApplicationConfiguration applicationConfiguration) {
        Map<String, Object> meteringCredentials = applicationConfiguration.getMeteringCredentials();

        Credentials credentials = new Credentials(MiscUtil.cast(meteringCredentials.get(CREDENTIALS_FIELD_NAME)));
        LOGGER.error(credentials.certificate());
        LOGGER.error(credentials.key());
        return new MeteringClient(credentials, createHttpClient(credentials));
    }

    private CloseableHttpClient createHttpClient(Credentials credentials) {
        HttpClientConnectionManager connectionManager = createConnectionManager(credentials);
        return HttpClients.custom()
                          .setConnectionManager(connectionManager)
                          .build();
    }

    private PoolingHttpClientConnectionManager createConnectionManager(Credentials credentials) {
        SSLContext sslContext = createSSLContext(credentials);
        TlsSocketStrategy tls = ClientTlsStrategyBuilder.create()
                                                        .setSslContext(sslContext)
                                                        .buildClassic();
        return PoolingHttpClientConnectionManagerBuilder.create()
                                                        .setTlsSocketStrategy(tls)
                                                        .build();
    }

    private SSLContext createSSLContext(Credentials credentials) {
        try {
            KeyStore keyStore = createKeyStore(credentials);
            return SSLContexts.custom()
                              .loadKeyMaterial(keyStore, StringUtils.EMPTY.toCharArray())
                              .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new SLException(e);
        }
    }

    private KeyStore createKeyStore(Credentials credentials) throws GeneralSecurityException, IOException {
        X509Certificate[] parsed = PemCertificateUtil.parsePkcs7String(credentials.certificate());
        PrivateKey privateKey = PemCertificateUtil.loadPrivateKey(credentials.key());

        KeyStore ks = KeyStore.getInstance(PKCS12);
        ks.load(null, null);
        ks.setKeyEntry(StringUtils.EMPTY, privateKey, StringUtils.EMPTY.toCharArray(), parsed);

        return ks;
    }
}
