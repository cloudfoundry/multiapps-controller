package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.common.util.MapUtil;

public interface ElementUpdater {

    <T> List<T> updateList(List<T> oldList, List<T> newList);

    <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap);

    class AttributeReplacer implements ElementUpdater {

        @Override
        public <T> List<T> updateList(List<T> oldList, List<T> newList) {
            return newList;
        }

        @Override
        public <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap) {
            return newMap;
        }

    }

    class AttributeMerger implements ElementUpdater {

        @Override
        public <T> List<T> updateList(List<T> oldList, List<T> newList) {
            return ListUtils.union(oldList, newList);
        }

        @Override
        public <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap) {
            return MapUtil.merge(oldMap, newMap);
        }

    }

    static ElementUpdater getUpdater(UpdateStrategy updateStrategy) {
        if (updateStrategy == UpdateStrategy.REPLACE) {
            return new AttributeReplacer();
        }
        return new AttributeMerger();
    }

    enum UpdateStrategy {
        REPLACE, MERGE
    }

}
