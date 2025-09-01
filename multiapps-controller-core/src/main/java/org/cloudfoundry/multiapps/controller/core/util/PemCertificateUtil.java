package org.cloudfoundry.multiapps.controller.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.MiscUtil;

public class PemCertificateUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String PKCS7_ENCODING = "PKCS7";
    private static final String X509_TYPE = "X.509";

    private PemCertificateUtil() {
    }

    public static X509Certificate[] parsePkcs7String(String pkcs7) {
        byte[] decodedCertificate = decodeBase64Certificate(pkcs7);
        List<X509Certificate> certificates = new ArrayList<>();

        try (InputStream decodedCertificateStream = new ByteArrayInputStream(decodedCertificate)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(X509_TYPE);
            CertPath certPath = certificateFactory.generateCertPath(decodedCertificateStream, PKCS7_ENCODING);

            for (Certificate certificate : certPath.getCertificates()) {
                certificates.add(MiscUtil.cast(certificate));
            }

            return certificates.toArray(new X509Certificate[0]);
        } catch (CertificateException | IOException e) {
            throw new SLException(e);
        }
    }

    public static PrivateKey loadPrivateKey(String privateKey) {
        try {
            byte[] decodedPrivateKey = decodeBase64Certificate(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
            return KeyFactory.getInstance(RSA_ALGORITHM)
                             .generatePrivate(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new SLException(e);
        }
    }

    private static byte[] decodeBase64Certificate(String certificate) {
        String trimmedCertificate = certificate.trim();

        String onlyBase64Certificate = trimmedCertificate
            .replaceAll("-----BEGIN [A-Z0-9 \\-]+-----", "")
            .replaceAll("-----END [A-Z0-9 \\-]+-----", "")
            .replaceAll("\\s", "");
        return Base64.getDecoder()
                     .decode(onlyBase64Certificate);
    }
}
