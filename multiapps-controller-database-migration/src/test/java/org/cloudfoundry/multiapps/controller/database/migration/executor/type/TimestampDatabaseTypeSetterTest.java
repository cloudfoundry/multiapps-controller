package org.cloudfoundry.multiapps.controller.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class TimestampDatabaseTypeSetterTest {

    private static final int COLUMN_INDEX = 1;

    @Mock
    private PreparedStatement preparedStatement;

    private TimestampDatabaseTypeSetter setter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        setter = new TimestampDatabaseTypeSetter();
    }

    @Test
    void testGetSupportedTypesContainsTimestamp() {
        List<String> supportedTypes = setter.getSupportedTypes();

        Assertions.assertEquals(List.of("timestamp"), supportedTypes);
    }

    @Test
    void testSetTypeDelegatesToSetTimestamp() throws SQLException {
        Timestamp value = Timestamp.valueOf("2026-06-26 13:16:22.060");

        setter.setType(COLUMN_INDEX, preparedStatement, value);

        Mockito.verify(preparedStatement)
               .setTimestamp(COLUMN_INDEX, value);
    }

    @Test
    void testSetTypeWithNullValue() throws SQLException {
        setter.setType(COLUMN_INDEX, preparedStatement, null);

        Mockito.verify(preparedStatement)
               .setTimestamp(COLUMN_INDEX, null);
    }

    @Test
    void testSetTypeWithNonTimestampValueThrowsClassCastException() {
        Assertions.assertThrows(ClassCastException.class,
                                () -> setter.setType(COLUMN_INDEX, preparedStatement, "2026-06-26"));
    }

}
