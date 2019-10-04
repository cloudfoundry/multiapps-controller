package com.sap.cloud.lm.sl.cf.process.util;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

public class JarSignatureOperationsTest {

    private static final String SYMANTEC_SUBJECT_DN = "CN=Symantec Class 3 SHA256 Code Signing CA, OU=Symantec Trust Network, O=Symantec Corporation, C=US";
    private static final String SYMANTEC_ISSUER_DN = "CN=VeriSign Class 3 Public Primary Certification Authority - G5, OU=\"(c) 2006 VeriSign, Inc. - For authorized use only\", OU=VeriSign Trust Network, O=\"VeriSign, Inc.\", C=US";
    private static final String UNSIGNED_MTAR = "unsigned.mtar";

    @Mock
    private JarSignatureVerifier verifier;

    @InjectMocks
    private JarSignatureOperations mtaCertificateChecker;

    @BeforeEach
    public void setUp() {
        mtaCertificateChecker = new JarSignatureOperations();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetCertificatesCheckWhetherSymantecExists() {
        List<X509Certificate> providedCertificates = mtaCertificateChecker.readCertificates(Constants.SYMANTEC_CERTIFICATE_FILE);
        Assertions.assertEquals(1, providedCertificates.size());
        Assertions.assertEquals(SYMANTEC_SUBJECT_DN, providedCertificates.get(0)
                                                                         .getSubjectDN()
                                                                         .toString());
        Assertions.assertEquals(SYMANTEC_ISSUER_DN, providedCertificates.get(0)
                                                                        .getIssuerDN()
                                                                        .toString());
    }

    @Test
    public void testGetCertificatesWithNullInputStream() {
        SLException exception = Assertions.assertThrows(SLException.class,
                                                        () -> mtaCertificateChecker.readCertificates("non-existing-certificate.crt"));
        Assertions.assertEquals("Missing input stream", exception.getMessage());
    }

    @Test
    public void testVerifyShouldThrowException() {
        URL resource = getClass().getResource(UNSIGNED_MTAR);
        List<X509Certificate> certificates = mtaCertificateChecker.readCertificates(Constants.SYMANTEC_CERTIFICATE_FILE);
        mtaCertificateChecker.checkCertificates(resource, certificates, null);
        Mockito.verify(verifier)
               .verify(resource, certificates, null);
    }
}
