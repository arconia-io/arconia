package io.arconia.dev.services.core.ssl;

import io.arconia.core.support.Incubating;

/**
 * Whether a dev service with SSL enabled requires a key store, configured through its
 * {@code ssl} properties, for the certificate it presents.
 */
@Incubating
public enum SslKeyStoreRequirement {

    /**
     * A key store is required when SSL is enabled: enabling SSL without one fails at startup.
     */
    REQUIRED,

    /**
     * A key store is optional: the container generates a certificate of its own when none is
     * configured. The configurer of the dev service is then not called, and only the
     * {@code @Ssl} annotation is declared on the container bean, so the protocol and options
     * of the {@code ssl} properties don't apply. Only for a container whose Spring Boot
     * connection details factory extracts the generated certificate when the {@code @Ssl}
     * annotation alone is declared, as the Elasticsearch one does.
     */
    OPTIONAL

}
