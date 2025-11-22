package io.arconia.docling.actuate;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.Assert;

import ai.docling.api.serve.DoclingServeApi;
import ai.docling.api.serve.health.HealthCheckResponse;

/**
 * Health indicator for Docling Serve.
 *
 * @see DoclingServeApi
 */
public final class DoclingServeHealthIndicator extends AbstractHealthIndicator {

    private final DoclingServeApi doclingServeApi;

    public DoclingServeHealthIndicator(DoclingServeApi doclingServeApi) {
        Assert.notNull(doclingServeApi, "doclingServeApi cannot be null");
        this.doclingServeApi = doclingServeApi;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Assert.notNull(builder, "builder cannot be null");

        try {
            HealthCheckResponse healthCheckResponse = doclingServeApi.health();

            if ("ok".equals(healthCheckResponse.getStatus())) {
                builder.up();
            } else if (healthCheckResponse.getStatus() == null) {
                builder.down().withDetail("status", "unknown");
            } else {
                builder.down().withDetail("status", healthCheckResponse.getStatus());
            }
        } catch (Exception e) {
            builder.down(e);
        }
    }

}
