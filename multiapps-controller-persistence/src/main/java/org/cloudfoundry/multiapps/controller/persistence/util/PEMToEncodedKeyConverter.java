package org.cloudfoundry.multiapps.controller.persistence.util;

import java.io.IOException;
import java.security.KeyPair;
import java.text.MessageFormat;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.cloudfoundry.multiapps.controller.persistence.Messages;

public class PEMToEncodedKeyConverter {

    public byte[] getPrivateEncodedKey(PEMParser pemParser) throws IOException {
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        Object pemContent = pemParser.readObject();
        if (pemContent instanceof PEMKeyPair) {
            KeyPair keyPair = converter.getKeyPair((PEMKeyPair) pemContent);
            return keyPair.getPrivate()
                          .getEncoded();
        }
        throw new IllegalArgumentException(MessageFormat.format(Messages.INVALID_KEY_FORMAT, pemContent.getClass()
                                                                                                       .getSimpleName()));
    }

}
