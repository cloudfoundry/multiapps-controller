package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;

import com.sap.cloud.lm.sl.cf.process.Constants;

@Named
public class JarSignatureOperations {

    @Inject
    private JarSignatureVerifier verifier;

    public void checkCertificates(URL url, List<X509Certificate> certificates, String certificateCN) {
        verifier.verify(url, certificates, certificateCN);
    }

    public List<X509Certificate> readCertificates(String certificatesFilename) {
        try (InputStream certificatesInputStream = getClass().getResourceAsStream(certificatesFilename)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(Constants.CERTIFICATE_TYPE_X_509);
            return (List<X509Certificate>) certificateFactory.generateCertificates(certificatesInputStream);
        } catch (CertificateException | IOException e) {
            throw new SLException(e, e.getMessage());
        }
    }

}
