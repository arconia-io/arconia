package io.arconia.ai.document.docling;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.options.ChunkerOptions;
import ai.docling.serve.api.chunk.request.options.HierarchicalChunkerOptions;
import ai.docling.serve.api.chunk.request.options.HybridChunkerOptions;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.source.Source;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A {@link DocumentReader} using Docling to read, convert, and chunk documents.
 */
public final class DoclingDocumentReader implements DocumentReader {

    private final DoclingServeApi doclingServeApi;
    private final ConvertDocumentOptions convertOptions;
    private final ChunkerOptions chunkerOptions;
    private final DoclingDocumentParser documentParser;
    private final Map<String, Object> metadata;
    private final List<Resource> files;
    private final List<URI> urls;

    private DoclingDocumentReader(DoclingServeApi doclingServeApi, ConvertDocumentOptions convertOptions, ChunkerOptions chunkerOptions, DoclingDocumentParser documentParser, Map<String, Object> metadata, List<Resource> files, List<URI> urls) {
        Assert.notNull(doclingServeApi, "doclingServeApi cannot be null");
        Assert.notNull(convertOptions, "convertOptions cannot be null");
        Assert.notNull(chunkerOptions, "chunkerOptions cannot be null");
        Assert.notNull(metadata, "metadata cannot be null");
        Assert.noNullElements(metadata.keySet(), "metadata cannot contain null keys");
        Assert.noNullElements(metadata.values(), "metadata cannot contain null values");
        Assert.notNull(files, "files cannot be null");
        Assert.noNullElements(files, "files cannot contain null elements");
        Assert.notNull(urls, "urls cannot be null");
        Assert.noNullElements(urls, "urls cannot contain null elements");
        Assert.isTrue(!files.isEmpty() || !urls.isEmpty(), "at least one file or url must be provided");

        this.doclingServeApi = doclingServeApi;
        this.convertOptions = convertOptions;
        this.chunkerOptions = chunkerOptions;
        this.documentParser = documentParser;
        this.metadata = metadata;
        this.files = files;
        this.urls = urls;
    }

    @Override
    public List<Document> get() {
        List<Source> sources = buildSources();

        ChunkDocumentResponse response = switch (chunkerOptions) {
            case HierarchicalChunkerOptions hierarchicalChunkerOptions -> doclingServeApi.chunkSourceWithHierarchicalChunker(
                    HierarchicalChunkDocumentRequest.builder()
                            .sources(sources)
                            .options(convertOptions)
                            .chunkingOptions(hierarchicalChunkerOptions)
                            .includeConvertedDoc(true)
                            .build());
            case HybridChunkerOptions hybridChunkerOptions -> doclingServeApi.chunkSourceWithHybridChunker(
                    HybridChunkDocumentRequest.builder()
                            .sources(sources)
                            .options(convertOptions)
                            .chunkingOptions(hybridChunkerOptions)
                            .includeConvertedDoc(true)
                            .build());
        };

        return documentParser.parse(response.getChunks(), metadata);
    }

    private List<Source> buildSources() {
        List<Source> sources = new ArrayList<>();

        for (Resource file : files) {
            String base64File;
            try {
                base64File = Base64.getEncoder().encodeToString(file.getContentAsByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Error reading file: " + file.getFilename(), e);
            }
            sources.add(FileSource.builder()
                    .filename(file.getFilename() != null ? file.getFilename() : "file")
                    .base64String(base64File)
                    .build());
        }

        for (URI url : urls) {
            sources.add(HttpSource.builder().url(url).build());
        }

        return sources;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DoclingServeApi doclingServeApi;
        private ConvertDocumentOptions convertOptions = ConvertDocumentOptions.builder().build();
        private ChunkerOptions chunkerOptions = HierarchicalChunkerOptions.builder().build();
        private DoclingDocumentParser documentParser = new DefaultDoclingDocumentParser();
        private Map<String, Object> metadata = new HashMap<>();
        private List<Resource> files = new ArrayList<>();
        private List<URI> urls = new ArrayList<>();

        private Builder() {}

        public Builder doclingServeApi(DoclingServeApi doclingServeApi) {
            this.doclingServeApi = doclingServeApi;
            return this;
        }

        public Builder convertOptions(ConvertDocumentOptions convertDocumentOptions) {
            this.convertOptions = convertDocumentOptions;
            return this;
        }

        public Builder chunkerOptions(ChunkerOptions chunkerOptions) {
            this.chunkerOptions = chunkerOptions;
            return this;
        }

        public Builder documentParser(DoclingDocumentParser documentParser) {
            this.documentParser = documentParser;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            Assert.notNull(metadata, "metadata cannot be null");
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder files(List<Resource> files) {
            Assert.notNull(files, "files cannot be null");
            this.files = files;
            return this;
        }

        public Builder files(Resource... files) {
            this.files.addAll(List.of(files));
            return this;
        }

        public Builder urls(List<URI> urls) {
            Assert.notNull(urls, "urls cannot be null");
            this.urls = urls;
            return this;
        }

        public Builder urls(String... urls) {
            for (String url : urls) {
                Assert.notNull(url, "url cannot be null");
                this.urls.add(URI.create(url));
            }
            return this;
        }

        public DoclingDocumentReader build() {
            return new DoclingDocumentReader(doclingServeApi, convertOptions, chunkerOptions, documentParser, metadata, files, urls);
        }

    }

}
