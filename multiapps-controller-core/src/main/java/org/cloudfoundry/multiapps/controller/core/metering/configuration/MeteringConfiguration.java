package org.cloudfoundry.multiapps.controller.core.metering.configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.metering.client.MeteringClient;
import org.cloudfoundry.multiapps.controller.core.metering.model.Credentials;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Configuration
public class MeteringConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringConfiguration.class);

    @Bean
    public MeteringClient buildMeteringClient(ApplicationConfiguration applicationConfiguration) throws Exception {
        List<Object> meteringCredentialsList = JsonUtil.convertJsonStringToObject(applicationConfiguration.getMeteringCredentials(),
                                                                                  List.class);
        Map<String, Object> meteringCredentials = JsonUtil.convertJsonStringToObject(meteringCredentialsList.get(0),
                                                                                     Map.class);

        Credentials credentials = new Credentials((Map<String, Object>) meteringCredentials.get("credentials"));
        LOGGER.error(credentials.certificate());
        LOGGER.error(credentials.key());
        return new MeteringClient(credentials, createHttpClient(credentials));
    }

    private CloseableHttpClient createHttpClient(Credentials credentials) throws Exception {
        List<X509Certificate> parsed = parsePkcs7String(credentials.certificate());
        PrivateKey privateKey = loadPrivateKey(credentials.key());
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        X509Certificate[] certs = new X509Certificate[parsed.size()];
        parsed.toArray(certs);
        ks.setKeyEntry(extractCommonName(parsed.get(0)), privateKey, EMPTY.toCharArray(), certs);

        PoolingHttpClientConnectionManager cm = null;
        try {
            cm = PoolingHttpClientConnectionManagerBuilder.create()
                                                          .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                                                                                                .setSslContext(
                                                                                                                    buildSSLContext(ks))
                                                                                                                .build()
                                                          )
                                                          .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return HttpClients.custom()
                          .setConnectionManager(cm)
                          .build();
    }

    private static SSLContext buildSSLContext(KeyStore keyStore) {
        try {
            return SSLContexts.custom() //
                              .loadKeyMaterial(keyStore, EMPTY.toCharArray()) //
                              .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ssl context", e);
        }
    }

    private static String extractCommonName(X509Certificate certificate) throws Exception {
        X500Name x500Name = new JcaX509CertificateHolder(certificate).getSubject();
        RDN CN = x500Name.getRDNs(BCStyle.CN)[0];

        return IETFUtils.valueToString(CN.getFirst()
                                         .getValue());
    }

    public static PrivateKey loadPrivateKey(String pem) throws Exception {
        String s = pem.trim();
        byte[] pkcs8 = extractPemBody(s, "PRIVATE KEY");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8);
        return KeyFactory.getInstance(detectKeyAlgorithm(pkcs8))
                         .generatePrivate(keySpec);
    }

    private static String detectKeyAlgorithm(byte[] pkcs8) {
        // Look for common OIDs inside the AlgorithmIdentifier
        String s = Base64.getEncoder()
                         .encodeToString(pkcs8);
        if (s.contains("AQAB")) { /* weak hint for RSA public exponent present later */ }
        // quick-and-dirty OID fingerprints:
        // 1.2.840.113549.1.1.1 rsaEncryption
        if (indexOf(pkcs8, new byte[] { 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01 }) >= 0)
            return "RSA";
        // 1.2.840.10045.2.1 ecPublicKey
        if (indexOf(pkcs8, new byte[] { 0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01 }) >= 0)
            return "EC";
        // 1.2.840.113549.1.1.10 RSASSA-PSS (still RSA keys)
        if (indexOf(pkcs8, new byte[] { 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x0a }) >= 0)
            return "RSA";
        return "RSA"; // default; adjust if you expect EC keys
    }

    private static int indexOf(byte[] a, byte[] b) {
        outer:
        for (int i = 0; i <= a.length - b.length; i++) {
            for (int j = 0; j < b.length; j++)
                if (a[i + j] != b[j])
                    continue outer;
            return i;
        }
        return -1;
    }

    private static byte[] extractPemBody(String pem, String type) {
        String re = "-----BEGIN " + type + "-----([\\s\\S]*?)-----END " + type + "-----";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(re)
                                                           .matcher(pem);
        if (!m.find())
            throw new IllegalArgumentException("PEM block not found: " + type);
        String b64 = m.group(1)
                      .replaceAll("\\s", "");
        return Base64.getDecoder()
                     .decode(b64);
    }

    public static List<X509Certificate> parsePkcs7String(String pkcs7) throws Exception {
        byte[] bytes = decodePemOrBase64(pkcs7);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // First try as explicit PKCS7 CertPath
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            CertPath cp = cf.generateCertPath(in, "PKCS7");
            List<X509Certificate> list = new ArrayList<>();
            for (Certificate c : cp.getCertificates()) {
                list.add((X509Certificate) c);
            }
            if (!list.isEmpty())
                return list;
        } catch (CertificateException ignored) {
            // Fall back below
        }

        // Fallback: let generateCertificates parse a collection (also handles PKCS7)
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            @SuppressWarnings("unchecked")
            Collection<X509Certificate> col =
                (Collection<X509Certificate>) (Collection<?>) cf.generateCertificates(in);
            return new ArrayList<>(col);
        }
    }

    private static byte[] decodePemOrBase64(String s) {
        String trimmed = s.trim();

        // Handle PEM headers for PKCS7 or CERTIFICATE
        if (trimmed.contains("-----BEGIN")) {
            String base64 = trimmed
                .replaceAll("-----BEGIN [A-Z0-9 \\-]+-----", "")
                .replaceAll("-----END [A-Z0-9 \\-]+-----", "")
                .replaceAll("\\s", "");
            return Base64.getDecoder()
                         .decode(base64);
        }

        // Try Base64 decode directly
        try {
            return Base64.getDecoder()
                         .decode(trimmed.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            // As a last resort, interpret as raw bytes of the string
            return trimmed.getBytes(StandardCharsets.ISO_8859_1);
        }
    }
}
