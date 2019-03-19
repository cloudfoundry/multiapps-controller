package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;

import com.sap.cloud.lm.sl.common.util.MapUtil;

public interface ElementUpdater {

    public <T> List<T> updateList(List<T> oldList, List<T> newList);

    public <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap);

    public class AttributeReplacer implements ElementUpdater {

        @Override
        public <T> List<T> updateList(List<T> oldList, List<T> newList) {
            return newList;
        }

        @Override
        public <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap) {
            return newMap;
        }

    }

    public class AttributeMerger implements ElementUpdater {

        @Override
        public <T> List<T> updateList(List<T> oldList, List<T> newList) {
            return ListUtils.union(oldList, newList);
        }

        @Override
        public <K, V> Map<K, V> updateMap(Map<K, V> oldMap, Map<K, V> newMap) {
            return MapUtil.merge(oldMap, newMap);
        }

    }

    public static ElementUpdater getUpdater(UpdateBehavior updateBehavior) {
        if (updateBehavior == UpdateBehavior.REPLACE) {
            return new AttributeReplacer();
        }

        return new AttributeMerger();
    }

    public enum UpdateBehavior {
        REPLACE, MERGE
    }
}
