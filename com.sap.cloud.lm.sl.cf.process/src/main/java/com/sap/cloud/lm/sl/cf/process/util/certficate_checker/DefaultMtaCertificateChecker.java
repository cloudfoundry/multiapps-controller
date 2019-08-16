package com.sap.cloud.lm.sl.cf.process.util.certficate_checker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.verifier.Verifier;
import com.sap.cloud.lm.sl.common.SLException;

@Component
public class DefaultMtaCertificateChecker implements MtaCertificateChecker {

    @Inject
    private Verifier verifier;

    @Override
    public void checkCertificates(URL url, List<X509Certificate> certificates, String certificateCN) {
        verifier.verify(certificates, certificateCN, url);
    }

    @Override
    public List<X509Certificate> readProvidedCertificates(String certificatesFilename) {
        InputStream symantecInputStream = getClass().getResourceAsStream(certificatesFilename);
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(Constants.CERTIFICATE_TYPE_X_509);
            Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(symantecInputStream);
            return (List<X509Certificate>) certificates;
        } catch (CertificateException e) {
            throw new SLException(e);
        } finally {
            closeInputStream(symantecInputStream);
        }
    }

    private void closeInputStream(InputStream symantecInputStream) {
        try {
            if (symantecInputStream != null) {
                symantecInputStream.close();
            }
        } catch (IOException e) {
            throw new SLException(e);
        }
    }

}
