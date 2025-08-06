package io.arconia.docling.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import io.arconia.core.support.Incubating;
import io.arconia.docling.client.convert.request.ConvertDocumentRequest;
import io.arconia.docling.client.convert.response.ConvertDocumentResponse;
import io.arconia.docling.client.health.HealthCheckResponse;

/**
 * Client interface for interacting with the Docling API.
 */
@Incubating(since = "0.15.0")
public interface DoclingClient {

    @GetExchange(url = "/health")
    HealthCheckResponse health();

    @PostExchange(url = "/v1/convert/source")
    ConvertDocumentResponse convertSource(@RequestBody ConvertDocumentRequest request);

}
