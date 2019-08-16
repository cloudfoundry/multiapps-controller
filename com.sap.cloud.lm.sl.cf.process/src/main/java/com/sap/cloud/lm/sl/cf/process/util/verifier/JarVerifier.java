package com.sap.cloud.lm.sl.cf.process.util.verifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import com.google.common.base.Splitter;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component
public class JarVerifier implements Verifier {

    private static final String META_INF = "META-INF";
    private static final String CN = "CN";
    private static final int BUFFER_SIZE = 8 * 1024;

    @Override
    public void verify(List<X509Certificate> targetCertificates, String certificateCN, URL jarURL) {
        try {
            JarFile jarFile = retrieveJarFile(jarURL);
            List<JarEntry> jarEntries = getJarEntries(jarFile);
            verifyJarEntries(targetCertificates, jarEntries, certificateCN);
        } catch (IOException e) {
            throw new ResourceAccessException(Messages.CERTIFICATE_VERIFICATION_HAS_FAILED, e);
        }
    }

    private JarFile retrieveJarFile(URL jarURL) throws IOException {
        jarURL = new URL("jar:" + jarURL.toString() + "!/");
        JarURLConnection connection = (JarURLConnection) jarURL.openConnection();
        return connection.getJarFile();
    }

    private List<JarEntry> getJarEntries(JarFile jarFile) throws IOException {
        List<JarEntry> jarEntries = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            readJarEntry(jarFile, jarEntries, entries);
        }
        return jarEntries;
    }

    private void readJarEntry(JarFile jarFile, List<JarEntry> jarEntries, Enumeration<JarEntry> entries) throws IOException {
        JarEntry jarEntry = entries.nextElement();
        validateJarEntryPath(jarFile, jarEntry);
        if (jarEntry.isDirectory()) {
            return;
        }
        jarEntries.add(jarEntry);
        readStream(jarFile, jarEntry);
    }

    private void validateJarEntryPath(JarFile jarFile, JarEntry jarEntry) throws IOException {
        Path currentRelativePath = Paths.get("");
        File jarEntryFile = new File(jarEntry.getName());
        if (!jarEntryFile.getCanonicalPath()
                         .startsWith(currentRelativePath.toAbsolutePath() + File.separator)) {
            throw new IllegalAccessError(MessageFormat.format(Messages.ENTRY_IS_TRYING_TO_LEAVE_TARGET_DIR, jarFile.getName()));
        }
    }

    private void readStream(JarFile jarFile, JarEntry jarEntry) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            while (inputStream.read(buffer, 0, buffer.length) != -1) {
                // Just read. This will throw a SecurityException if a signature/digest check fails.
            }
        }
    }

    private void verifyJarEntries(List<X509Certificate> targetCerts, List<JarEntry> jarEntries, String certificateCN) {
        for (JarEntry jarEntry : jarEntries) {
            Certificate[] entryCertificates = jarEntry.getCertificates();
            if (entryCertificates == null || entryCertificates.length == 0) {
                checkJarEntryName(jarEntry);
                continue;
            }
            verifyJarEntryIsSigned(targetCerts, entryCertificates, certificateCN);
        }
    }

    private void checkJarEntryName(JarEntry jarEntry) {
        if (!jarEntry.getName()
                     .startsWith(META_INF)) {
            throw new SecurityException(Messages.ARCHIVE_CONTAINS_UNSIGNED_FILES);
        }
    }

    private void verifyJarEntryIsSigned(List<X509Certificate> targetCerts, Certificate[] entryCertificates, String certificateCN) {
        boolean signedAsExpected = isSignedAsExpected(targetCerts, entryCertificates, certificateCN);
        if (!signedAsExpected) {
            throw new SecurityException(Messages.ARCHIVE_IS_NOT_SIGNED_BY_TRUSTED_SIGNER);
        }
    }

    private boolean isSignedAsExpected(List<X509Certificate> targetCerts, Certificate[] entryCertificates, String certificateCN) {
        List<X509Certificate> entryCertificateChain;
        int startIndex = 0;
        while (!(entryCertificateChain = getAChain(entryCertificates, startIndex)).isEmpty()) {
            if (validCertificateChain(targetCerts, entryCertificateChain, certificateCN)) {
                return true;
            }
            startIndex += entryCertificateChain.size();
        }
        return false;
    }

    List<X509Certificate> getAChain(Certificate[] entryCertificates, int startIndex) {
        if (startIndex > entryCertificates.length - 1) {
            return Collections.emptyList();
        }
        int nextChainIndex = findNextChainIndex(entryCertificates, startIndex);
        int certificateChainSize = (nextChainIndex - startIndex) + 1;
        return getX509CertificateChain(entryCertificates, startIndex, certificateChainSize);
    }

    private int findNextChainIndex(Certificate[] certs, int startIndex) {
        int chainIndex;
        for (chainIndex = startIndex; chainIndex < certs.length - 1; chainIndex++) {
            if (!Objects.equals(((X509Certificate) certs[chainIndex + 1]).getSubjectDN(),
                                ((X509Certificate) certs[chainIndex]).getIssuerDN())) {
                break;
            }
        }
        return chainIndex;
    }

    private List<X509Certificate> getX509CertificateChain(Certificate[] certificates, int startIndex, int certChainSize) {
        List<X509Certificate> x509Certificates = new ArrayList<>();
        for (int i = 0; i < certChainSize; i++) {
            x509Certificates.add((X509Certificate) certificates[startIndex + i]);
        }
        return x509Certificates;
    }

    private boolean validCertificateChain(List<X509Certificate> targetCerts, List<X509Certificate> entryCertificateChain,
                                          String certificateCN) {
        boolean certificateCNMatches = false;
        boolean targetCertificatesMatchEntryCertificate = false;
        for (X509Certificate entryCertificate : entryCertificateChain) {
            checkValidityOfCertificate(entryCertificate);
            if (!certificateCNMatches) {
                certificateCNMatches = checkCertificateCN(entryCertificate, certificateCN);
            }
            if (!targetCertificatesMatchEntryCertificate) {
                targetCertificatesMatchEntryCertificate = doTargetCertificatesMatchEntryCertificate(targetCerts, entryCertificate);
            }
        }
        return certificateCNMatches && targetCertificatesMatchEntryCertificate;
    }

    private void checkValidityOfCertificate(X509Certificate x509Certificate) {
        try {
            x509Certificate.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw new SecurityException(e);
        }
    }

    private boolean doTargetCertificatesMatchEntryCertificate(List<X509Certificate> targetCertificates, X509Certificate entryCertificate) {
        return targetCertificates.stream()
                                 .anyMatch(certificate -> Objects.equals(certificate, entryCertificate));
    }

    private boolean checkCertificateCN(X509Certificate entryCertificate, String certificateCN) {
        Map<String, String> params = Splitter.on(", ")
                                             .withKeyValueSeparator("=")
                                             .split(entryCertificate.getSubjectDN()
                                                                    .getName());
        return Objects.equals(params.get(CN), certificateCN);
    }
}
