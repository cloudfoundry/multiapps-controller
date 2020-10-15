package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.common.util.MapUtil;

public interface ElementUpdater {

    <T> List<T> updateList(List<T> oldList, List<T> newList);

    <T> Set<T> updateSet(Set<T> oldSet, Set<T> newSet);

    <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap);

    class AttributeReplacer implements ElementUpdater {

        @Override
        public <T> List<T> updateList(List<T> oldList, List<T> newList) {
            return newList;
        }

        @Override
        public <T> Set<T> updateSet(Set<T> oldSet, Set<T> newSet) {
            return newSet;
        }

        @Override
        public <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap) {
            return newMap;
        }

    }

    class AttributeMerger implements ElementUpdater {

        @Override
        public <T> List<T> updateList(List<T> oldList, List<T> newList) {
            List<T> newPartsOnly = ListUtils.subtract(newList, oldList);
            return ListUtils.union(oldList, newPartsOnly);
        }

        @Override
        public <T> Set<T> updateSet(Set<T> oldSet, Set<T> newSet) {
            return SetUtils.union(oldSet, newSet)
                           .toSet();
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
