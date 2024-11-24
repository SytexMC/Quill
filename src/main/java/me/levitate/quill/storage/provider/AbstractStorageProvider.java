package me.levitate.quill.storage.provider;

import lombok.Getter;
import me.levitate.quill.cache.Cache;
import me.levitate.quill.cache.local.LocalCache;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractStorageProvider<K, V> implements StorageProvider<K, V> {
    protected final Plugin plugin;
    protected final Class<K> keyClass;
    protected final Class<V> valueClass;

    @Getter
    protected final Cache<K, V> cache;

    @Getter
    protected final SerializationProvider serializationProvider;

    protected volatile boolean connected;

    protected AbstractStorageProvider(Plugin plugin, Class<K> keyClass, Class<V> valueClass,
                                      SerializationProvider serializationProvider) {
        this.plugin = plugin;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.serializationProvider = serializationProvider;
        this.connected = false;
        this.cache = new LocalCache<>();
    }

    @Override
    public V getOrDefault(K key, V defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return cache.get(key).orElse(defaultValue);
    }

    @Override
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return cache.containsKey(key);
    }

    @Override
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return cache.get(key);
    }

    @Override
    public void put(K key, V value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        cache.put(key, value);
    }

    @Override
    public void update(K key, Consumer<V> updater) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");
        cache.get(key).ifPresent(value -> {
            updater.accept(value);
            cache.put(key, value);
        });
    }

    @Override
    public void batchUpdate(Collection<K> keys, Consumer<V> updater) {
        Objects.requireNonNull(keys, "Keys collection cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");
        keys.forEach(key -> update(key, updater));
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.size() == 0;
    }

    @Override
    public Set<K> keySet() {
        return cache.keys();
    }

    @Override
    public Collection<V> values() {
        return cache.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Map<K, V> map = new HashMap<>();
        for (K key : cache.keys()) {
            cache.get(key).ifPresent(value -> map.put(key, value));
        }
        return Collections.unmodifiableSet(map.entrySet());
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() {
        cache.close();
        connected = false;
    }
}