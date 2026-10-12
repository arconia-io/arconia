package io.arconia.docling.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import ai.docling.serve.api.validation.ValidationError;
import ai.docling.serve.api.validation.ValidationErrorDetail;
import ai.docling.serve.api.validation.ValidationException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a 422 response from the Docling Serve API into a {@link ValidationException} carrying
 * the parsed {@link ValidationError}. A 422 without validation details, for example, from a gateway,
 * is reported as the usual {@link HttpClientErrorException} with its body preserved.
 */
final class ValidationErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ValidationErrorHandler.class);

    static final HttpStatusCode STATUS = HttpStatus.UNPROCESSABLE_CONTENT;

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addMixIn(ValidationErrorDetail.Builder.class, ValidationErrorDetailBuilderMixin.class)
            .build();

    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        byte[] body = StreamUtils.copyToByteArray(response.getBody());
        Charset charset = charset(response);

        Optional<ValidationError> validationError = parse(new String(body, charset));
        if (validationError.isPresent()) {
            throw new ValidationException(validationError.get(), message(request, validationError.get()));
        }
        throw HttpClientErrorException.create(response.getStatusCode(), response.getStatusText(), response.getHeaders(), body, charset);
    }

    /**
     * Any JSON object parses into a ValidationError, so only one with details counts as a validation error.
     */
    private Optional<ValidationError> parse(String body) {
        try {
            return Optional.of(jsonMapper.readValue(body, ValidationError.class))
                    .filter(error -> !error.getErrorDetails().isEmpty());
        } catch (JacksonException ex) {
            logger.debug("The 422 response body is not a validation error", ex);
            return Optional.empty();
        }
    }

    private static String message(HttpRequest request, ValidationError validationError) {
        return "The Docling Serve API rejected the %s request to %s:%n%s".formatted(request.getMethod(), request.getURI(),
                validationError.getErrorDetails().stream()
                        .map(ValidationErrorDetail::getMessage)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(System.lineSeparator())));
    }

    private static Charset charset(ClientHttpResponse response) {
        MediaType contentType = response.getHeaders().getContentType();
        Charset charset = contentType != null ? contentType.getCharset() : null;
        return charset != null ? charset : StandardCharsets.UTF_8;
    }

    /**
     * Docling Serve echoes the rejected input in each detail. For a file source, that is the whole
     * document, which has no place in an exception.
     */
    @JsonIgnoreProperties("input")
    interface ValidationErrorDetailBuilderMixin {
    }

}
