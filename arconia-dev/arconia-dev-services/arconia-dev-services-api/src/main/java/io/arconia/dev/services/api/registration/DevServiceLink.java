package io.arconia.dev.services.api.registration;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * A link exposed by a dev service, such as a management console or a telemetry endpoint.
 *
 * @param label the human-readable label of the link (e.g. {@code Grafana}, {@code OTLP/HTTP})
 * @param url the URL the link points to
 */
@Incubating
public record DevServiceLink(String label, String url) {

    public DevServiceLink {
        Assert.hasText(label, "label cannot be null or empty");
        Assert.hasText(url, "url cannot be null or empty");
    }

}
