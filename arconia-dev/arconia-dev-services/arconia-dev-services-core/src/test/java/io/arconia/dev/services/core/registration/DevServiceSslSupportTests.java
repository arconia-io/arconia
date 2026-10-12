package io.arconia.dev.services.core.registration;

import java.lang.annotation.Annotation;
import java.security.KeyStore;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.TestSslDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceDefinition.SslSupport;
import io.arconia.dev.services.core.registration.DevServiceSslSupport.DevServiceSslBundle;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestLinkContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslKeyStoreRequirement;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServiceSslSupport}.
 */
class DevServiceSslSupportTests {

    private static final String PREFIX = "Dev Service 'postgres': ";

    private static final DevServiceSslConfigurer<TestPostgresContainer> CONFIGURER = (container, bundle) -> container.withEnv("SSL", "true");

    private static final SslSupport SUPPORT = new SslSupport(CONFIGURER, SslKeyStoreRequirement.REQUIRED);

    private static final SslSupport SUPPORT_WITH_OPTIONAL_KEY_STORE = new SslSupport(CONFIGURER, SslKeyStoreRequirement.OPTIONAL);

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    @Nested
    class Validate {

        @Test
        void whenPropertiesDoNotSupportSslThenNoSupportIsValid() {
            assertThatCode(() -> DevServiceSslSupport.validate(PREFIX, TestDevServicesProperties.DEFAULT, null)).doesNotThrowAnyException();
        }

        @Test
        void whenPropertiesDoNotSupportSslThenDeclaringSupportThrows() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DevServiceSslSupport.validate(PREFIX, TestDevServicesProperties.DEFAULT, SUPPORT))
                    .withMessage(PREFIX + "SSL support is declared, but the properties don't implement SslDevServicesProperties: implement it, or remove the ssl(...) declaration");
        }

        @Test
        void whenSslIsDisabledThenTheOtherPropertiesHaveNoEffect() {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.pem();
            properties.getSsl().setEnabled(false);

            // As with the client-side ssl.enabled properties of Spring Boot, the switch alone decides.
            assertThatCode(() -> DevServiceSslSupport.validate(PREFIX, properties, null)).doesNotThrowAnyException();
            assertThatCode(() -> DevServiceSslSupport.validate(PREFIX, properties, SUPPORT)).doesNotThrowAnyException();
        }

        @Test
        void whenSslIsEnabledWithoutSupportThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> DevServiceSslSupport.validate(PREFIX, TestSslDevServicesProperties.pem(), null))
                    .withMessage(PREFIX + "the 'ssl.enabled' property is true, but the dev service declares no SSL support");
        }

        @Test
        void whenPemAndJksAreBothConfiguredThenThrow() {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.pem();
            properties.getSsl().getJks().getKeystore().setLocation(TestSslDevServicesProperties.KEY_STORE);

            assertThatIllegalStateException()
                    .isThrownBy(() -> DevServiceSslSupport.validate(PREFIX, properties, SUPPORT))
                    .withMessage(PREFIX + "the 'ssl.pem' and 'ssl.jks' properties cannot be used together: configure one of them");
        }

        @Test
        void whenOnlyOneHalfOfThePemKeyStoreIsConfiguredThenThrowNamingTheOther() {
            TestSslDevServicesProperties certificateOnly = TestSslDevServicesProperties.enabledWithoutKeyStore();
            certificateOnly.getSsl().getPem().getKeystore().setCertificate(TestSslDevServicesProperties.CERTIFICATE);
            TestSslDevServicesProperties keyOnly = TestSslDevServicesProperties.enabledWithoutKeyStore();
            keyOnly.getSsl().getPem().getKeystore().setPrivateKey(TestSslDevServicesProperties.PRIVATE_KEY);

            assertThatIllegalStateException()
                    .isThrownBy(() -> DevServiceSslSupport.validate(PREFIX, certificateOnly, SUPPORT))
                    .withMessage(PREFIX + "the 'ssl.pem.keystore.private-key' property is required with 'ssl.pem.keystore.certificate'");
            assertThatIllegalStateException()
                    .isThrownBy(() -> DevServiceSslSupport.validate(PREFIX, keyOnly, SUPPORT_WITH_OPTIONAL_KEY_STORE))
                    .withMessage(PREFIX + "the 'ssl.pem.keystore.certificate' property is required with 'ssl.pem.keystore.private-key'");
        }

        @Test
        void whenCertificateIsConfiguredOnlyButNoKeyStoreIsThenThrow() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> DevServiceSslSupport.validate(PREFIX, TestSslDevServicesProperties.enabledWithoutKeyStore(), SUPPORT))
                    .withMessage(PREFIX + "the 'ssl.enabled' property is true, but no key store is configured: set 'ssl.pem.keystore.certificate' and 'ssl.pem.keystore.private-key', or 'ssl.jks.keystore.location'");
        }

        @Test
        void whenCertificateMayBeGeneratedThenEnablingSslAloneIsValid() {
            assertThatCode(() -> DevServiceSslSupport.validate(PREFIX, TestSslDevServicesProperties.enabledWithoutKeyStore(), SUPPORT_WITH_OPTIONAL_KEY_STORE))
                    .doesNotThrowAnyException();
        }

    }

    @Nested
    class CreateBundle {

        @Test
        void whenSslIsNotEnabledThenNoBundle() {
            assertThat(DevServiceSslSupport.createBundle(definition(TestDevServicesProperties.DEFAULT, null), RESOURCE_LOADER)).isNull();
            assertThat(DevServiceSslSupport.createBundle(definition(new TestSslDevServicesProperties(), SUPPORT), RESOURCE_LOADER)).isNull();
        }

        @Test
        void whenNoKeyStoreIsConfiguredThenNoBundle() {
            assertThat(DevServiceSslSupport.createBundle(definition(TestSslDevServicesProperties.enabledWithoutKeyStore(), SUPPORT_WITH_OPTIONAL_KEY_STORE), RESOURCE_LOADER)).isNull();
        }

        @Test
        void whenPemIsConfiguredThenBundleIsBuiltFromIt() throws Exception {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.pem();
            properties.getSsl().getPem().setProtocol("TLSv1.3");
            properties.getSsl().getPem().getKey().setAlias("custom");

            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(properties, SUPPORT), RESOURCE_LOADER);

            assertThat(sslBundle).isNotNull();
            assertThat(sslBundle.bundle().getProtocol()).isEqualTo("TLSv1.3");
            KeyStore keyStore = sslBundle.bundle().getStores().getKeyStore();
            assertThat(keyStore).isNotNull();
            assertThat(keyStore.isKeyEntry("custom")).isTrue();
            assertThat(sslBundle.certificateChain()).startsWith("-----BEGIN CERTIFICATE-----");
        }

        @Test
        void whenJksIsConfiguredThenBundleIsBuiltFromIt() throws Exception {
            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(TestSslDevServicesProperties.jks(), SUPPORT), RESOURCE_LOADER);

            assertThat(sslBundle).isNotNull();
            KeyStore keyStore = sslBundle.bundle().getStores().getKeyStore();
            assertThat(keyStore).isNotNull();
            assertThat(keyStore.isKeyEntry("server")).isTrue();
            // The chain in the key store, server certificate and CA, is what the application trusts.
            KeyStore trustStore = sslBundle.trustBundle().getStores().getTrustStore();
            assertThat(trustStore).isNotNull();
            assertThat(trustStore.size()).isEqualTo(2);
        }

        @Test
        void whenJksKeyPasswordDiffersFromStorePasswordThenItMustBeConfigured() {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.jks();
            properties.getSsl().getJks().getKeystore().setLocation(SslTestCertificates.KEY_STORE_WITH_KEY_PASSWORD);

            assertThatIllegalStateException()
                    .isThrownBy(() -> DevServiceSslSupport.createBundle(definition(properties, SUPPORT), RESOURCE_LOADER))
                    .withMessage("Dev Service 'postgres': failed to load the SSL bundle from the 'ssl' properties")
                    .withStackTraceContaining("set the key store password, or the key password when it differs from the one of the key store");

            properties.getSsl().getJks().getKey().setPassword(SslTestCertificates.KEY_PASSWORD);
            assertThat(DevServiceSslSupport.createBundle(definition(properties, SUPPORT), RESOURCE_LOADER)).isNotNull();
        }

        @Test
        void whenBundleCannotBeLoadedThenThrowNamingTheService() {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.pem();
            properties.getSsl().getPem().getKeystore().setCertificate("classpath:ssl/missing.crt");

            assertThatIllegalStateException()
                    .isThrownBy(() -> DevServiceSslSupport.createBundle(definition(properties, SUPPORT), RESOURCE_LOADER))
                    .withMessage("Dev Service 'postgres': failed to load the SSL bundle from the 'ssl' properties")
                    .withCauseInstanceOf(RuntimeException.class);
        }

    }

    @Nested
    class TrustBundle {

        @Test
        void trustBundleTrustsTheCertificateChainOfTheDevService() throws Exception {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.pem();
            properties.getSsl().getPem().getKeystore().setCertificate(TestSslDevServicesProperties.CERTIFICATE_CHAIN);
            properties.getSsl().getPem().setProtocol("TLSv1.3");
            properties.getSsl().getPem().getOptions().setCiphers(Set.of("TLS_AES_256_GCM_SHA384"));
            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(properties, SUPPORT), RESOURCE_LOADER);

            SslBundle trustBundle = sslBundle.trustBundle();

            // The application presents no certificate of its own.
            assertThat(trustBundle.getStores().getKeyStore()).isNull();
            KeyStore trustStore = trustBundle.getStores().getTrustStore();
            assertThat(trustStore).isNotNull();
            assertThat(trustStore.size()).isEqualTo(2);
            assertThat(trustBundle.getProtocol()).isEqualTo("TLSv1.3");
            assertThat(trustBundle.getOptions().getCiphers()).containsExactly("TLS_AES_256_GCM_SHA384");
            // The bundle is complete: the trust managers can be created from it.
            assertThat(trustBundle.getManagers().getTrustManagers()).isNotEmpty();
        }

    }

    @Nested
    class Annotations {

        @Test
        void whenBundleIsPresentThenTrustStoreAndSslAnnotationsAreCreated() {
            TestSslDevServicesProperties properties = TestSslDevServicesProperties.pem();
            properties.getSsl().getPem().setProtocol("TLSv1.3");
            properties.getSsl().getPem().getOptions().setCiphers(Set.of("TLS_AES_256_GCM_SHA384"));
            properties.getSsl().getPem().getOptions().setEnabledProtocols(Set.of("TLSv1.3"));
            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(properties, SUPPORT), RESOURCE_LOADER);

            MergedAnnotations annotations = merged(DevServiceSslSupport.createAnnotations(sslBundle));

            // The certificate is inlined: no file of the application is referenced.
            assertThat(annotations.get(PemTrustStore.class).getString("certificate")).startsWith("-----BEGIN CERTIFICATE-----");
            assertThat(annotations.get(PemTrustStore.class).getString("privateKey")).isEmpty();
            assertThat(annotations.get(Ssl.class).getString("protocol")).isEqualTo("TLSv1.3");
            assertThat(annotations.get(Ssl.class).getStringArray("ciphers")).containsExactly("TLS_AES_256_GCM_SHA384");
            assertThat(annotations.get(Ssl.class).getStringArray("enabledProtocols")).containsExactly("TLSv1.3");
        }

        @Test
        void whenBundleHasNoOptionsThenSslAnnotationHasDefaults() {
            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(TestSslDevServicesProperties.pem(), SUPPORT), RESOURCE_LOADER);

            MergedAnnotations annotations = merged(DevServiceSslSupport.createAnnotations(sslBundle));

            assertThat(annotations.get(Ssl.class).getString("protocol")).isEqualTo("TLS");
            assertThat(annotations.get(Ssl.class).getStringArray("ciphers")).isEmpty();
            assertThat(annotations.get(Ssl.class).getStringArray("enabledProtocols")).isEmpty();
        }

        @Test
        void whenNoBundleThenOnlySslAnnotationIsCreated() {
            // Spring Boot extracts the certificate the container generates when @Ssl alone is present.
            MergedAnnotations annotations = merged(DevServiceSslSupport.createAnnotations(null));

            assertThat(annotations.isPresent(Ssl.class)).isTrue();
            assertThat(annotations.isPresent(PemTrustStore.class)).isFalse();
        }

    }

    @Nested
    class Configure {

        @Test
        void whenContainerMatchesThenConfigurerIsApplied() {
            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(TestSslDevServicesProperties.pem(), SUPPORT), RESOURCE_LOADER);
            var container = new TestPostgresContainer();

            DevServiceSslSupport.configure(container, definition(TestSslDevServicesProperties.pem(), SUPPORT), sslBundle.bundle());

            assertThat(container.getEnvMap()).containsEntry("SSL", "true");
        }

        @Test
        void whenContainerDoesNotMatchTheConfigurerThenTheCastFails() {
            DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(definition(TestSslDevServicesProperties.pem(), SUPPORT), RESOURCE_LOADER);

            // A programming error of the dev service, reported by the JVM with both types named.
            assertThatExceptionOfType(ClassCastException.class)
                    .isThrownBy(() -> DevServiceSslSupport.configure(new TestLinkContainer(), definition(TestSslDevServicesProperties.pem(), SUPPORT), sslBundle.bundle()))
                    .withMessageContaining(TestLinkContainer.class.getName())
                    .withMessageContaining(TestPostgresContainer.class.getName());
        }

    }

    /**
     * A definition built as the registry builds it, through the spec, with the given SSL support.
     */
    private static DevServiceDefinition definition(TestDevServicesProperties properties, @Nullable SslSupport sslSupport) {
        DevServiceSpec spec = new DevServiceSpec()
                .name("postgres")
                .description("Test Dev Service")
                .properties(properties)
                .container(TestPostgresContainer.class, TestPostgresContainer::new);
        if (sslSupport != null) {
            spec.ssl(sslSupport.configurer(), sslSupport.keyStoreRequirement());
        }
        return spec.build();
    }

    private static MergedAnnotations merged(List<Annotation> annotations) {
        return MergedAnnotations.from(annotations.toArray(new Annotation[0]));
    }

}
