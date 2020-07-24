package org.cloudfoundry.multiapps.controller.core.cf.util;

import java.util.List;

public interface CloudModelBuilderContentCalculator<T> {

    List<T> calculateContentForBuilding(List<? extends T> elements);

}
