package me.levitate.quill.storage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import me.levitate.quill.serializers.JSONSerializers;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class JSONStorage<K, V> {
    @Getter
    private final Map<K, V> storage;
    private final ObjectMapper mapper;
    private final SimpleModule module;
    private final Plugin plugin;

    private String fileName;
    private Class<K> keyClass;
    private Class<V> valueClass;
    private File file;

    public JSONStorage(Plugin plugin) {
        this.plugin = plugin;
        this.storage = new ConcurrentHashMap<>();
        this.mapper = new ObjectMapper();
        this.module = new SimpleModule();

        // Configure ObjectMapper
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.registerModule(module);

        JSONSerializers.registerDefaults(this);
    }

    public JSONStorage<K, V> fileName(String fileName) {
        this.fileName = fileName + (fileName.endsWith(".json") ? "" : ".json");
        return this;
    }

    public JSONStorage<K, V> keyClass(Class<K> keyClass) {
        this.keyClass = keyClass;
        return this;
    }

    public JSONStorage<K, V> valueClass(Class<V> valueClass) {
        this.valueClass = valueClass;
        return this;
    }

    public <T> JSONStorage<K, V> addConverter(Class<T> type, JsonSerializer<T> serializer, JsonDeserializer<T> deserializer) {
        module.addSerializer(type, serializer);
        module.addDeserializer(type, deserializer);
        mapper.registerModule(module);
        return this;
    }

    public <T> JSONStorage<K, V> addStringConverter(Class<T> type, Function<T, String> toString, Function<String, T> fromString) {
        addConverter(type,
                new JsonSerializer<T>() {
                    @Override
                    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                        gen.writeString(toString.apply(value));
                    }
                },
                new JsonDeserializer<T>() {
                    @Override
                    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        return fromString.apply(p.getText());
                    }
                }
        );
        return this;
    }

    public JSONStorage<K, V> build() {
        if (fileName == null || keyClass == null || valueClass == null) {
            throw new IllegalStateException("Missing required parameters for storage initialization");
        }

        setupFileSystem();
        load();
        return this;
    }

    private void setupFileSystem() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create data folder: " + dataFolder.getAbsolutePath());
            return;
        }
        this.file = new File(dataFolder, fileName);
    }

    public synchronized void save() {
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            mapper.writeValue(os, storage);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data to " + file + ": " + e.getMessage());
        }
    }

    public synchronized void load() {
        if (!file.exists()) {
            return;
        }

        try (InputStream is = Files.newInputStream(file.toPath())) {
            JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            Map<K, V> loadedMap = mapper.readValue(is, mapType);

            storage.clear();
            if (loadedMap != null) {
                storage.putAll(loadedMap);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load data from " + file + ": " + e.getMessage());
        }
    }

    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(storage.get(key));
    }

    public V getOrDefault(K key, V defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return storage.getOrDefault(key, defaultValue);
    }

    public synchronized void put(K key, V value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        storage.put(key, value);
    }

    public synchronized void update(K key, Consumer<V> updater) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");

        storage.computeIfPresent(key, (k, v) -> {
            updater.accept(v);
            return v;
        });
    }

    public synchronized boolean remove(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return storage.remove(key) != null;
    }

    public synchronized void batchUpdate(Collection<K> keys, Consumer<V> updater) {
        Objects.requireNonNull(keys, "Keys collection cannot be null");
        Objects.requireNonNull(updater, "Updater cannot be null");

        keys.forEach(key -> update(key, updater));
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return storage.containsKey(key);
    }

    public int size() {
        return storage.size();
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(storage.keySet());
    }

    public Collection<V> values() {
        return Collections.unmodifiableCollection(storage.values());
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(storage.entrySet());
    }

    public synchronized void saveKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        V value = storage.get(key);
        if (value == null) return;

        File keyFile = getKeyFile(key);
        try {
            mapper.writeValue(keyFile, value);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save key data for " + key + ": " + e.getMessage());
        }
    }

    public synchronized void loadKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        File keyFile = getKeyFile(key);

        if (!keyFile.exists()) {
            return;
        }

        try {
            V value = mapper.readValue(keyFile, valueClass);
            if (value != null) {
                storage.put(key, value);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load key data for " + key + ": " + e.getMessage());
        }
    }

    public synchronized void invalidateKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        storage.remove(key);
    }

    private File getKeyFile(K key) {
        String sanitizedKey = sanitizeFileName(Objects.requireNonNull(key, "Key cannot be null").toString());
        return new File(plugin.getDataFolder(), fileName.replace(".json", "") + "_" + sanitizedKey + ".json");
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}