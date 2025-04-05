package com.sevtinge.hyperceiler.module.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiValueMap<K, V> {
    private Map<K, List<V>> map = new HashMap<>();

    public void put(K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public List<V> get(K key) {
        return map.getOrDefault(key, new ArrayList<>());
    }

    public List<K> getKeys() {
        return new ArrayList<>(map.keySet());
    }

    public Map<K, List<V>> getMap() {
        return map;
    }
}
