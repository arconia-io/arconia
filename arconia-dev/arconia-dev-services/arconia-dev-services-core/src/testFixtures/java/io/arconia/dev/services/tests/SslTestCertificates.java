package io.arconia.dev.services.tests;

/**
 * The test certificates on the test classpath, for tests enabling SSL for a dev service:
 * a certificate for {@code localhost} issued by a test certificate authority, its key, and the
 * same material as PKCS12 keystores. All certificates are valid for a hundred years.
 */
public final class SslTestCertificates {

    /**
     * The certificate a dev service presents, issued by {@link #CA_CERTIFICATE}.
     */
    public static final String CERTIFICATE = "classpath:ssl/server.crt";

    /**
     * The certificate chain: the server certificate followed by the issuing CA certificate.
     */
    public static final String CERTIFICATE_CHAIN = "classpath:ssl/server-chain.crt";

    public static final String PRIVATE_KEY = "classpath:ssl/server.key";

    public static final String CA_CERTIFICATE = "classpath:ssl/ca.crt";

    /**
     * A PKCS12 keystore with the server key under the {@link #KEY_ALIAS} alias and the CA
     * certificate in its chain.
     */
    public static final String KEY_STORE = "classpath:ssl/server.p12";

    public static final String KEY_ALIAS = "server";

    /**
     * A PKCS12 truststore with the CA certificate.
     */
    public static final String TRUST_STORE = "classpath:ssl/truststore.p12";

    public static final String STORE_PASSWORD = "secret";

    /**
     * A JKS keystore with the server key under the {@link #KEY_ALIAS} alias, protected by
     * {@link #KEY_PASSWORD} rather than the store password.
     */
    public static final String KEY_STORE_WITH_KEY_PASSWORD = "classpath:ssl/server-keypass.jks";

    public static final String KEY_PASSWORD = "otherpass";

    /**
     * A PKCS12 keystore with two key entries, {@code server} and {@code second}.
     */
    public static final String KEY_STORE_WITH_TWO_KEYS = "classpath:ssl/server-two-keys.p12";

    private SslTestCertificates() {}

    /**
     * The properties enabling SSL for the given dev service with the test certificate and key in PEM form.
     */
    public static String[] pemProperties(String serviceName) {
        String prefix = "arconia.dev.services." + serviceName + ".ssl.";
        return new String[] {
                prefix + "enabled=true",
                prefix + "pem.keystore.certificate=" + CERTIFICATE,
                prefix + "pem.keystore.private-key=" + PRIVATE_KEY
        };
    }

    /**
     * The properties enabling SSL for the given dev service with the test keystore.
     */
    public static String[] jksProperties(String serviceName) {
        String prefix = "arconia.dev.services." + serviceName + ".ssl.";
        return new String[] {
                prefix + "enabled=true",
                prefix + "jks.keystore.location=" + KEY_STORE,
                prefix + "jks.keystore.password=" + STORE_PASSWORD
        };
    }

}
