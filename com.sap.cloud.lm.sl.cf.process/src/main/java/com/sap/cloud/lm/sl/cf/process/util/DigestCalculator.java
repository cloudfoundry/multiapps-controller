package com.sap.cloud.lm.sl.cf.process.util;

import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

public class DigestCalculator {
    private MessageDigest messageDigest;

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
