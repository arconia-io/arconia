package io.arconia.dev.services.core.registration;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.autoconfigure.ssl.SslBundleProperties;
import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.DevServicesProperties;
import io.arconia.dev.services.api.config.SslConfig;
import io.arconia.dev.services.api.config.SslDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceDefinition.SslSupport;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.core.ssl.SslKeyStoreRequirement;

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
    static void validate(String prefix, DevServicesProperties properties, @Nullable SslSupport sslSupport) {
        if (!(properties instanceof SslDevServicesProperties sslProperties)) {
            Assert.isNull(sslSupport, () -> prefix + "SSL support is declared, but the properties don't implement SslDevServicesProperties: implement it, or remove the ssl(...) declaration");
            return;
        }
        SslConfig ssl = sslProperties.getSsl();
        Assert.notNull(ssl, () -> prefix + "the 'ssl' property cannot be null");
        if (!ssl.isEnabled()) {
            return;
        }
        Assert.notNull(sslSupport, () -> prefix + "the 'ssl.enabled' property is true, but the dev service declares no SSL support");

        boolean certificate = StringUtils.hasText(ssl.getPem().getKeystore().getCertificate());
        boolean privateKey = StringUtils.hasText(ssl.getPem().getKeystore().getPrivateKey());
        boolean jks = hasJksKeyStore(ssl);
        Assert.state(!((certificate || privateKey) && jks),
                () -> prefix + "the 'ssl.pem' and 'ssl.jks' properties cannot be used together: configure one of them");
        Assert.state(certificate == privateKey, () -> prefix + (certificate
                ? "the 'ssl.pem.keystore.private-key' property is required with 'ssl.pem.keystore.certificate'"
                : "the 'ssl.pem.keystore.certificate' property is required with 'ssl.pem.keystore.private-key'"));
        Assert.state(certificate || jks || sslSupport.keyStoreRequirement() == SslKeyStoreRequirement.OPTIONAL,
                () -> prefix + "the 'ssl.enabled' property is true, but no key store is configured: set 'ssl.pem.keystore.certificate' and 'ssl.pem.keystore.private-key', or 'ssl.jks.keystore.location'");
    }

    /**
     * Build the SSL bundle of the given dev service from its SSL properties, when SSL is
     * enabled and a key store is configured, with the resources resolved as the application
     * resolves its own. A dev service with SSL enabled and no key store has a container
     * generating its own certificate: there is no bundle to build. The key store is loaded here,
     * so that a bundle that cannot be loaded fails at registration, naming the dev service.
     */
    @Nullable
    static DevServiceSslBundle createBundle(DevServiceDefinition service, ResourceLoader resourceLoader) {
        if (!service.isSslEnabled()) {
            return null;
        }
        SslConfig ssl = ((SslDevServicesProperties) service.properties()).getSsl();
        boolean pem = hasPemKeyStore(ssl);
        if (!pem && !hasJksKeyStore(ssl)) {
            return null;
        }
        try {
            ResourceLoader applicationResourceLoader = ApplicationResourceLoader.get(resourceLoader);
            SslBundle bundle = pem
                    ? PropertiesSslBundle.get(pemProperties(ssl.getPem()), applicationResourceLoader)
                    : PropertiesSslBundle.get(jksProperties(ssl.getJks()), applicationResourceLoader);
            String certificateChain = new String(SslBundleFiles.certificateChain(bundle).getBytes(), StandardCharsets.US_ASCII);
            return new DevServiceSslBundle(bundle, createTrustBundle(bundle, certificateChain), certificateChain);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Dev Service '%s': failed to load the SSL bundle from the 'ssl' properties".formatted(service.name()), ex);
        }
    }

    private static boolean hasPemKeyStore(SslConfig ssl) {
        return StringUtils.hasText(ssl.getPem().getKeystore().getCertificate());
    }

    private static boolean hasJksKeyStore(SslConfig ssl) {
        return StringUtils.hasText(ssl.getJks().getKeystore().getLocation());
    }

    /**
     * The PEM properties as the Spring Boot ones they mirror, for Spring Boot to build the bundle.
     */
    private static PemSslBundleProperties pemProperties(SslConfig.Pem pem) {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(pem.getKeystore().getCertificate());
        properties.getKeystore().setPrivateKey(pem.getKeystore().getPrivateKey());
        properties.getKeystore().setPrivateKeyPassword(pem.getKeystore().getPrivateKeyPassword());
        copyBundleProperties(pem, properties);
        return properties;
    }

    /**
     * The JKS properties as the Spring Boot ones they mirror, for Spring Boot to build the bundle.
     */
    private static JksSslBundleProperties jksProperties(SslConfig.Jks jks) {
        JksSslBundleProperties properties = new JksSslBundleProperties();
        properties.getKeystore().setType(jks.getKeystore().getType());
        properties.getKeystore().setProvider(jks.getKeystore().getProvider());
        properties.getKeystore().setLocation(jks.getKeystore().getLocation());
        properties.getKeystore().setPassword(jks.getKeystore().getPassword());
        copyBundleProperties(jks, properties);
        return properties;
    }

    private static void copyBundleProperties(SslConfig.Bundle bundle, SslBundleProperties properties) {
        properties.getKey().setAlias(bundle.getKey().getAlias());
        properties.getKey().setPassword(bundle.getKey().getPassword());
        properties.getOptions().setCiphers(bundle.getOptions().getCiphers());
        properties.getOptions().setEnabledProtocols(bundle.getOptions().getEnabledProtocols());
        properties.setProtocol(bundle.getProtocol());
    }

    /**
     * The bundle the application connects to the dev service with: it trusts the certificate
     * chain the dev service presents, with the protocol and options of the dev service bundle,
     * and presents no certificate of its own. It is built from the chain in PEM form the same
     * way Spring Boot builds it from the {@code @PemTrustStore} annotation.
     */
    private static SslBundle createTrustBundle(SslBundle bundle, String certificateChain) {
        PemSslStoreBundle stores = new PemSslStoreBundle(null, PemSslStoreDetails.forCertificate(certificateChain));
        return SslBundle.of(stores, SslBundleKey.NONE, bundle.getOptions(), bundle.getProtocol());
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
        ((DevServiceSslConfigurer<Container<?>>) sslSupport.configurer()).configure(container, bundle);
    }

    /**
     * The SSL bundle of a dev service, resolved once at registration: the bundle of the dev
     * service, the bundle the application connects to it with, and the certificate chain the dev
     * service presents in PEM form, for the application to trust.
     */
    record DevServiceSslBundle(SslBundle bundle, SslBundle trustBundle, String certificateChain) {

        DevServiceSslBundle {
            Assert.notNull(bundle, "bundle cannot be null");
            Assert.notNull(trustBundle, "trustBundle cannot be null");
            Assert.hasText(certificateChain, "certificateChain cannot be null or empty");
        }

    }

}
