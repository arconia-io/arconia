package io.arconia.docling.actuate;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.docling.client.DoclingClient;
import io.arconia.docling.client.health.HealthCheckResponse;

/**
 * Health indicator for Docling.
 *
 * @see DoclingClient
 */
@Incubating(since = "0.15.0")
public final class DoclingHealthIndicator extends AbstractHealthIndicator {

    private final DoclingClient doclingClient;

    public DoclingHealthIndicator(DoclingClient doclingClient) {
        Assert.notNull(doclingClient, "doclingClient cannot be null");
        this.doclingClient = doclingClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Assert.notNull(builder, "builder cannot be null");

        try {
            HealthCheckResponse healthCheckResponse = doclingClient.health();

            if ("ok".equals(healthCheckResponse.status())) {
                builder.up();
            } else if (healthCheckResponse.status() == null) {
                builder.down().withDetail("status", "unknown");
            } else {
                builder.down().withDetail("status", healthCheckResponse.status());
            }
        } catch (Exception e) {
            builder.down(e);
        }
    }

}
