package io.arconia.dev.services.core.registration;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.SslConfig;
import io.arconia.dev.services.api.config.SslDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceDefinition.SslSupport;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.core.ssl.SslBundleKeyEntry;
import io.arconia.dev.services.core.ssl.SslCertificate;

/**
 * Configures support for SSL in dev services.
 * <p>
 * The SSL bundle of a dev service has the meaning of the bundle a Spring Boot server is configured
 * with: its key store is what the dev service presents. The application trusts the certificate
 * chain the dev service presents, through the annotations Spring Boot reads from the container
 * bean to build the SSL bundle of the connection details.
 */
final class DevServiceSslSupport {

    /**
     * The prefix of the name the SSL bundle the application connects to a dev service with is
     * registered under in the {@code SslBundles} of the application.
     */
    static final String BUNDLE_NAME_PREFIX = "dev-services-";

    private DevServiceSslSupport() {}

    /**
     * The name the SSL bundle of the dev service with the given name is registered under.
     */
    static String bundleName(String serviceName) {
        return BUNDLE_NAME_PREFIX + serviceName;
    }

    /**
     * Check the SSL properties of a dev service against the SSL support it declares.
     */
    static void validate(String prefix, BaseDevServicesProperties properties, @Nullable SslSupport sslSupport) {
        if (!(properties instanceof SslDevServicesProperties sslProperties)) {
            Assert.isNull(sslSupport, () -> prefix + "SSL support is declared, but the properties don't implement SslDevServicesProperties: implement it, or remove the ssl(...) declaration");
            return;
        }
        SslConfig ssl = sslProperties.getSsl();
        Assert.notNull(ssl, () -> prefix + "the 'ssl' property cannot be null");
        Assert.notNull(ssl.getPem(), () -> prefix + "the 'ssl.pem' property cannot be null");
        Assert.notNull(ssl.getJks(), () -> prefix + "the 'ssl.jks' property cannot be null");
        if (!ssl.isEnabled()) {
            return;
        }
        Assert.notNull(sslSupport, () -> prefix + "the 'ssl.enabled' property is true, but the dev service declares no SSL support");

        boolean certificate = StringUtils.hasText(ssl.getPem().getKeystore().getCertificate());
        boolean privateKey = StringUtils.hasText(ssl.getPem().getKeystore().getPrivateKey());
        boolean jks = StringUtils.hasText(ssl.getJks().getKeystore().getLocation());
        Assert.state(!((certificate || privateKey) && jks),
                () -> prefix + "the 'ssl.pem' and 'ssl.jks' properties cannot be used together: configure one of them");
        Assert.state(certificate == privateKey, () -> prefix + (certificate
                ? "the 'ssl.pem.keystore.private-key' property is required with 'ssl.pem.keystore.certificate'"
                : "the 'ssl.pem.keystore.certificate' property is required with 'ssl.pem.keystore.private-key'"));
        Assert.state(certificate || jks || sslSupport.certificate() == SslCertificate.CONFIGURED_OR_GENERATED,
                () -> prefix + "the 'ssl.enabled' property is true, but no key store is configured: set 'ssl.pem.keystore.certificate' and 'ssl.pem.keystore.private-key', or 'ssl.jks.keystore.location'");
    }

    /**
     * Build the SSL bundle of the given dev service from its SSL properties, when SSL is
     * enabled and a key store is configured. A dev service with SSL enabled and no key store
     * has a container generating its own certificate: there is no bundle to build.
     */
    @Nullable
    static DevServiceSslBundle createBundle(DevServiceDefinition service, ClassLoader classLoader) {
        if (!service.isSslEnabled()) {
            return null;
        }
        SslConfig ssl = ((SslDevServicesProperties) service.properties()).getSsl();
        boolean pem = StringUtils.hasText(ssl.getPem().getKeystore().getCertificate());
        boolean jks = StringUtils.hasText(ssl.getJks().getKeystore().getLocation());
        if (!pem && !jks) {
            return null;
        }
        try {
            ResourceLoader resourceLoader = ApplicationResourceLoader.get(classLoader);
            SslBundle bundle = pem ? createPemBundle(ssl.getPem(), resourceLoader) : createJksBundle(ssl.getJks(), resourceLoader);
            SslBundleKeyEntry keyEntry = SslBundleKeyEntry.resolve(bundle);
            String certificateChain = new String(SslBundleFiles.certificateChain(bundle).getBytes(), StandardCharsets.US_ASCII);
            return new DevServiceSslBundle(bundle, keyEntry, certificateChain);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Dev Service '%s': failed to load the SSL bundle from the 'ssl' properties".formatted(service.name()), ex);
        }
    }

    /**
     * The bundle from the PEM properties. The store is loaded eagerly.
     */
    private static SslBundle createPemBundle(SslConfig.Pem pem, ResourceLoader resourceLoader) {
        SslConfig.Pem.Keystore store = pem.getKeystore();
        PemSslStore keyStore = PemSslStore.load(new PemSslStoreDetails(null, store.getCertificate(), store.getPrivateKey(), store.getPrivateKeyPassword()), resourceLoader);
        Assert.notNull(keyStore, "The PEM key store cannot be loaded");
        keyStore = keyStore.withAlias(pem.getKey().getAlias()).withPassword(pem.getKey().getPassword());
        return createBundle(new PemSslStoreBundle(keyStore, null), pem);
    }

    /**
     * The bundle from the JKS properties.
     */
    private static SslBundle createJksBundle(SslConfig.Jks jks, ResourceLoader resourceLoader) {
        SslConfig.Jks.Keystore store = jks.getKeystore();
        JksSslStoreDetails details = new JksSslStoreDetails(store.getType(), store.getProvider(), store.getLocation(), store.getPassword());
        return createBundle(new JksSslStoreBundle(details, null, resourceLoader), jks);
    }

    private static SslBundle createBundle(SslStoreBundle stores, SslConfig.Bundle bundle) {
        SslBundleKey key = SslBundleKey.of(bundle.getKey().getPassword(), bundle.getKey().getAlias());
        SslOptions options = SslOptions.of(bundle.getOptions().getCiphers(), bundle.getOptions().getEnabledProtocols());
        return SslBundle.of(stores, key, options, bundle.getProtocol());
    }

    /**
     * The bundle the application connects to the dev service with: it trusts the certificate
     * chain the dev service presents, with the protocol and options of the dev service bundle,
     * and presents no certificate of its own.
     */
    static SslBundle createTrustBundle(DevServiceSslBundle sslBundle) {
        try {
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);
            int index = 0;
            for (X509Certificate certificate : sslBundle.keyEntry().certificateChain()) {
                trustStore.setCertificateEntry("certificate-" + index++, certificate);
            }
            SslBundle bundle = sslBundle.bundle();
            return SslBundle.of(SslStoreBundle.of(null, null, trustStore), SslBundleKey.NONE, bundle.getOptions(), bundle.getProtocol());
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("Failed to build the trust bundle of the dev service", ex);
        }
    }

    /**
     * The annotations to declare on the container bean for Spring Boot to connect the
     * application over SSL: the certificate chain to trust, inline so that no file of the
     * application is referenced, and the SSL options. With no bundle, the container generates
     * its own certificate, which Spring Boot extracts when the {@code @Ssl} annotation alone is present.
     */
    static List<Annotation> createAnnotations(@Nullable DevServiceSslBundle sslBundle) {
        List<Annotation> annotations = new ArrayList<>();
        Map<String, Object> sslAttributes = new HashMap<>();
        if (sslBundle != null) {
            SslBundle bundle = sslBundle.bundle();
            sslAttributes.put("protocol", bundle.getProtocol());
            SslOptions options = bundle.getOptions();
            if (options.getCiphers() != null) {
                sslAttributes.put("ciphers", options.getCiphers());
            }
            if (options.getEnabledProtocols() != null) {
                sslAttributes.put("enabledProtocols", options.getEnabledProtocols());
            }
            annotations.add(AnnotationUtils.synthesizeAnnotation(Map.of("certificate", sslBundle.certificateChain()), PemTrustStore.class, null));
        }
        annotations.add(AnnotationUtils.synthesizeAnnotation(sslAttributes, Ssl.class, null));
        return annotations;
    }

    /**
     * Configure the given container to use SSL with the given bundle, through the configurer
     * the dev service declares.
     */
    @SuppressWarnings("unchecked")
    static void configure(GenericContainer<?> container, DevServiceDefinition service, SslBundle bundle) {
        SslSupport sslSupport = service.sslSupport();
        Assert.notNull(sslSupport, "sslSupport cannot be null");
        ((DevServiceSslConfigurer<GenericContainer<?>>) sslSupport.configurer()).configure(container, bundle);
    }

    /**
     * The SSL bundle of a dev service, resolved once at registration: the bundle, its key
     * entry, and the certificate chain it presents in PEM form, for the application to trust.
     */
    record DevServiceSslBundle(SslBundle bundle, SslBundleKeyEntry keyEntry, String certificateChain) {

        DevServiceSslBundle {
            Assert.notNull(bundle, "bundle cannot be null");
            Assert.notNull(keyEntry, "keyEntry cannot be null");
            Assert.hasText(certificateChain, "certificateChain cannot be null or empty");
        }

    }

}
