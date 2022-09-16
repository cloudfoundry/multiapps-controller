package org.cloudfoundry.multiapps.controller.persistence.util;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;

public class ClientKeyConfigurationHandler {

    public Path createEncodedKeyFile(String pemKey, String fileName) {
        try (PEMParser pemParser = new PEMParser(new StringReader(pemKey))) {
            PEMToEncodedKeyConverter pemKeyConverter = new PEMToEncodedKeyConverter();
            byte[] encodedKey = pemKeyConverter.getPrivateEncodedKey(pemParser);
            return writeEncodedKeyToFile(encodedKey, fileName);
        } catch (IOException e) {
            throw new SLException(e, Messages.GENERATING_KEY_FILE_FAILED, fileName);
        }
    }

    private Path writeEncodedKeyToFile(byte[] encodedKey, String fileName) throws IOException {
        return Files.write(Paths.get(fileName), encodedKey);
    }
}
