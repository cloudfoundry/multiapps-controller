package org.cloudfoundry.multiapps.controller.database.migration.executor.type;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class BytesDatabaseTypeSetterTest {

    private static final int COLUMN_INDEX = 1;

    @Mock
    private PreparedStatement preparedStatement;

    private BytesDatabaseTypeSetter setter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        setter = new BytesDatabaseTypeSetter();
    }

    @Test
    void testGetSupportedTypesContainsBytea() {
        List<String> supportedTypes = setter.getSupportedTypes();

        Assertions.assertEquals(List.of("bytea"), supportedTypes);
    }

    @Test
    void testSetTypeDelegatesToSetBytes() throws SQLException {
        byte[] value = "descriptor-payload".getBytes(StandardCharsets.UTF_8);

        setter.setType(COLUMN_INDEX, preparedStatement, value);

        Mockito.verify(preparedStatement)
               .setBytes(COLUMN_INDEX, value);
    }

    @Test
    void testSetTypeWithNullValue() throws SQLException {
        setter.setType(COLUMN_INDEX, preparedStatement, null);

        Mockito.verify(preparedStatement)
               .setBytes(COLUMN_INDEX, null);
    }

    @Test
    void testSetTypeWithEmptyByteArray() throws SQLException {
        byte[] value = new byte[0];

        setter.setType(COLUMN_INDEX, preparedStatement, value);

        Mockito.verify(preparedStatement)
               .setBytes(COLUMN_INDEX, value);
    }

    @Test
    void testSetTypeWithNonByteArrayValueThrowsClassCastException() {
        Assertions.assertThrows(ClassCastException.class,
                                () -> setter.setType(COLUMN_INDEX, preparedStatement, "not-a-byte-array"));
    }

}
