package com.sap.cloud.lm.sl.cf.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.inject.Named;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Splitter;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named
public class JarSignatureVerifier {

    private static final String META_INF = "META-INF";
    private static final String CN = "CN";
    private static final int BUFFER_SIZE = 8 * 1024;

    public void verify(URL jarFileUrl, List<X509Certificate> targetCertificates, String certificateCN) {
        try {
            JarFile jarFile = openJarFile(jarFileUrl);
            List<JarEntry> jarEntries = getJarEntries(jarFile);
            verifyJarEntries(jarEntries, targetCertificates, certificateCN);
        } catch (IOException | SecurityException | SLException e) {
            throw new SLException(e, Messages.COULD_NOT_VERIFY_ARCHIVE_SIGNATURE, e.getMessage());
        }
    }

    private JarFile openJarFile(URL jarFileUrl) throws IOException {
        JarURLConnection connection = (JarURLConnection) toJarUrl(jarFileUrl).openConnection();
        return connection.getJarFile();
    }

    private URL toJarUrl(URL jarUrl) throws MalformedURLException {
        return new URL("jar:" + jarUrl.toString() + "!/");
    }

    private List<JarEntry> getJarEntries(JarFile jarFile) {
        return EnumerationUtils.toList(jarFile.entries())
                               .stream()
                               .map(jarEntry -> readJarEntry(jarFile, jarEntry))
                               .filter(jarEntry -> !jarEntry.isDirectory())
                               .collect(Collectors.toList());
    }

    private JarEntry readJarEntry(JarFile jarFile, JarEntry jarEntry) {
        FileUtils.validatePath(jarEntry.getName());
        try {
            verifySignature(jarFile, jarEntry);
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
        }
        return jarEntry;
    }

    private void verifySignature(JarFile jarFile, JarEntry jarEntry) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            while (inputStream.read(buffer, 0, buffer.length) != -1) {
                // Just read. This will throw a SecurityException if a signature/digest check fails.
                // JarEntry.getCertificates() will return null if the entry input stream is not read first
            }
        }
    }

    private void verifyJarEntries(List<JarEntry> jarEntries, List<X509Certificate> targetCertificates, String certificateCN) {
        verifyArchiveFilesAreSigned(jarEntries);
        for (JarEntry jarEntry : getSignedJarEntries(jarEntries)) {
            validateCertificateChain(targetCertificates, toX509Certificates(jarEntry.getCertificates()), certificateCN);
        }
    }

    private void verifyArchiveFilesAreSigned(List<JarEntry> jarEntries) {
        List<JarEntry> nonMetaInformationJarEntries = getNonMetaInformationJarEntries(jarEntries);
        List<JarEntry> nonSignedJarEntries = getNonSignedJarEntries(nonMetaInformationJarEntries);
        if (!nonSignedJarEntries.isEmpty()) {
            if (nonMetaInformationJarEntries.size() == nonSignedJarEntries.size()) {
                throw new SLException(Messages.THE_ARCHIVE_IS_NOT_SIGNED);
            }
            throw new SLException(Messages.THE_ARCHIVE_CONTAINS_UNSIGNED_FILES, getJarEntriesNames(nonSignedJarEntries));
        }
    }

    private List<JarEntry> getNonMetaInformationJarEntries(List<JarEntry> jarEntries) {
        return jarEntries.stream()
                         .filter(jarEntry -> !isMetaInformation(jarEntry))
                         .collect(Collectors.toList());
    }

    private boolean isMetaInformation(JarEntry jarEntry) {
        return jarEntry.getName()
                       .startsWith(META_INF);
    }

    private List<JarEntry> getNonSignedJarEntries(List<JarEntry> jarEntries) {
        return jarEntries.stream()
                         .filter(jarEntry -> !isSigned(jarEntry))
                         .collect(Collectors.toList());
    }

    private boolean isSigned(JarEntry jarEntry) {
        return !ArrayUtils.isEmpty(jarEntry.getCertificates());
    }

    private String getJarEntriesNames(List<JarEntry> jarEntries) {
        return jarEntries.stream()
                         .map(ZipEntry::getName)
                         .collect(Collectors.joining(System.lineSeparator()));
    }

    private List<JarEntry> getSignedJarEntries(List<JarEntry> jarEntries) {
        return jarEntries.stream()
                         .filter(this::isSigned)
                         .collect(Collectors.toList());
    }

    private List<X509Certificate> toX509Certificates(Certificate[] certificates) {
        return Arrays.stream(certificates)
                     .map(certificate -> (X509Certificate) certificate)
                     .collect(Collectors.toList());
    }

    private void validateCertificateChain(List<X509Certificate> targetCertificates, List<X509Certificate> candidateCertificateChain,
                                          String certificateCN) {
        boolean certificateCNMatches = false;
        boolean targetCertificatesMatchEntryCertificate = false;
        for (X509Certificate certificate : candidateCertificateChain) {
            checkValidityOfCertificate(certificate);
            if (!certificateCNMatches) {
                certificateCNMatches = certificateCNMatches(certificate, certificateCN);
            }
            if (!targetCertificatesMatchEntryCertificate) {
                targetCertificatesMatchEntryCertificate = doTargetCertificatesMatchEntryCertificate(targetCertificates, certificate);
            }
        }
        if (!certificateCNMatches) {
            throw new SLException(Messages.WILL_LOOK_FOR_CERTIFICATE_CN, certificateCN);
        }
        if (!targetCertificatesMatchEntryCertificate) {
            throw new SLException(Messages.THE_ARCHIVE_IS_NOT_SIGNED_BY_TRUSTED_CERTIFICATE_AUTHORITY,
                                  getCertificatesNames(targetCertificates));
        }
    }

    private void checkValidityOfCertificate(X509Certificate x509Certificate) {
        try {
            x509Certificate.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private boolean doTargetCertificatesMatchEntryCertificate(List<X509Certificate> targetCertificates, X509Certificate certificate) {
        return targetCertificates.stream()
                                 .anyMatch(currentCertificate -> Objects.equals(currentCertificate, certificate));
    }

    private boolean certificateCNMatches(X509Certificate certificate, String certificateCN) {
        if (certificateCN == null) {
            return true;
        }
        return Objects.equals(getCertificateCN(certificate), certificateCN);
    }

    private String getCertificateCN(X509Certificate certificate) {
        Map<String, String> parameters = getCertificateSubjectParameters(certificate);
        return parameters.get(CN);
    }

    private Map<String, String> getCertificateSubjectParameters(X509Certificate certificate) {
        Pattern subjectSeparatorPattern = Pattern.compile(", *");
        return Splitter.on(subjectSeparatorPattern)
                       .withKeyValueSeparator("=")
                       .split(certificate.getSubjectDN()
                                         .getName());
    }

    private List<String> getCertificatesNames(List<X509Certificate> targetCertificates) {
        return targetCertificates.stream()
                                 .map(this::getCertificateCN)
                                 .collect(Collectors.toList());
    }

}
