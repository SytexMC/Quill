package me.levitate.quill.storage;

import lombok.Getter;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public abstract class AbstractStorageProvider<K, V> implements StorageProvider<K, V> {
    protected final Plugin plugin;
    protected final Map<K, V> cache;
    protected final Class<K> keyClass;
    protected final Class<V> valueClass;
    protected volatile boolean connected;

    @Getter
    protected final SerializationProvider serializationProvider;

    protected AbstractStorageProvider(Plugin plugin, Class<K> keyClass, Class<V> valueClass,
                                      SerializationProvider serializationProvider) {
        this.plugin = plugin;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.cache = new ConcurrentHashMap<>();
        this.serializationProvider = serializationProvider;
        this.connected = false;
    }

    @Override
    public V getOrDefault(K key, V defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return cache.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return cache.containsKey(key);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(cache.entrySet());
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    protected void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("Storage provider is not connected");
        }
    }

    protected void logError(String message, Exception e) {
        plugin.getLogger().log(Level.SEVERE, message, e);
    }
}