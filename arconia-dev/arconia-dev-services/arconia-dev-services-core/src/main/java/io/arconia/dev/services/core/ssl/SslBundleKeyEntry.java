package io.arconia.dev.services.core.ssl;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * The key entry of an {@link SslBundle}: the private key the bundle holds and the certificate
 * chain it presents, leaf certificate first. The entry is the one selected by the key alias of
 * the bundle, or the only key entry of its key store.
 *
 * @param alias the alias of the entry in the key store
 * @param privateKey the private key
 * @param certificateChain the certificate chain, leaf certificate first
 */
@Incubating
public record SslBundleKeyEntry(
        String alias,
        PrivateKey privateKey,
        List<X509Certificate> certificateChain
) {

    public SslBundleKeyEntry {
        Assert.hasText(alias, "alias cannot be null or empty");
        Assert.notNull(privateKey, "privateKey cannot be null");
        Assert.notEmpty(certificateChain, "certificateChain cannot be null or empty");
        certificateChain = List.copyOf(certificateChain);
    }

    /**
     * Resolve the key entry of the given bundle. The key password is the one of the bundle key,
     * or the one of the key store when none is set, as Spring Boot resolves it.
     *
     * @throws IllegalStateException if the bundle has no key store, the entry cannot be
     * determined or read, or it holds no private key with a certificate chain
     */
    public static SslBundleKeyEntry resolve(SslBundle bundle) {
        Assert.notNull(bundle, "bundle cannot be null");
        SslStoreBundle stores = bundle.getStores();
        KeyStore keyStore = stores.getKeyStore();
        Assert.notNull(keyStore, "The SSL bundle has no key store");
        String alias = resolveAlias(keyStore, bundle.getKey().getAlias());
        try {
            Assert.state(keyStore.isKeyEntry(alias), () -> "The SSL bundle key store has no key entry named '" + alias + "'");
            String password = bundle.getKey().getPassword();
            if (password == null) {
                password = stores.getKeyStorePassword();
            }
            Key key = keyStore.getKey(alias, (password != null) ? password.toCharArray() : null);
            Assert.state(key instanceof PrivateKey, () -> "The SSL bundle key store entry '" + alias + "' is not a private key");
            Certificate[] chain = keyStore.getCertificateChain(alias);
            Assert.state(chain != null && chain.length > 0, () -> "The SSL bundle key store entry '" + alias + "' has no certificate chain");
            return new SslBundleKeyEntry(alias, (PrivateKey) key, x509(chain, alias));
        } catch (UnrecoverableKeyException ex) {
            throw new IllegalStateException("The key '%s' of the SSL bundle cannot be read: set the key password when it differs from the one of the key store".formatted(alias), ex);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to read the key entry '%s' of the SSL bundle".formatted(alias), ex);
        }
    }

    /**
     * The alias to use: the given one, or the only key entry of the store. With several key
     * entries, the choice is not left to the unspecified iteration order of the store.
     */
    private static String resolveAlias(KeyStore keyStore, @Nullable String alias) {
        if (alias != null) {
            return alias;
        }
        List<String> keyAliases = new ArrayList<>();
        try {
            for (String candidate : Collections.list(keyStore.aliases())) {
                if (keyStore.isKeyEntry(candidate)) {
                    keyAliases.add(candidate);
                }
            }
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to read the entries of the SSL bundle key store", ex);
        }
        Assert.state(!keyAliases.isEmpty(), "The SSL bundle key store has no key entry");
        Assert.state(keyAliases.size() == 1,
                () -> "The SSL bundle key store has several key entries " + keyAliases + ": set the key alias to select one");
        return keyAliases.get(0);
    }

    private static List<X509Certificate> x509(Certificate[] chain, String alias) {
        List<X509Certificate> certificates = new ArrayList<>(chain.length);
        for (Certificate certificate : chain) {
            Assert.state(certificate instanceof X509Certificate,
                    () -> "The SSL bundle key store entry '" + alias + "' holds a certificate that is not an X.509 one");
            certificates.add((X509Certificate) certificate);
        }
        return certificates;
    }

}
