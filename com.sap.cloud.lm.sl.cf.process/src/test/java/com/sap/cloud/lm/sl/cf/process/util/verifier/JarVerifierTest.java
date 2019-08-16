package com.sap.cloud.lm.sl.cf.process.util.verifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.web.client.ResourceAccessException;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public class JarVerifierTest {

    private static final String NON_SYMANTEC_SIGNATURE_MTAR = "non-symantec-signature.mtar";
    private static final String VALID_SIGNATURE_MTAR = "valid-signature.mtar";
    private static final String UNSIGNED_MTAR = "unsigned.mtar";
    private static final String EXPIRED_MTAR = "expired-certificate.mtar";

    private JarVerifier verifier;

    @BeforeEach
    public void setUp() {
        verifier = new JarVerifier();
    }

    @Test
    public void verifyMtarWithNonSymantecCertificate() {
        URL resource = getClass().getResource(NON_SYMANTEC_SIGNATURE_MTAR);
        SecurityException securityException = Assertions.assertThrows(SecurityException.class, verifierVerify(resource));
        Assertions.assertEquals(Messages.ARCHIVE_IS_NOT_SIGNED_BY_TRUSTED_SIGNER, securityException.getMessage());
    }

    @Test
    public void verifyMtarWhichIsNotSigned() {
        URL resource = getClass().getResource(UNSIGNED_MTAR);
        SecurityException securityException = Assertions.assertThrows(SecurityException.class, verifierVerify(resource));
        Assertions.assertEquals(Messages.ARCHIVE_CONTAINS_UNSIGNED_FILES, securityException.getMessage());
    }

    @Test
    public void testWhenFileIsNotFound() throws IOException {
        URL resource = new URL("file:invalid");
        ResourceAccessException resourceAccessException = Assertions.assertThrows(ResourceAccessException.class, verifierVerify(resource));
        Assertions.assertTrue(Objects.requireNonNull(resourceAccessException.getMessage())
                                     .contains(Messages.CERTIFICATE_VERIFICATION_HAS_FAILED));
    }

    @Test
    public void testWithValidSignatureMtar() throws Throwable {
        URL resource = getClass().getResource(VALID_SIGNATURE_MTAR);
        verifierVerify(resource).execute();
    }

    @Test
    public void testWithExpiredCertificate() {
        URL resource = getClass().getResource(EXPIRED_MTAR);
        Assertions.assertThrows(SecurityException.class, verifierVerify(resource));
    }

    private Executable verifierVerify(URL resource) {
        return () -> verifier.verify(readTargetCertificatesFromFile(), ApplicationConfiguration.DEFAULT_CERTIFICATE_CN, resource);
    }

    @Test
    public void testGetAChainWhenStartIndexIsGreaterThanEntryCertificatesSize() {
        List<X509Certificate> certificateChain = verifier.getAChain(new Certificate[0], 1);
        Assertions.assertEquals(0, certificateChain.size());
    }

    @Test
    public void testGetAChainWhenThereAreTwoChains() {
        verifyFirstChain(verifier);
        verifySecondChain(verifier);
    }

    private void verifyFirstChain(JarVerifier verifier) {
        List<X509Certificate> certificateChain = verifier.getAChain(getTwoCertificateChains(), 0);
        Assertions.assertEquals(2, certificateChain.size());
        Assertions.assertEquals("CN=Symantec Class 3 SHA256 Code Signing CA, OU=Symantec Trust Network, O=Symantec Corporation, C=US",
                                certificateChain.get(0)
                                                .getSubjectDN()
                                                .getName());
        Assertions.assertEquals("CN=VeriSign Class 3 Public Primary Certification Authority - G5, OU=\"(c) 2006 VeriSign, Inc. - For authorized use only\", OU=VeriSign Trust Network, O=\"VeriSign, Inc.\", C=US",
                                certificateChain.get(0)
                                                .getIssuerDN()
                                                .getName());
        Assertions.assertEquals("CN=VeriSign Class 3 Public Primary Certification Authority - G5, OU=\"(c) 2006 VeriSign, Inc. - For authorized use only\", OU=VeriSign Trust Network, O=\"VeriSign, Inc.\", C=US",
                                certificateChain.get(1)
                                                .getSubjectDN()
                                                .getName());
        Assertions.assertEquals("CN=SAP SE, OU=Trust Network 2, O=SAP CORP, C=GE", certificateChain.get(1)
                                                                                                   .getIssuerDN()
                                                                                                   .getName());
    }

    private void verifySecondChain(JarVerifier verifier) {
        List<X509Certificate> certificateChain = verifier.getAChain(getTwoCertificateChains(), 2);
        Assertions.assertEquals(2, certificateChain.size());
        Assertions.assertEquals("CN=SAP SE 2, OU=Network SAP, O=SAP CORP, C=FE", certificateChain.get(0)
                                                                                                 .getSubjectDN()
                                                                                                 .getName());
        Assertions.assertEquals("CN=VeriSign Class 2, OU=\"(c) 2006 VeriSign\", O=VeriSign, C=MD", certificateChain.get(0)
                                                                                                                   .getIssuerDN()
                                                                                                                   .getName());
        Assertions.assertEquals("CN=VeriSign Class 2, OU=\"(c) 2006 VeriSign\", O=VeriSign, C=MD", certificateChain.get(1)
                                                                                                                   .getSubjectDN()
                                                                                                                   .getName());
        Assertions.assertEquals("CN=SAP FE, OU=Trust Network 3, O=SAP CORP, C=UK", certificateChain.get(1)
                                                                                                   .getIssuerDN()
                                                                                                   .getName());
    }

    private List<X509Certificate> readTargetCertificatesFromFile() throws CertificateException, IOException {
        InputStream symantecInputStream = getClass().getResourceAsStream(Constants.SYMANTEC_CERTIFICATE_FILE);
        CertificateFactory certificateFactory = CertificateFactory.getInstance(Constants.CERTIFICATE_TYPE_X_509);
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(symantecInputStream);
        symantecInputStream.close();
        return (List<X509Certificate>) certificates;
    }

    private Certificate[] getTwoCertificateChains() {
        Certificate[] certificates = new Certificate[4];
        Principal subjectPrincipalFirstChain = createPrincipal("CN=Symantec Class 3 SHA256 Code Signing CA, OU=Symantec Trust Network, O=Symantec Corporation, C=US");
        Principal issuerPrincipalFirstChain = createPrincipal("CN=VeriSign Class 3 Public Primary Certification Authority - G5, OU=\"(c) 2006 VeriSign, Inc. - For authorized use only\", OU=VeriSign Trust Network, O=\"VeriSign, Inc.\", C=US");
        certificates[0] = createCertificate(subjectPrincipalFirstChain, issuerPrincipalFirstChain);
        certificates[1] = createCertificate(issuerPrincipalFirstChain, createPrincipal("CN=SAP SE, OU=Trust Network 2, O=SAP CORP, C=GE"));
        Principal subjectPrincipalSecondChain = createPrincipal("CN=SAP SE 2, OU=Network SAP, O=SAP CORP, C=FE");
        Principal issuerPrincipalSecondChain = createPrincipal("CN=VeriSign Class 2, OU=\"(c) 2006 VeriSign\", O=VeriSign, C=MD");
        certificates[2] = createCertificate(subjectPrincipalSecondChain, issuerPrincipalSecondChain);
        certificates[3] = createCertificate(issuerPrincipalSecondChain, createPrincipal("CN=SAP FE, OU=Trust Network 3, O=SAP CORP, C=UK"));
        return certificates;
    }

    private Principal createPrincipal(String name) {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName())
               .thenReturn(name);
        return principal;
    }

    private X509Certificate createCertificate(Principal subjectPrincipal, Principal issuerPrincipal) {
        X509Certificate certificate = Mockito.mock(X509Certificate.class);
        Mockito.when(certificate.getSubjectDN())
               .thenReturn(subjectPrincipal);
        Mockito.when(certificate.getIssuerDN())
               .thenReturn(issuerPrincipal);
        return certificate;
    }
}
