package me.levitate.quill.storage.provider;

import me.levitate.quill.storage.serializers.SerializationProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface StorageProvider<K, V> extends AutoCloseable {
    void connect() throws Exception;

    void disconnect() throws Exception;

    void save() throws Exception;

    void saveKey(K key) throws Exception;

    void load() throws Exception;

    void loadKey(K key) throws Exception;

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

    CompletableFuture<Void> loadKeyAsync(K key);

    CompletableFuture<Void> saveKeyAsync(K key);

    CompletableFuture<Void> saveAsync();

    CompletableFuture<Void> loadAsync();

    @Override
    default void close() throws Exception {
        disconnect();
    }
}