package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class SchemaVersionUtils {

    private static final int MAJOR_SCHEMA_VERSION_THREE = 3;

    public static <T> List<?> getEntityData(T entity, Function<T, Integer> versionGetter, Function<T, List<?>> dataGetter) {
        return versionGetter.apply(entity) < MAJOR_SCHEMA_VERSION_THREE ? Collections.emptyList() : dataGetter.apply(entity);
    }
}
