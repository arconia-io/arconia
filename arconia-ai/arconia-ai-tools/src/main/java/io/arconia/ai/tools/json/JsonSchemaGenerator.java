package io.arconia.ai.tools.json;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

/**
 * Utilities to generate JSON Schemas from Java entities.
 */
public class JsonSchemaGenerator {

    private static final AtomicReference<SchemaGenerator> SCHEMA_GENERATOR = new AtomicReference<>();

    public static String generate(Method method) {
        var generator = buildSchemaGenerator();

        ObjectNode schema = JsonParser.getObjectMapper().createObjectNode();
        schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier()); // Option.SCHEMA_VERSION_INDICATOR
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        for (int i = 0; i < method.getParameterCount(); i++) {
            var parameterName = method.getParameters()[i].getName();
            var parameterType = method.getGenericParameterTypes()[i];
            properties.set(parameterName, generator.generateSchema(parameterType));
        }

        schema.put("additionalProperties", false); // Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT

        var requiredArray = schema.putArray("required");
        Stream.of(method.getParameters()).map(Parameter::getName).forEach(requiredArray::add);

        return schema.toPrettyString();
    }

    // Based on the implementation in ModelOptionsUtils.
    private static SchemaGenerator buildSchemaGenerator() {
        if (SCHEMA_GENERATOR.get() != null) {
            return SCHEMA_GENERATOR.get();
        }

        JacksonModule jacksonModule = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED);
        Swagger2Module swaggerModule = new Swagger2Module();

        SchemaGeneratorConfig schemaGeneratorConfig = new SchemaGeneratorConfigBuilder(JsonParser.getObjectMapper(),
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
            .with(jacksonModule)
            .with(swaggerModule)
            // .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
            .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
            .with(Option.PLAIN_DEFINITION_KEYS)
            .without(Option.SCHEMA_VERSION_INDICATOR)
            .build();

        SchemaGenerator generator = new SchemaGenerator(schemaGeneratorConfig);

        SCHEMA_GENERATOR.set(generator);

        return generator;
    }

}
