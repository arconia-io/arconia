package io.arconia.dev.services.api.config;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;

/**
 * SSL configuration of a dev service.
 * <p>
 * The bundle has the meaning of the bundle a Spring Boot server is configured with: the key
 * store holds the certificate and private key the dev service presents. The application is
 * configured to trust it automatically.
 */
@Incubating
public class SslConfig {

    /**
     * Whether to enable SSL support for the dev service.
     */
    private boolean enabled = false;

    /**
     * SSL bundle of the dev service from PEM-encoded certificate and private key.
     */
    private final Pem pem = new Pem();

    /**
     * SSL bundle of the dev service from a Java keystore.
     */
    private final Jks jks = new Jks();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Pem getPem() {
        return pem;
    }

    public Jks getJks() {
        return jks;
    }

    /**
     * The properties common to both forms of the bundle.
     */
    public abstract static class Bundle {

        /**
         * Key details for the bundle.
         */
        private final Key key = new Key();

        /**
         * Options for the SSL connection.
         */
        private final Options options = new Options();

        /**
         * SSL Protocol to use.
         */
        private String protocol = "TLS";

        public Key getKey() {
            return key;
        }

        public Options getOptions() {
            return options;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

    }

    /**
     * The bundle from PEM-encoded certificate and private key.
     */
    public static class Pem extends Bundle {

        /**
         * Keystore properties.
         */
        private final Keystore keystore = new Keystore();

        public Keystore getKeystore() {
            return keystore;
        }

        /**
         * A PEM-encoded keystore.
         */
        public static class Keystore {

            /**
             * Location or content of the certificate or certificate chain in PEM format.
             */
            @Nullable
            private String certificate;

            /**
             * Location or content of the private key in PEM format.
             */
            @Nullable
            private String privateKey;

            /**
             * Password used to decrypt an encrypted private key.
             */
            @Nullable
            private String privateKeyPassword;

            @Nullable
            public String getCertificate() {
                return certificate;
            }

            public void setCertificate(@Nullable String certificate) {
                this.certificate = certificate;
            }

            @Nullable
            public String getPrivateKey() {
                return privateKey;
            }

            public void setPrivateKey(@Nullable String privateKey) {
                this.privateKey = privateKey;
            }

            @Nullable
            public String getPrivateKeyPassword() {
                return privateKeyPassword;
            }

            public void setPrivateKeyPassword(@Nullable String privateKeyPassword) {
                this.privateKeyPassword = privateKeyPassword;
            }

        }

    }

    /**
     * The bundle from a Java keystore.
     */
    public static class Jks extends Bundle {

        /**
         * Keystore properties.
         */
        private final Keystore keystore = new Keystore();

        public Keystore getKeystore() {
            return keystore;
        }

        /**
         * A Java keystore.
         */
        public static class Keystore {

            /**
             * Type of the store to create, e.g. JKS.
             */
            @Nullable
            private String type;

            /**
             * Provider for the store.
             */
            @Nullable
            private String provider;

            /**
             * Location of the resource containing the store content.
             */
            @Nullable
            private String location;

            /**
             * Password used to access the store.
             */
            @Nullable
            private String password;

            @Nullable
            public String getType() {
                return type;
            }

            public void setType(@Nullable String type) {
                this.type = type;
            }

            @Nullable
            public String getProvider() {
                return provider;
            }

            public void setProvider(@Nullable String provider) {
                this.provider = provider;
            }

            @Nullable
            public String getLocation() {
                return location;
            }

            public void setLocation(@Nullable String location) {
                this.location = location;
            }

            @Nullable
            public String getPassword() {
                return password;
            }

            public void setPassword(@Nullable String password) {
                this.password = password;
            }

        }

    }

    /**
     * The key in the key store.
     */
    public static class Key {

        /**
         * The password used to access the key in the key store.
         */
        @Nullable
        private String password;

        /**
         * The alias that identifies the key in the key store.
         */
        @Nullable
        private String alias;

        @Nullable
        public String getPassword() {
            return password;
        }

        public void setPassword(@Nullable String password) {
            this.password = password;
        }

        @Nullable
        public String getAlias() {
            return alias;
        }

        public void setAlias(@Nullable String alias) {
            this.alias = alias;
        }

    }

    /**
     * The options of the SSL connection.
     */
    public static class Options {

        /**
         * Supported SSL ciphers.
         */
        @Nullable
        private Set<String> ciphers;

        /**
         * Enabled SSL protocols.
         */
        @Nullable
        private Set<String> enabledProtocols;

        @Nullable
        public Set<String> getCiphers() {
            return ciphers;
        }

        public void setCiphers(@Nullable Set<String> ciphers) {
            this.ciphers = ciphers;
        }

        @Nullable
        public Set<String> getEnabledProtocols() {
            return enabledProtocols;
        }

        public void setEnabledProtocols(@Nullable Set<String> enabledProtocols) {
            this.enabledProtocols = enabledProtocols;
        }

    }

}
