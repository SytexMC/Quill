package me.levitate.quill.storage;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import lombok.Getter;
import me.levitate.quill.storage.adapters.bukkit.ItemStackAdapter;
import me.levitate.quill.storage.adapters.bukkit.LocationAdapter;
import me.levitate.quill.storage.adapters.bukkit.WorldAdapter;
import me.levitate.quill.storage.adapters.common.BooleanAdapter;
import me.levitate.quill.storage.adapters.common.UUIDAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

/**
 * This is the class that handles the JSON storage.
 *
 * @param <K> Key
 * @param <V> Value
 */
@Getter
public class JSONStorage<K, V> {
    private final Map<K, V> storage;
    private final Moshi moshi;
    private final JsonAdapter<Map<K, V>> jsonAdapter;
    private final File file;

    protected JSONStorage(Builder<K, V> builder) {
        this.storage = new HashMap<>();
        this.file = new File(builder.dataFolder, builder.fileName);

        // Build Moshi instance with all registered adapters
        Moshi.Builder moshiBuilder = new Moshi.Builder()
                .add(new LocationAdapter())
                .add(new WorldAdapter())
                .add(new ItemStackAdapter())
                .add(new UUIDAdapter())
                .add(new BooleanAdapter());

        // Add custom adapters
        builder.customAdapters.forEach(adapter ->
                moshiBuilder.add(adapter.type, adapter.jsonAdapter));

        this.moshi = moshiBuilder.build();

        // Create the type adapter for the Map
        this.jsonAdapter = moshi.adapter(Types.newParameterizedType(
                Map.class,
                builder.keyClass,
                builder.valueClass
        ));

        // Create data folder if it doesn't exist
        if (!builder.dataFolder.exists() && !builder.dataFolder.mkdirs()) {
            throw new RuntimeException("Could not create data folder: " + builder.dataFolder);
        }
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonAdapter.toJson(storage));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write data to json file: " + file, e);
        }
    }

    public void load() {
        if (!file.exists()) return;

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            Map<K, V> loadedMap = jsonAdapter.fromJson(content);
            if (loadedMap != null) {
                storage.clear();
                storage.putAll(loadedMap);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data from json file: " + file, e);
        }
    }

    public void update(K key, Consumer<V> consumer) {
        storage.computeIfPresent(key, (k, v) -> {
            consumer.accept(v);
            return v;
        });
    }

    public void put(K key, V value) {
        storage.put(key, value);
    }

    public void remove(K key) {
        storage.remove(key);
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(storage.get(key));
    }

    public Collection<V> values() {
        return storage.values();
    }

    public Set<K> keys() {
        return storage.keySet();
    }

    public Map<K, V> getAll() {
        return new HashMap<>(storage);
    }

    public void clear() {
        storage.clear();
    }

    public static class Builder<K, V> {
        private final List<AdapterEntry<?>> customAdapters = new ArrayList<>();
        private File dataFolder;
        private String fileName;
        private Class<K> keyClass;
        private Class<V> valueClass;

        public Builder<K, V> dataFolder(File dataFolder) {
            this.dataFolder = dataFolder;
            return this;
        }

        public Builder<K, V> fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder<K, V> keyClass(Class<K> keyClass) {
            this.keyClass = keyClass;
            return this;
        }

        public Builder<K, V> valueClass(Class<V> valueClass) {
            this.valueClass = valueClass;
            return this;
        }

        public <T> Builder<K, V> addAdapter(Type type, JsonAdapter<T> adapter) {
            customAdapters.add(new AdapterEntry<>(type, adapter));
            return this;
        }

        public JSONStorage<K, V> build() {
            validate();
            return new JSONStorage<>(this);
        }

        private void validate() {
            if (dataFolder == null) throw new IllegalStateException("Data folder is required");
            if (fileName == null) throw new IllegalStateException("File name is required");
            if (keyClass == null) throw new IllegalStateException("Key class is required");
            if (valueClass == null) throw new IllegalStateException("Value class is required");
        }
    }

    private static class AdapterEntry<T> {
        final Type type;
        final JsonAdapter<T> jsonAdapter;

        AdapterEntry(Type type, JsonAdapter<T> jsonAdapter) {
            this.type = type;
            this.jsonAdapter = jsonAdapter;
        }
    }
}