package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.List;

public interface CloudModelBuilderContentCalculator<T> {

    List<T> calculateContentForBuilding(List<? extends T> elements);

}
