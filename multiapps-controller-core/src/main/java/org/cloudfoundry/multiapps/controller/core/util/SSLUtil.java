package org.cloudfoundry.multiapps.controller.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

public class SSLUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSLUtil.class);

    private SSLUtil() {
    }

    private static final X509TrustManager NULL_TRUST_MANAGER = new X509ExtendedTrustManager(){

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            LOGGER.info("==checking checkServerTrusted X509Certificate chain with Socket");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
            // NOSONAR
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) {
            LOGGER.info("==checking checkServerTrusted with X509Certificate");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

    };

    public static void disableSSLValidation() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { NULL_TRUST_MANAGER }, null);
            SSLContext.setDefault(context);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("==returning from disableSSLValidation");
    }

    public static SSLContext disableSSLCertValidation() {
        SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { NULL_TRUST_MANAGER }, null);

        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        return context;
    }

}
