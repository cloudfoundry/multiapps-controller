package org.cloudfoundry.multiapps.controller.process.util;

import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

public class DigestCalculator {
    private final MessageDigest messageDigest;

    public DigestCalculator(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;
    }

    public void updateDigest(byte[] bytes, int offset, int len) {
        messageDigest.update(bytes, offset, len);
    }

    public String getDigest() {
        return DatatypeConverter.printHexBinary(messageDigest.digest());
    }
}
