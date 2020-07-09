package com.sap.cloud.lm.sl.cf.database.migration.executor.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DatabaseTypeSetterFactoryTest {

    private static final String BOOL_TYPE = "bool";
    private static final String LONG_TYPE = "int8";
    private static final String STRING_TYPE = "varchar";

    @Test
    public void testGetWithNullStringParameter() {
        DatabaseTypeSetterFactory databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        Assertions.assertThrows(IllegalStateException.class, () -> databaseTypeSetterFactory.get(null));
    }

    @Test
    public void testGetWithEmptyStringParameter() {
        DatabaseTypeSetterFactory databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        Assertions.assertThrows(IllegalStateException.class, () -> databaseTypeSetterFactory.get(""));
    }

    @Test
    public void testGetWithCustomRegisteredTypeSettersAndSingleMatch() {
        List<DatabaseTypeSetter> registeredDatabaseTypeSetters = Arrays.asList(new BooleanDatabaseTypeSetter());
        DatabaseTypeSetterFactory databaseTypeSetterFactory = new DatabaseTypeSetterFactory(registeredDatabaseTypeSetters);

        DatabaseTypeSetter resultDatabaseTypeSetter = databaseTypeSetterFactory.get(BOOL_TYPE);

        Assertions.assertEquals(registeredDatabaseTypeSetters.get(0), resultDatabaseTypeSetter);
    }

    @Test
    public void testGetWithCustomRegisteredTypeSettersWhenNoMatchingTypeSetter() {
        List<DatabaseTypeSetter> registeredDatabaseTypeSetters = Collections.emptyList();
        DatabaseTypeSetterFactory databaseTypeSetterFactory = new DatabaseTypeSetterFactory(registeredDatabaseTypeSetters);

        Assertions.assertThrows(IllegalStateException.class, () -> databaseTypeSetterFactory.get(BOOL_TYPE));
    }

    @Test
    public void testGetWithCustomRegisteredTypeSettersWhenMultipleMatchingTypeSetters() {
        List<DatabaseTypeSetter> registeredDatabaseTypeSetters = Arrays.asList(new BooleanDatabaseTypeSetter(),
                                                                               new BooleanDatabaseTypeSetter());
        DatabaseTypeSetterFactory databaseTypeSetterFactory = new DatabaseTypeSetterFactory(registeredDatabaseTypeSetters);

        Assertions.assertThrows(IllegalStateException.class, () -> databaseTypeSetterFactory.get(BOOL_TYPE));
    }

    @Test
    public void testGetWithDefaultRegisteredTypeSettersWhenMatchingDefaultTypeSetters() {
        DatabaseTypeSetterFactory databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        DatabaseTypeSetter resultDatabaseTypeSetter = databaseTypeSetterFactory.get(BOOL_TYPE);
        Assertions.assertTrue(resultDatabaseTypeSetter.getSupportedTypes()
                                                      .contains(BOOL_TYPE));

        resultDatabaseTypeSetter = databaseTypeSetterFactory.get(LONG_TYPE);
        Assertions.assertTrue(resultDatabaseTypeSetter.getSupportedTypes()
                                                      .contains(LONG_TYPE));

        resultDatabaseTypeSetter = databaseTypeSetterFactory.get(STRING_TYPE);
        Assertions.assertTrue(resultDatabaseTypeSetter.getSupportedTypes()
                                                      .contains(STRING_TYPE));
    }

}
