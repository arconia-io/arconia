package io.arconia.dev.services.core.ssl;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;
import org.testcontainers.images.builder.Transferable;

import io.arconia.core.support.Incubating;

/**
 * The certificates and keys of an {@link SslBundle} as PEM files to copy into a container, so
 * that a dev service gets what its service expects whatever form the bundle was configured in.
 * <p>
 * The key entry used is the one selected by the key alias of the bundle, or the only key entry
 * of its key store. Private keys are written unencrypted, in PKCS#8 form.
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
     * The certificate authorities to give a service that requires a file of them: the
     * certificate chain the bundle presents, since the bundle of a dev service has no trust
     * store of its own.
     */
    public static Transferable certificateAuthorities(SslBundle bundle) {
        return certificateChain(bundle);
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
