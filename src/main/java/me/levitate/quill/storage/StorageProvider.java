package me.levitate.quill.storage;

import me.levitate.quill.storage.serializers.SerializationProvider;
import java.util.*;
import java.util.function.Consumer;

public interface StorageProvider<K, V> extends AutoCloseable {
    void connect() throws Exception;
    void disconnect() throws Exception;
    void save() throws Exception;
    void load() throws Exception;
    Optional<V> get(K key);
    V getOrDefault(K key, V defaultValue);
    void put(K key, V value);
    void update(K key, Consumer<V> updater);
    boolean remove(K key);
    void batchUpdate(Collection<K> keys, Consumer<V> updater);
    boolean containsKey(K key);
    int size();
    boolean isEmpty();
    Set<K> keySet();
    Collection<V> values();
    Set<Map.Entry<K, V>> entrySet();
    SerializationProvider getSerializationProvider();
    boolean isConnected();

    @Override
    default void close() throws Exception {
        disconnect();
    }
}