package io.arconia.docling.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;

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

    @PostExchange(url = "/v1/chunk/hierarchical/source")
    @Override
    ChunkDocumentResponse chunkSourceWithHierarchicalChunker(@RequestBody HierarchicalChunkDocumentRequest hierarchicalChunkDocumentRequest);

    @PostExchange(url = "/v1/chunk/hybrid/source")
    @Override
    ChunkDocumentResponse chunkSourceWithHybridChunker(@RequestBody HybridChunkDocumentRequest hybridChunkDocumentRequest);

    @Override
    default <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

}
