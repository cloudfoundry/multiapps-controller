package org.cloudfoundry.multiapps.controller.database.migration.executor.type;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseTypeSetterFactory {

    private static final List<DatabaseTypeSetter> DEFAULT_REGISTERED_TYPE_SETTERS = Arrays.asList(new StringDatabaseTypeSetter(),
                                                                                                  new BooleanDatabaseTypeSetter(),
                                                                                                  new LongDatabaseTypeSetter());
    private final List<DatabaseTypeSetter> registeredTypeSetters;

    public DatabaseTypeSetterFactory() {
        this(DEFAULT_REGISTERED_TYPE_SETTERS);
    }

    public DatabaseTypeSetterFactory(List<DatabaseTypeSetter> registeredTypeSetters) {
        this.registeredTypeSetters = registeredTypeSetters;
    }

    public DatabaseTypeSetter get(String databaseType) {
        List<DatabaseTypeSetter> matchingTypeSetters = registeredTypeSetters.stream()
                                                                            .filter(typeSetter -> containsIgnoreCase(databaseType,
                                                                                                                     typeSetter))
                                                                            .collect(Collectors.toList());
        if (matchingTypeSetters.isEmpty()) {
            throw new IllegalStateException(MessageFormat.format("No database type setter is defined for type \"{0}\"", databaseType));
        }

        if (matchingTypeSetters.size() > 1) {
            throw new IllegalStateException(MessageFormat.format("More than one database type setters are defined for type \"{0}\"",
                                                                 databaseType));
        }

        return matchingTypeSetters.get(0);
    }

    private boolean containsIgnoreCase(String databaseType, DatabaseTypeSetter typeSetter) {
        return typeSetter.getSupportedTypes()
                         .stream()
                         .anyMatch(type -> type.equalsIgnoreCase(databaseType));
    }

}
