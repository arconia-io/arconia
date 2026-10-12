package io.arconia.dev.services.core;

import io.arconia.dev.services.api.config.SslConfig;
import io.arconia.dev.services.api.config.SslDevServicesProperties;
import io.arconia.dev.services.tests.SslTestCertificates;

/**
 * Configuration properties for a dev service under test that supports SSL. The {@code ssl}
 * values are mutable, so that a test configures them as a user would through properties, and
 * shared by the copies the {@code with*} methods return.
 */
public class TestSslDevServicesProperties extends TestDevServicesProperties implements SslDevServicesProperties {

    public static final String CERTIFICATE = SslTestCertificates.CERTIFICATE;

    public static final String CERTIFICATE_CHAIN = SslTestCertificates.CERTIFICATE_CHAIN;

    public static final String PRIVATE_KEY = SslTestCertificates.PRIVATE_KEY;

    public static final String CA_CERTIFICATE = SslTestCertificates.CA_CERTIFICATE;

    public static final String KEY_STORE = SslTestCertificates.KEY_STORE;

    public static final String STORE_PASSWORD = SslTestCertificates.STORE_PASSWORD;

    private SslConfig ssl = new SslConfig();

    /**
     * Properties enabling SSL for the dev service with the test certificate and key in PEM form.
     */
    public static TestSslDevServicesProperties pem() {
        TestSslDevServicesProperties properties = new TestSslDevServicesProperties();
        properties.getSsl().setEnabled(true);
        properties.getSsl().getPem().getKeystore().setCertificate(CERTIFICATE);
        properties.getSsl().getPem().getKeystore().setPrivateKey(PRIVATE_KEY);
        return properties;
    }

    /**
     * Properties enabling SSL for the dev service with the test keystore.
     */
    public static TestSslDevServicesProperties jks() {
        TestSslDevServicesProperties properties = new TestSslDevServicesProperties();
        properties.getSsl().setEnabled(true);
        properties.getSsl().getJks().getKeystore().setLocation(KEY_STORE);
        properties.getSsl().getJks().getKeystore().setPassword(STORE_PASSWORD);
        return properties;
    }

    /**
     * Properties enabling SSL with no key store, as for a dev service generating its own certificate.
     */
    public static TestSslDevServicesProperties enabledWithoutKeyStore() {
        TestSslDevServicesProperties properties = new TestSslDevServicesProperties();
        properties.getSsl().setEnabled(true);
        return properties;
    }

    @Override
    public SslConfig getSsl() {
        return ssl;
    }

    @Override
    protected TestSslDevServicesProperties copy() {
        TestSslDevServicesProperties copy = new TestSslDevServicesProperties();
        copyInto(copy);
        copy.ssl = ssl;
        return copy;
    }

}
