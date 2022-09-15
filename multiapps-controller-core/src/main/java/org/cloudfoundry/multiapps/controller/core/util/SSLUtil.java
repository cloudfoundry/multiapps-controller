package org.cloudfoundry.multiapps.controller.core.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLUtil {

    private SSLUtil() {
    }

    private static final X509TrustManager NULL_TRUST_MANAGER = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
            // NOSONAR
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) {
            // NOSONAR
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
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

}
