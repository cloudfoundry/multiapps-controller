package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;

public class JarSignatureVerifierTest {

    private static final String CUSTOM_CERTIFICATE_MTAR = "custom-certificate.mtar";
    private static final String UNSIGNED_MTAR = "unsigned.mtar";
    private static final String EXPIRED_MTAR = "expired-certificate.mtar";
    private static final String ALTERED_MTAR = "altered.mtar";
    private static final String CONTAINS_UNSIGNED_FILES = "contains-unsigned-files.mtar";
    private static final String CUSTOM_CERTIFICATE = "custom-certificate.crt";

    private final JarSignatureVerifier verifier = new JarSignatureVerifier();

    @Test
    public void verifyMtarWithNonSymantecCertificate() {
        URL resource = getClass().getResource(CUSTOM_CERTIFICATE_MTAR);
        SLException exception = Assertions.assertThrows(SLException.class,
                                                        verifierVerify(resource, Constants.SYMANTEC_CERTIFICATE_FILE, null));
        Assertions.assertEquals(MessageFormat.format(Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE,
                                                     MessageFormat.format(Messages.THE_ARCHIVE_IS_NOT_SIGNED_BY_TRUSTED_CERTIFICATE_AUTHORITY,
                                                                          "[Symantec Class 3 SHA256 Code Signing CA]")),
                                exception.getMessage());
    }

    @Test
    public void verifyMtarWithUnsignedFiles() {
        URL resource = getClass().getResource(CONTAINS_UNSIGNED_FILES);
        SLException exception = Assertions.assertThrows(SLException.class,
                                                        verifierVerify(resource, Constants.SYMANTEC_CERTIFICATE_FILE, null));
        Assertions.assertEquals(MessageFormat.format(Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE,
                                                     MessageFormat.format(Messages.THE_ARCHIVE_CONTAINS_UNSIGNED_FILES, "unsigned")),
                                exception.getMessage());
    }

    @Test
    public void verifyMtarWhichIsNotSigned() {
        URL resource = getClass().getResource(UNSIGNED_MTAR);
        SLException exception = Assertions.assertThrows(SLException.class,
                                                        verifierVerify(resource, Constants.SYMANTEC_CERTIFICATE_FILE, null));
        Assertions.assertEquals(MessageFormat.format(Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE, Messages.THE_ARCHIVE_IS_NOT_SIGNED),
                                exception.getMessage());
    }

    @Test
    public void testWhenFileIsNotFound() throws IOException {
        URL resource = new URL("file:invalid");
        Assertions.assertThrows(SLException.class, verifierVerify(resource, Constants.SYMANTEC_CERTIFICATE_FILE, null));
    }

    @Test
    public void testWithValidSignatureMtarWithoutCustomCertificateCN() {
        URL resource = getClass().getResource(CUSTOM_CERTIFICATE_MTAR);
        Assertions.assertDoesNotThrow(verifierVerify(resource, CUSTOM_CERTIFICATE, null));
    }

    @Test
    public void testWithValidSignatureMtarWithCustomCertificateCN() {
        String notValidCertificateCn = "Not valid";
        URL resource = getClass().getResource(CUSTOM_CERTIFICATE_MTAR);
        SLException exception = Assertions.assertThrows(SLException.class, verifierVerify(resource, CUSTOM_CERTIFICATE,
                                                                                          notValidCertificateCn));
        Assertions.assertEquals(MessageFormat.format(Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE,
                                                     MessageFormat.format(Messages.WILL_LOOK_FOR_CERTIFICATE_CN, notValidCertificateCn)),
                                exception.getMessage());
    }

    @Test
    public void testWithExpiredCertificate() {
        URL resource = getClass().getResource(EXPIRED_MTAR);
        SLException exception = Assertions.assertThrows(SLException.class,
                                                        verifierVerify(resource, Constants.SYMANTEC_CERTIFICATE_FILE, null));
        Assertions.assertTrue(exception.getMessage()
                                       .contains(MessageFormat.format(Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE, "NotAfter:")));
    }

    @Test
    public void testWithAlteredMtar() {
        URL resource = getClass().getResource(ALTERED_MTAR);
        SLException exception = Assertions.assertThrows(SLException.class,
                                                        verifierVerify(resource, Constants.SYMANTEC_CERTIFICATE_FILE, null));
        Assertions.assertEquals(MessageFormat.format(Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE,
                                                     "SHA-256 digest error for META-INF/mtad.yaml"),
                                exception.getMessage());
    }

    private Executable verifierVerify(URL resource, String filename, String certificateCN) {
        return () -> verifier.verify(resource, readTargetCertificatesFromFile(filename), certificateCN);
    }

    private List<X509Certificate> readTargetCertificatesFromFile(String filename) throws CertificateException, IOException {
        InputStream symantecInputStream = getClass().getResourceAsStream(filename);
        CertificateFactory certificateFactory = CertificateFactory.getInstance(Constants.CERTIFICATE_TYPE_X_509);
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(symantecInputStream);
        symantecInputStream.close();
        return (List<X509Certificate>) certificates;
    }
}
