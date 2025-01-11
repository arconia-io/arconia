package io.arconia.ai.tools.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.springframework.ai.util.JacksonUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utilities to perform parsing operations between JSON and Java.
 */
public class JsonParser {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .addModules(JacksonUtils.instantiateAvailableModules())
        .build();

    /**
     * Returns a Jackson {@link ObjectMapper} instance tailored for
     * JSON-parsing operations for tool calling and structured output.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Converts a JSON string to a Java object.
     */
    public static <T> T fromJson(String json, Class<T> type) {
        Assert.notNull(json, "json cannot be null");
        Assert.notNull(type, "type cannot be null");

        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getName()), ex);
        }
    }

    /**
     * Converts a JSON string to a Java object.
     */
    public static <T> T fromJson(String json, TypeReference<T> type) {
        Assert.notNull(json, "json cannot be null");
        Assert.notNull(type, "type cannot be null");

        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Conversion from JSON to %s failed".formatted(type.getType().getTypeName()), ex);
        }
    }

    /**
     * Converts a Java object to a JSON string.
     */
    public static String toJson(@Nullable Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Conversion from Object to JSON failed", ex);
        }
    }

    /**
     * Convert a Java Object to a typed Object.
     * Based on the implementation in MethodInvokingFunctionCallback.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Object toTypedObject(Object value, Class<?> type) {
        Assert.notNull(value, "value cannot be null");
        Assert.notNull(type, "type cannot be null");

        var javaType = ClassUtils.resolvePrimitiveIfNecessary(type);

        if (javaType == String.class) {
            return value.toString();
        } else if (javaType == Byte.class) {
            return Byte.parseByte(value.toString());
        } else if (javaType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (javaType == Short.class) {
            return Short.parseShort(value.toString());
        } else if (javaType == Long.class) {
            return Long.parseLong(value.toString());
        } else if (javaType == Double.class) {
            return Double.parseDouble(value.toString());
        } else if (javaType == Float.class) {
            return Float.parseFloat(value.toString());
        } else if (javaType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (javaType.isEnum()) {
            return Enum.valueOf((Class<Enum>) javaType, value.toString());
        }

        String json = JsonParser.toJson(value);
        return JsonParser.fromJson(json, javaType);
    }

}
