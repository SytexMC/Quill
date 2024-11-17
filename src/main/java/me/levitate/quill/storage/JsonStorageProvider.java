package me.levitate.quill.storage;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class JsonStorageProvider<K, V> extends AbstractStorageProvider<K, V> {
    private final ObjectMapper mapper;
    private final File file;
    private final String fileName;
    private final Object fileLock = new Object();

    public JsonStorageProvider(Plugin plugin, Class<K> keyClass, Class<V> valueClass,
                               String fileName, SerializationProvider serializationProvider) {
        super(plugin, keyClass, valueClass, serializationProvider);
        this.fileName = fileName + (fileName.endsWith(".json") ? "" : ".json");
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        serializationProvider.configureMapper(this.mapper);
        this.file = new File(plugin.getDataFolder(), this.fileName);
    }

    @Override
    public void connect() throws Exception {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new Exception("Failed to create data folder");
        }
        connected = true;
        load();
    }

    @Override
    public void disconnect() throws Exception {
        save();
        cache.clear();
        connected = false;
    }

    @Override
    public synchronized void save() throws Exception {
        ensureConnected();
        synchronized (fileLock) {
            try {
                File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
                mapper.writeValue(tempFile, cache);

                // Atomic file replacement
                if (file.exists() && !file.delete()) {
                    throw new Exception("Failed to delete existing file during save");
                }
                if (!tempFile.renameTo(file)) {
                    throw new Exception("Failed to rename temporary file during save");
                }
            } catch (Exception e) {
                logError("Failed to save data to " + file, e);
                throw e;
            }
        }
    }

    @Override
    public synchronized void load() throws Exception {
        ensureConnected();
        synchronized (fileLock) {
            if (!file.exists()) return;

            try {
                JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
                Map<K, V> loadedData = mapper.readValue(file, mapType);
                cache.clear();
                if (loadedData != null) {
                    cache.putAll(loadedData);
                }
            } catch (Exception e) {
                logError("Failed to load data from " + file, e);
                throw e;
            }
        }
    }

    @Override
    public Optional<V> get(K key) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public synchronized void put(K key, V value) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        cache.put(key, value);
    }

    @Override
    public synchronized void update(K key, Consumer<V> updater) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");

        cache.computeIfPresent(key, (k, v) -> {
            updater.accept(v);
            return v;
        });
    }

    @Override
    public synchronized boolean remove(K key) {
        ensureConnected();
        Objects.requireNonNull(key, "Key cannot be null");
        return cache.remove(key) != null;
    }

    @Override
    public synchronized void batchUpdate(Collection<K> keys, Consumer<V> updater) {
        ensureConnected();
        Objects.requireNonNull(keys, "Keys collection cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");

        for (K key : keys) {
            update(key, updater);
        }
    }
}