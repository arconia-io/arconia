package io.arconia.dev.services.core.ssl;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemContent;
import org.testcontainers.images.builder.Transferable;

import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SslBundleFiles}.
 */
class SslBundleFilesTests {

    private static final String SERVER_SUBJECT = "CN=localhost,O=Arconia";

    private static final String CA_SUBJECT = "CN=Arconia Dev Services Test CA,O=Arconia";

    @Test
    void certificateChainIsTheCertificateOfTheKeyEntry() {
        Transferable file = SslBundleFiles.certificateChain(pemBundle(SslTestCertificates.CERTIFICATE));

        assertThat(file.getFileMode()).isEqualTo(Transferable.DEFAULT_FILE_MODE);
        assertThat(subjects(certificates(file))).containsExactly(SERVER_SUBJECT);
    }

    @Test
    void certificateChainKeepsTheWholeChainLeafFirst() {
        Transferable file = SslBundleFiles.certificateChain(pemBundle(SslTestCertificates.CERTIFICATE_CHAIN));

        assertThat(subjects(certificates(file))).containsExactly(SERVER_SUBJECT, CA_SUBJECT);
    }

    @Test
    void privateKeyIsThePrivateKeyOfTheKeyEntry() {
        SslBundle bundle = pemBundle(SslTestCertificates.CERTIFICATE);

        Transferable file = SslBundleFiles.privateKey(bundle);

        assertThat(content(file)).startsWith("-----BEGIN PRIVATE KEY-----");
        PemContent pem = PemContent.of(content(file));
        assertThat(pem).isNotNull();
        assertThat(pem.getPrivateKey()).isNotNull()
                .satisfies(key -> assertThat(key.getEncoded()).isEqualTo(SslBundleKeyEntry.resolve(bundle).privateKey().getEncoded()));
    }

    @Test
    void certificateChainWithPrivateKeyHoldsBothCertificatesFirst() {
        SslBundle bundle = pemBundle(SslTestCertificates.CERTIFICATE_CHAIN);

        String content = content(SslBundleFiles.certificateChainWithPrivateKey(bundle));

        assertThat(content).startsWith("-----BEGIN CERTIFICATE-----").endsWith("-----END PRIVATE KEY-----\n");
        PemContent pem = PemContent.of(content);
        assertThat(pem).isNotNull();
        assertThat(subjects(pem.getCertificates())).containsExactly(SERVER_SUBJECT, CA_SUBJECT);
        assertThat(pem.getPrivateKey()).isNotNull();
    }

    @Test
    void certificateAuthoritiesAreTheCertificateChain() {
        Transferable file = SslBundleFiles.certificateAuthorities(pemBundle(SslTestCertificates.CERTIFICATE_CHAIN));

        assertThat(subjects(certificates(file))).containsExactly(SERVER_SUBJECT, CA_SUBJECT);
    }

    @Test
    void jksBundleIsRenderedTheSameWay() {
        SslBundle bundle = jksBundle();

        assertThat(subjects(certificates(SslBundleFiles.certificateChain(bundle)))).containsExactly(SERVER_SUBJECT, CA_SUBJECT);
        assertThat(content(SslBundleFiles.privateKey(bundle))).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(subjects(certificates(SslBundleFiles.certificateAuthorities(bundle)))).containsExactly(SERVER_SUBJECT, CA_SUBJECT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"certificateChain", "privateKey", "certificateChainWithPrivateKey", "certificateAuthorities"})
    void filesAreDeterministic(String method) {
        // The content of the copied files is part of the hash Testcontainers reuses containers by.
        Function<SslBundle, Transferable> file = switch (method) {
            case "certificateChain" -> SslBundleFiles::certificateChain;
            case "privateKey" -> SslBundleFiles::privateKey;
            case "certificateChainWithPrivateKey" -> SslBundleFiles::certificateChainWithPrivateKey;
            default -> SslBundleFiles::certificateAuthorities;
        };

        assertThat(file.apply(jksBundle()).getBytes()).isEqualTo(file.apply(jksBundle()).getBytes());
        assertThat(file.apply(pemBundle(SslTestCertificates.CERTIFICATE_CHAIN)).getBytes())
                .isEqualTo(file.apply(pemBundle(SslTestCertificates.CERTIFICATE_CHAIN)).getBytes());
    }

    private static SslBundle pemBundle(String certificate) {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(certificate);
        properties.getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);
        return PropertiesSslBundle.get(properties);
    }

    private static SslBundle jksBundle() {
        JksSslBundleProperties properties = new JksSslBundleProperties();
        properties.getKeystore().setLocation(SslTestCertificates.KEY_STORE);
        properties.getKeystore().setPassword(SslTestCertificates.STORE_PASSWORD);
        return PropertiesSslBundle.get(properties);
    }

    private static String content(Transferable file) {
        return new String(file.getBytes(), StandardCharsets.US_ASCII);
    }

    private static List<X509Certificate> certificates(Transferable file) {
        PemContent pem = PemContent.of(content(file));
        assertThat(pem).isNotNull();
        return pem.getCertificates();
    }

    private static List<String> subjects(List<X509Certificate> certificates) {
        return certificates.stream().map(certificate -> certificate.getSubjectX500Principal().getName()).toList();
    }

}
