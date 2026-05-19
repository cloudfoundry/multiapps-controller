package org.cloudfoundry.multiapps.controller.persistence.util;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientKeyConfigurationHandlerTest {

    private final ClientKeyConfigurationHandler handler = new ClientKeyConfigurationHandler();
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("client-key-test");
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                  .forEach(p -> p.toFile()
                                 .delete());
        }
    }

    @Test
    void testCreateEncodedKeyFileWritesEncodedBytes() throws Exception {
        String pem = generateRsaKeyPairPem();
        Path target = tempDir.resolve("key.bin");

        Path result = handler.createEncodedKeyFile(pem, target.toString());

        Assertions.assertEquals(target, result);
        Assertions.assertTrue(Files.exists(result));
        Assertions.assertTrue(Files.size(result) > 0);
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
