package io.arconia.docling.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import ai.docling.api.serve.DoclingServeApi;
import ai.docling.api.serve.convert.request.ConvertDocumentRequest;
import ai.docling.api.serve.convert.response.ConvertDocumentResponse;
import ai.docling.api.serve.health.HealthCheckResponse;

/**
 * Client interface for interacting with the Docling Serve API.
 */
public interface DoclingServeClient extends DoclingServeApi {

    @GetExchange(url = "/health")
    @Override
    HealthCheckResponse health();

    @PostExchange(url = "/v1/convert/source")
    @Override
    ConvertDocumentResponse convertSource(@RequestBody ConvertDocumentRequest request);

    @Override
    default <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

}
