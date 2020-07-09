package com.sap.cloud.lm.sl.cf.database.migration.executor.type;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DatabaseTypeSetterFactoryTest {
    private static final String EXISTING_DATABASE_TYPE_SETTER = "bool";

    private static final String NON_EXISTING_DATABASE_TYPE_SETTER = "test";

    private DatabaseTypeSetterFactory databaseTypeSetterFactory;

    @Test
    public void testGetWithNullStringParameter() {
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseTypeSetterFactory.get(null);
        });
    }

    @Test
    public void testGetWithEmptyStringParameter() {
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseTypeSetterFactory.get("");
        });
    }

    @Test
    public void testGetWithNullCustomRegisteredTypeSetters() {
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory(null);

        DatabaseTypeSetter resultDatabaseTypeSetter = databaseTypeSetterFactory.get(EXISTING_DATABASE_TYPE_SETTER);

        Assertions.assertTrue(resultDatabaseTypeSetter.getSupportedTypes()
                                                      .contains(EXISTING_DATABASE_TYPE_SETTER));
    }

    @Test
    public void testGetWithEmptyCustomRegisteredTypeSetters() {
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory(new ArrayList<DatabaseTypeSetter>());

        DatabaseTypeSetter resultDatabaseTypeSetter = databaseTypeSetterFactory.get(EXISTING_DATABASE_TYPE_SETTER);

        Assertions.assertTrue(resultDatabaseTypeSetter.getSupportedTypes()
                                                      .contains(EXISTING_DATABASE_TYPE_SETTER));
    }

    @Test
    public void testGetWithCustomRegisteredTypeSettersWhenSingleMatchingTypeSetter() {
        List<DatabaseTypeSetter> registeredDatabaseTypeSetters = buildSingleMatchingregisteredDatabaseTypeSettersCollection();
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory(registeredDatabaseTypeSetters);

        DatabaseTypeSetter resultDatabaseTypeSetter = databaseTypeSetterFactory.get(EXISTING_DATABASE_TYPE_SETTER);

        Assertions.assertEquals(registeredDatabaseTypeSetters.get(0), resultDatabaseTypeSetter);
    }

    @Test
    public void testGetWithCustomRegisteredTypeSettersWhenNoMatchingTypeSetter() {
        List<DatabaseTypeSetter> registeredDatabaseTypeSetters = buildSingleMatchingregisteredDatabaseTypeSettersCollection();
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory(registeredDatabaseTypeSetters);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseTypeSetterFactory.get(NON_EXISTING_DATABASE_TYPE_SETTER);
        });
    }

    @Test
    public void testGetWithCustomRegisteredTypeSettersWhenMultipleMatchingTypeSetters() {
        List<DatabaseTypeSetter> registeredDatabaseTypeSetters = buildDoubleMatchingregisteredDatabaseTypeSettersCollection();
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory(registeredDatabaseTypeSetters);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseTypeSetterFactory.get(EXISTING_DATABASE_TYPE_SETTER);
        });
    }

    @Test
    public void testGetWithDefaultRegisteredTypeSettersWhenMatchingTypeSetter() {
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        DatabaseTypeSetter resultDatabaseTypeSetter = databaseTypeSetterFactory.get(EXISTING_DATABASE_TYPE_SETTER);

        Assertions.assertTrue(resultDatabaseTypeSetter.getSupportedTypes()
                                                      .contains(EXISTING_DATABASE_TYPE_SETTER));
    }

    @Test
    public void testGetWithDefaultRegisteredTypeSettersWhenNoMatchingTypeSetter() {
        databaseTypeSetterFactory = new DatabaseTypeSetterFactory();

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseTypeSetterFactory.get(NON_EXISTING_DATABASE_TYPE_SETTER);
        });
    }

    private List<DatabaseTypeSetter> buildSingleMatchingregisteredDatabaseTypeSettersCollection() {
        List<DatabaseTypeSetter> databaseTypeSetters = new ArrayList<>();
        databaseTypeSetters.add(new BooleanDatabaseTypeSetter());
        return databaseTypeSetters;
    }

    private List<DatabaseTypeSetter> buildDoubleMatchingregisteredDatabaseTypeSettersCollection() {
        List<DatabaseTypeSetter> databaseTypeSetters = buildSingleMatchingregisteredDatabaseTypeSettersCollection();
        databaseTypeSetters.add(new BooleanDatabaseTypeSetter());
        return databaseTypeSetters;
    }
}
