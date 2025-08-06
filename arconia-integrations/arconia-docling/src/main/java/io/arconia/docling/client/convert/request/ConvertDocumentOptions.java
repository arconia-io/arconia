package io.arconia.docling.client.convert.request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.docling.client.convert.request.options.ImageRefMode;
import io.arconia.docling.client.convert.request.options.InputFormat;
import io.arconia.docling.client.convert.request.options.OcrEngine;
import io.arconia.docling.client.convert.request.options.OutputFormat;
import io.arconia.docling.client.convert.request.options.PdfBackend;
import io.arconia.docling.client.convert.request.options.PictureDescriptionApi;
import io.arconia.docling.client.convert.request.options.PictureDescriptionLocal;
import io.arconia.docling.client.convert.request.options.ProcessingPipeline;
import io.arconia.docling.client.convert.request.options.TableFormerMode;

/**
 * Options for configuring the document conversion process with Docling.
 */
@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConvertDocumentOptions(

        @JsonProperty("from_formats")
        @Nullable
        List<InputFormat> fromFormats,

        @JsonProperty("to_formats")
        @Nullable
        List<OutputFormat> toFormats,

        @JsonProperty("image_export_mode")
        @Nullable
        ImageRefMode imageExportMode,

        @JsonProperty("do_ocr")
        @Nullable
        Boolean doOcr,

        @JsonProperty("force_ocr")
        @Nullable
        Boolean forceOcr,

        @JsonProperty("ocr_engine")
        @Nullable
        OcrEngine ocrEngine,

        @JsonProperty("ocr_lang")
        @Nullable
        List<String> ocrLang,

        @JsonProperty("pdf_backend")
        @Nullable
        PdfBackend pdfBackend,

        @JsonProperty("table_mode")
        @Nullable
        TableFormerMode tableMode,

        @JsonProperty("pipeline")
        @Nullable
        ProcessingPipeline pipeline,

        @JsonProperty("page_range")
        @Nullable
        Integer @Nullable [] pageRange,

        @JsonProperty("document_timeout")
        @Nullable
        Duration documentTimeout,

        @JsonProperty("abort_on_error")
        @Nullable
        Boolean abortOnError,

        @JsonProperty("do_table_structure")
        @Nullable
        Boolean doTableStructure,

        @JsonProperty("include_images")
        @Nullable
        Boolean includeImages,

        @JsonProperty("images_scale")
        @Nullable
        Double imagesScale,

        @JsonProperty("md_page_break_placeholder")
        @Nullable
        String mdPageBreakPlaceholder,

        @JsonProperty("do_code_enrichment")
        @Nullable
        Boolean doCodeEnrichment,

        @JsonProperty("do_formula_enrichment")
        @Nullable
        Boolean doFormulaEnrichment,

        @JsonProperty("do_picture_classification")
        @Nullable
        Boolean doPictureClassification,

        @JsonProperty("do_picture_description")
        @Nullable
        Boolean doPictureDescription,

        @JsonProperty("picture_description_area_threshold")
        @Nullable
        Double pictureDescriptionAreaThreshold,

        @JsonProperty("picture_description_local")
        @Nullable
        PictureDescriptionLocal pictureDescriptionLocal,

        @JsonProperty("picture_description_api")
        @Nullable
        PictureDescriptionApi pictureDescriptionApi

) {

    public ConvertDocumentOptions {
        Assert.isTrue(pictureDescriptionLocal == null || pictureDescriptionApi == null, "picture_description_local and picture_description_api cannot both be set");
    }

    private ConvertDocumentOptions(Builder builder) {
        this(
                builder.fromFormats,
                builder.toFormats,
                builder.imageExportMode,
                builder.doOcr,
                builder.forceOcr,
                builder.ocrEngine,
                builder.ocrLang,
                builder.pdfBackend,
                builder.tableMode,
                builder.pipeline,
                builder.pageRange,
                builder.documentTimeout,
                builder.abortOnError,
                builder.doTableStructure,
                builder.includeImages,
                builder.imagesScale,
                builder.mdPageBreakPlaceholder,
                builder.doCodeEnrichment,
                builder.doFormulaEnrichment,
                builder.doPictureClassification,
                builder.doPictureDescription,
                builder.pictureDescriptionAreaThreshold,
                builder.pictureDescriptionLocal,
                builder.pictureDescriptionApi
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        @Nullable private List<InputFormat> fromFormats;
        @Nullable private List<OutputFormat> toFormats;
        @Nullable private ImageRefMode imageExportMode;
        @Nullable private Boolean doOcr;
        @Nullable private Boolean forceOcr;
        @Nullable private OcrEngine ocrEngine;
        @Nullable private List<String> ocrLang;
        @Nullable private PdfBackend pdfBackend;
        @Nullable private TableFormerMode tableMode;
        @Nullable private ProcessingPipeline pipeline;
        @Nullable private Integer @Nullable [] pageRange;
        @Nullable private Duration documentTimeout;
        @Nullable private Boolean abortOnError;
        @Nullable private Boolean doTableStructure;
        @Nullable private Boolean includeImages;
        @Nullable private Double imagesScale;
        @Nullable private String mdPageBreakPlaceholder;
        @Nullable private Boolean doCodeEnrichment;
        @Nullable private Boolean doFormulaEnrichment;
        @Nullable private Boolean doPictureClassification;
        @Nullable private Boolean doPictureDescription;
        @Nullable private Double pictureDescriptionAreaThreshold;
        @Nullable private PictureDescriptionLocal pictureDescriptionLocal;
        @Nullable private PictureDescriptionApi pictureDescriptionApi;

        private Builder() {}

        /**
         * Input format(s) to convert from.
         */
        public Builder fromFormats(@Nullable List<InputFormat> fromFormats) {
            this.fromFormats = new ArrayList<>(fromFormats);
            return this;
        }

        /**
         * Output format(s) to convert to.
         */
        public Builder toFormats(@Nullable List<OutputFormat> toFormats) {
            this.toFormats = new ArrayList<>(toFormats);
            return this;
        }

        /**
         * Image export mode for the document (in case of JSON, Markdown or HTML).
         */
        public Builder imageExportMode(@Nullable ImageRefMode imageExportMode) {
            this.imageExportMode = imageExportMode;
            return this;
        }

        /**
         * If enabled, the bitmap content will be processed using OCR.
         */
        public Builder doOcr(@Nullable Boolean doOcr) {
            this.doOcr = doOcr;
            return this;
        }

        /**
         * If enabled, replace existing text with OCR-generated text over content.
         */
        public Builder forceOcr(@Nullable Boolean forceOcr) {
            this.forceOcr = forceOcr;
            return this;
        }

        /**
         * The OCR engine to use.
         */
        public Builder ocrEngine(@Nullable OcrEngine ocrEngine) {
            this.ocrEngine = ocrEngine;
            return this;
        }

        /**
         * List of languages used by the OCR engine. Note that each OCR engine has different values for the language names.
         */
        public Builder ocrLang(@Nullable List<String> ocrLang) {
            this.ocrLang = new ArrayList<>(ocrLang);
            return this;
        }

        /**
         * The PDF backend to use.
         */
        public Builder pdfBackend(@Nullable PdfBackend pdfBackend) {
            this.pdfBackend = pdfBackend;
            return this;
        }

        /**
         * Mode to use for table structure.
         */
        public Builder tableMode(@Nullable TableFormerMode tableMode) {
            this.tableMode = tableMode;
            return this;
        }

        /**
         * Choose the pipeline to process PDF or image files.
         */
        public Builder pipeline(@Nullable ProcessingPipeline pipeline) {
            this.pipeline = pipeline;
            return this;
        }

        /**
         * Only convert a range of pages. The page number starts at 1.
         */
        public Builder pageRange(@Nullable Integer fromPage, @Nullable Integer toPage) {
            Assert.isTrue((fromPage != null && toPage != null) || (fromPage == null && toPage == null), "fromPage and toPage must both be null or both not null");
            if (fromPage != null) {
                this.pageRange = new Integer[]{fromPage, toPage};
            } else {
                this.pageRange = null;
            }
            return this;
        }

        /**
         * The timeout for processing each document.
         */
        public Builder documentTimeout(@Nullable Duration documentTimeout) {
            this.documentTimeout = documentTimeout;
            return this;
        }

        /**
         * Abort on error if enabled.
         */
        public Builder abortOnError(@Nullable Boolean abortOnError) {
            this.abortOnError = abortOnError;
            return this;
        }

        /**
         * If enabled, the table structure will be extracted.
         */
        public Builder doTableStructure(@Nullable Boolean doTableStructure) {
            this.doTableStructure = doTableStructure;
            return this;
        }

        /**
         * If enabled, images will be extracted from the document.
         */
        public Builder includeImages(@Nullable Boolean includeImages) {
            this.includeImages = includeImages;
            return this;
        }

        /**
         * Scale factor for images.
         */
        public Builder imagesScale(@Nullable Double imagesScale) {
            this.imagesScale = imagesScale;
            return this;
        }

        /**
         * Add this placeholder betweek pages in the markdown output.
         */
        public Builder mdPageBreakPlaceholder(@Nullable String mdPageBreakPlaceholder) {
            this.mdPageBreakPlaceholder = mdPageBreakPlaceholder;
            return this;
        }

        /**
         * If enabled, perform OCR code enrichment.
         */
        public Builder doCodeEnrichment(@Nullable Boolean doCodeEnrichment) {
            this.doCodeEnrichment = doCodeEnrichment;
            return this;
        }

        /**
         * If enabled, perform formula OCR, return LaTeX code.
         */
        public Builder doFormulaEnrichment(@Nullable Boolean doFormulaEnrichment) {
            this.doFormulaEnrichment = doFormulaEnrichment;
            return this;
        }

        /**
         * If enabled, classify pictures in documents.
         */
        public Builder doPictureClassification(@Nullable Boolean doPictureClassification) {
            this.doPictureClassification = doPictureClassification;
            return this;
        }

        /**
         * If enabled, describe pictures in documents.
         */
        public Builder doPictureDescription(@Nullable Boolean doPictureDescription) {
            this.doPictureDescription = doPictureDescription;
            return this;
        }

        /**
         * Minimum percentage of the area for a picture to be processed with the models.
         */
        public Builder pictureDescriptionAreaThreshold(@Nullable Double pictureDescriptionAreaThreshold) {
            this.pictureDescriptionAreaThreshold = pictureDescriptionAreaThreshold;
            return this;
        }

        /**
         * Options for running a local vision-language model in the picture description. The parameters refer to a model hosted on Hugging Face.
         * This parameter is mutually exclusive with picture_description_api.
         */
        public Builder pictureDescriptionLocal(@Nullable PictureDescriptionLocal pictureDescriptionLocal) {
            this.pictureDescriptionLocal = pictureDescriptionLocal;
            return this;
        }

        /**
         * API details for using a vision-language model in the picture description.
         * This parameter is mutually exclusive with picture_description_local.
         */
        public Builder pictureDescriptionApi(@Nullable PictureDescriptionApi pictureDescriptionApi) {
            this.pictureDescriptionApi = pictureDescriptionApi;
            return this;
        }

        public ConvertDocumentOptions build() {
            return new ConvertDocumentOptions(this);
        }

    }

}
