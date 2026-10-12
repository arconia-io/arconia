package io.arconia.dev.services.core.ssl;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;
import org.testcontainers.images.builder.Transferable;

import io.arconia.core.support.Incubating;

/**
 * The certificates and keys of an {@link SslBundle} as PEM files to copy into a container, so
 * that a dev service gets what its service expects whatever form the bundle was configured in.
 * <p>
 * The key entry used is the one of {@link SslBundleKeyEntry}. Private keys are written
 * unencrypted, in PKCS#8 form.
 */
@Incubating
public final class SslBundleFiles {

    private static final Base64.Encoder PEM_ENCODER = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));

    private SslBundleFiles() {}

    /**
     * The certificate chain the bundle presents, leaf certificate first.
     */
    public static Transferable certificateChain(SslBundle bundle) {
        return pem(certificates(SslBundleKeyEntry.resolve(bundle).certificateChain()));
    }

    /**
     * The private key of the bundle.
     */
    public static Transferable privateKey(SslBundle bundle) {
        return pem(privateKey(SslBundleKeyEntry.resolve(bundle).privateKey()));
    }

    /**
     * The certificate chain and the private key of the bundle in a single file, as some
     * services expect them, certificates first.
     */
    public static Transferable certificateChainWithPrivateKey(SslBundle bundle) {
        SslBundleKeyEntry keyEntry = SslBundleKeyEntry.resolve(bundle);
        return pem(certificates(keyEntry.certificateChain()) + privateKey(keyEntry.privateKey()));
    }

    /**
     * The certificate authorities to give a service that requires a file of them: the entries
     * of the trust store of the bundle or, when it has none, the certificate chain the bundle
     * presents.
     */
    public static Transferable certificateAuthorities(SslBundle bundle) {
        Assert.notNull(bundle, "bundle cannot be null");
        List<Certificate> certificates = new ArrayList<>();
        KeyStore trustStore = bundle.getStores().getTrustStore();
        if (trustStore != null) {
            try {
                for (String alias : Collections.list(trustStore.aliases())) {
                    if (trustStore.isCertificateEntry(alias)) {
                        certificates.add(trustStore.getCertificate(alias));
                    }
                }
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Failed to read the trust store of the SSL bundle", ex);
            }
        }
        if (certificates.isEmpty()) {
            certificates.addAll(SslBundleKeyEntry.resolve(bundle).certificateChain());
        }
        return pem(certificates(certificates));
    }

    private static String certificates(List<? extends Certificate> certificates) {
        StringBuilder pem = new StringBuilder();
        for (Certificate certificate : certificates) {
            try {
                pem.append(encode("CERTIFICATE", certificate.getEncoded()));
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Failed to encode a certificate of the SSL bundle", ex);
            }
        }
        return pem.toString();
    }

    private static String privateKey(PrivateKey key) {
        byte[] encoded = key.getEncoded();
        Assert.state(encoded != null, "The private key of the SSL bundle cannot be encoded");
        return encode("PRIVATE KEY", encoded);
    }

    private static String encode(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n" + PEM_ENCODER.encodeToString(der) + "\n-----END " + type + "-----\n";
    }

    private static Transferable pem(String content) {
        return Transferable.of(content.getBytes(StandardCharsets.US_ASCII));
    }

}
