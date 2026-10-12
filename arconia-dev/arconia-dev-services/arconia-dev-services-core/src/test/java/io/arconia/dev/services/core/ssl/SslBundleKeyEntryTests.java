package io.arconia.dev.services.core.ssl;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link SslBundleKeyEntry}.
 */
class SslBundleKeyEntryTests {

    private static final String SERVER_SUBJECT = "CN=localhost,O=Arconia";

    @Test
    void whenKeyStoreHasOneKeyEntryThenItIsResolved() {
        SslBundleKeyEntry keyEntry = SslBundleKeyEntry.resolve(pemBundle(SslTestCertificates.CERTIFICATE_CHAIN, null, null));

        assertThat(keyEntry.alias()).isEqualTo("ssl");
        assertThat(keyEntry.privateKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyEntry.certificateChain()).hasSize(2);
        assertThat(keyEntry.certificateChain().getFirst().getSubjectX500Principal().getName()).isEqualTo(SERVER_SUBJECT);
    }

    @Test
    void whenKeyAliasIsSetThenThatEntryIsResolved() {
        assertThat(SslBundleKeyEntry.resolve(jksBundle(SslTestCertificates.KEY_STORE, "server", null)).alias()).isEqualTo("server");
        assertThatIllegalStateException()
                .isThrownBy(() -> SslBundleKeyEntry.resolve(jksBundle(SslTestCertificates.KEY_STORE, "other", null)))
                .withMessage("The SSL bundle key store has no key entry named 'other'");
    }

    @Test
    void whenKeyStoreHasSeveralKeyEntriesThenAliasIsRequired() {
        assertThatIllegalStateException()
                .isThrownBy(() -> SslBundleKeyEntry.resolve(jksBundle(SslTestCertificates.KEY_STORE_WITH_TWO_KEYS, null, null)))
                .withMessageContaining("The SSL bundle key store has several key entries")
                .withMessageContaining("second").withMessageContaining("server")
                .withMessageContaining("set the key alias to select one");
        assertThat(SslBundleKeyEntry.resolve(jksBundle(SslTestCertificates.KEY_STORE_WITH_TWO_KEYS, "second", null)).alias()).isEqualTo("second");
    }

    @Test
    void whenKeyPasswordDiffersFromStorePasswordThenItMustBeSet() {
        assertThatIllegalStateException()
                .isThrownBy(() -> SslBundleKeyEntry.resolve(jksBundle(SslTestCertificates.KEY_STORE_WITH_KEY_PASSWORD, null, null)))
                .withMessage("The key 'server' of the SSL bundle cannot be read: set the key store password, or the key password when it differs from the one of the key store");
        assertThat(SslBundleKeyEntry.resolve(jksBundle(SslTestCertificates.KEY_STORE_WITH_KEY_PASSWORD, null, SslTestCertificates.KEY_PASSWORD)).alias())
                .isEqualTo("server");
    }

    @Test
    void whenPemKeyHasAliasAndPasswordThenTheyAreHonored() {
        SslBundleKeyEntry keyEntry = SslBundleKeyEntry.resolve(pemBundle(SslTestCertificates.CERTIFICATE, "custom", "changeit"));

        assertThat(keyEntry.alias()).isEqualTo("custom");
        assertThat(keyEntry.certificateChain()).hasSize(1);
    }

    @Test
    void whenBundleHasNoKeyStoreThenThrow() {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getTruststore().setCertificate(SslTestCertificates.CA_CERTIFICATE);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> SslBundleKeyEntry.resolve(PropertiesSslBundle.get(properties)))
                .withMessage("The SSL bundle has no key store");
    }

    @Test
    void whenKeyStoreHasNoKeyEntryThenThrow() {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(SslTestCertificates.CERTIFICATE);

        assertThatIllegalStateException()
                .isThrownBy(() -> SslBundleKeyEntry.resolve(PropertiesSslBundle.get(properties)))
                .withMessage("The SSL bundle key store has no key entry");
    }

    private static SslBundle pemBundle(String certificate, @Nullable String alias, @Nullable String password) {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(certificate);
        properties.getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);
        properties.getKey().setAlias(alias);
        properties.getKey().setPassword(password);
        return PropertiesSslBundle.get(properties);
    }

    private static SslBundle jksBundle(String location, @Nullable String alias, @Nullable String keyPassword) {
        JksSslBundleProperties properties = new JksSslBundleProperties();
        properties.getKeystore().setLocation(location);
        properties.getKeystore().setPassword(SslTestCertificates.STORE_PASSWORD);
        properties.getKey().setAlias(alias);
        properties.getKey().setPassword(keyPassword);
        return PropertiesSslBundle.get(properties);
    }

}
