package com.sap.cloud.lm.sl.cf.process.util.verifier;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

public interface Verifier {

    void verify(List<X509Certificate> targetCertificates, String certificateCN, URL jarURL);
}
