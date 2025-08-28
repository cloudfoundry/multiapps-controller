package org.cloudfoundry.multiapps.controller.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class PemCertificateUtils {

    private static final String JKS = "JKS";
    private static final Logger LOGGER = LoggerFactory.getLogger(PemCertificateUtils.class);

    public static KeyStore generateKeyStore(String certificate1, String privateKey) {
        try {
            Certificate certificate = loadCertificate(certificate1);

            KeyStore keyStore = KeyStore.getInstance(JKS);

            keyStore.load(null, EMPTY.toCharArray());
            keyStore.setKeyEntry(extractCommonName((X509Certificate) certificate), createPrivateKey(privateKey, EMPTY),
                                 EMPTY.toCharArray(), ArrayUtils.toArray(certificate));

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Could not load certificate", e);
        }
    }

    private static Certificate loadCertificate(String certificatePem) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            //final byte[] content = readCertificateBytes(certificatePem);

            return certificateFactory.generateCertificate(new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Could not load certificate", e);
        }
    }

    private static String extractCommonName(X509Certificate certificate) throws Exception {
        X500Name x500Name = new JcaX509CertificateHolder(certificate).getSubject();
        RDN CN = x500Name.getRDNs(BCStyle.CN)[0];

        return IETFUtils.valueToString(CN.getFirst()
                                         .getValue());
    }

    private static byte[] readCertificateBytes(String certificate) {
        try (PemReader pemReader = new PemReader(new StringReader(certificate))) {
            return pemReader.readPemObject()
                            .getContent();
        } catch (IOException e) {
            throw new RuntimeException("Could not load certificate", e);
        }
    }

    private static PrivateKey createPrivateKey(String privateKey, String password) throws IOException, PKCSException {
        Object privateKeyObject = new PEMParser(new StringReader(privateKey)).readObject();

        JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
        if (privateKeyObject instanceof PEMKeyPair) {
            PEMKeyPair pemKeyPair = (PEMKeyPair) privateKeyObject;
            KeyPair keyPair = jcaPEMKeyConverter.getKeyPair(pemKeyPair);
            return keyPair.getPrivate();
        } else if (privateKeyObject instanceof PrivateKeyInfo) {
            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) privateKeyObject;
            return jcaPEMKeyConverter.getPrivateKey(privateKeyInfo);
        } else if (privateKeyObject instanceof PKCS8EncryptedPrivateKeyInfo) {
            PKCS8EncryptedPrivateKeyInfo privateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) privateKeyObject;

            JcePKCSPBEInputDecryptorProviderBuilder builder = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("BC");
            InputDecryptorProvider decryptionProvider = builder.build(password.toCharArray());

            return jcaPEMKeyConverter.getPrivateKey(privateKeyInfo.decryptPrivateKeyInfo(decryptionProvider));
        } else {
            throw new IllegalArgumentException("Unsupported private key format '" + privateKeyObject.getClass()
                                                                                                    .getSimpleName() + '"');
        }
    }
}
