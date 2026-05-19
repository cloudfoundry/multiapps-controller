package org.cloudfoundry.multiapps.controller.persistence.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PEMToEncodedKeyConverterTest {

    private final PEMToEncodedKeyConverter converter = new PEMToEncodedKeyConverter();

    @Test
    void testReturnsPrivateKeyEncodingForPEMKeyPair() throws Exception {
        String pem = generateRsaKeyPairPem();

        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            byte[] result = converter.getPrivateEncodedKey(parser);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.length > 0);
        }
    }

    @Test
    void testThrowsForUnsupportedPEMObject() throws Exception {
        String pem = "-----BEGIN CERTIFICATE-----\n"
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvBC9X+2Zg99Q==\n"
            + "-----END CERTIFICATE-----\n";

        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Assertions.assertThrows(Exception.class, () -> converter.getPrivateEncodedKey(parser));
        }
    }

    private String generateRsaKeyPairPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(keyPair);
        }
        return writer.toString();
    }
}
