package com.sap.cloud.lm.sl.cf.persistence.stream;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class DigestInputStreamAdapter<T extends InputStream> extends DecoratedInputStream<DigestInputStream, T> {

    public DigestInputStreamAdapter(T inputStream, MessageDigest messageDigest) {
        super(new DigestInputStream(inputStream, messageDigest), inputStream);
    }

}
