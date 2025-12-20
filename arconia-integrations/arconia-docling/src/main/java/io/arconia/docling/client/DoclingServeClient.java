package io.arconia.docling.client;

import java.util.concurrent.CompletionStage;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.clear.request.ClearConvertersRequest;
import ai.docling.serve.api.clear.request.ClearResultsRequest;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.task.request.TaskResultRequest;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Client interface for interacting with the Docling Serve API.
 */
public interface DoclingServeClient extends DoclingServeApi {

    // HEALTH

    @GetExchange(url = "/health")
    @Override
    HealthCheckResponse health();

    // CONVERT

    @PostExchange(url = "/v1/convert/source")
    @Override
    ConvertDocumentResponse convertSource(@RequestBody ConvertDocumentRequest request);

    @Override
    default CompletionStage<ConvertDocumentResponse> convertSourceAsync(ConvertDocumentRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    // CHUNK

    @PostExchange(url = "/v1/chunk/hierarchical/source")
    @Override
    ChunkDocumentResponse chunkSourceWithHierarchicalChunker(@RequestBody HierarchicalChunkDocumentRequest hierarchicalChunkDocumentRequest);

    @PostExchange(url = "/v1/chunk/hybrid/source")
    @Override
    ChunkDocumentResponse chunkSourceWithHybridChunker(@RequestBody HybridChunkDocumentRequest hybridChunkDocumentRequest);

    @Override
    default CompletionStage<ChunkDocumentResponse> chunkSourceWithHierarchicalChunkerAsync(HierarchicalChunkDocumentRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    @Override
    default CompletionStage<ChunkDocumentResponse> chunkSourceWithHybridChunkerAsync(HybridChunkDocumentRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    // CLEAR

    @Override
    default ClearResponse clearConverters(ClearConvertersRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    @Override
    default ClearResponse clearResults(ClearResultsRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    // TASK

    @Override
    default TaskStatusPollResponse pollTaskStatus(TaskStatusPollRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    @Override
    default ConvertDocumentResponse convertTaskResult(TaskResultRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    @Override
    default ChunkDocumentResponse chunkTaskResult(TaskResultRequest request) {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

    // BUILDER

    @Override
    default <T extends DoclingServeApi, B extends DoclingApiBuilder<T, B>> DoclingApiBuilder<T, B> toBuilder() {
        throw new UnsupportedOperationException("The Arconia DoclingClient does not support this operation.");
    }

}
