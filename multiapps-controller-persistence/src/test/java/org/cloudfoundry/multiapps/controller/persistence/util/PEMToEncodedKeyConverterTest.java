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
    void testThrowsIllegalArgumentExceptionWithInvalidKeyFormatMessageForPublicKey() throws Exception {
        String pem = generatePublicKeyPem();

        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                                                                      () -> converter.getPrivateEncodedKey(parser));
            Assertions.assertTrue(thrown.getMessage()
                                        .startsWith("Invalid key format:"),
                                  () -> "Unexpected message: " + thrown.getMessage());
        }
    }

    private String generatePublicKeyPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(keyPair.getPublic());
        }
        return writer.toString();
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
