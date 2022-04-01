package com.sap.cloud.lm.sl.cf.core.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

public class SSLUtil {

    private static final X509TrustManager NULL_TRUST_MANAGER = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
            // Do nothing.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) {
            // Do nothing.
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    };

    public static void disableSSLValidation() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { NULL_TRUST_MANAGER }, null);
            SSLContext.setDefault(context);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void useCaCertificateValidation(String certificatePath) {
        InputStream is = null;
        try {
            is = new FileInputStream(certificatePath);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            X509Certificate caCertificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                                                                                .generateCertificate(is);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("caCert", caCertificate);

            trustManagerFactory.init(keyStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagerFactory.getTrustManagers(), null);
            SSLContext.setDefault(context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
