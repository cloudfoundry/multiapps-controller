package com.sap.cloud.lm.sl.cf.process.util.certficate_checker;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

public interface MtaCertificateChecker {

    void checkCertificates(URL url, List<X509Certificate> certificates, String certificateCN);

    List<X509Certificate> readProvidedCertificates(String certificatesFilename);
}
