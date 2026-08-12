package io.arconia.multitenancy.tenantdetails.jdbc;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class JdbcHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        for (var sql : new String[]{"io/arconia/multitenancy/data/autoconfigure/schema-postgresql.sql",
                "io/arconia/multitenancy/data/autoconfigure/schema-drop-postgresql.sql"})
            hints.resources().registerPattern(sql);
    }
}
