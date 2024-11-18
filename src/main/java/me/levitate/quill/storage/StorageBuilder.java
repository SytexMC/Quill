package me.levitate.quill.storage;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import me.levitate.quill.storage.json.JSONStorageProvider;
import me.levitate.quill.storage.provider.StorageProvider;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

public class StorageBuilder<K, V> {
    private final Plugin plugin;
    private final SerializationProvider serializationProvider;

    private Class<K> keyClass;
    private Class<V> valueClass;
    private String fileName;

    private StorageBuilder(Plugin plugin) {
        this.plugin = plugin;
        this.serializationProvider = new SerializationProvider();
    }

    public static <K, V> StorageBuilder<K, V> create(Plugin plugin) {
        return new StorageBuilder<>(plugin);
    }

    public StorageBuilder<K, V> keyClass(Class<K> keyClass) {
        this.keyClass = keyClass;
        return this;
    }

    public StorageBuilder<K, V> valueClass(Class<V> valueClass) {
        this.valueClass = valueClass;
        return this;
    }

    public StorageBuilder<K, V> fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Register a custom serializer and deserializer for a class
     * @param type The class type to serialize/deserialize
     * @param serializer The serializer implementation
     * @param deserializer The deserializer implementation
     * @param <T> The type parameter
     * @return The builder instance for chaining
     */
    public <T> StorageBuilder<K, V> registerSerializer(Class<T> type,
                                                       JsonSerializer<T> serializer,
                                                       JsonDeserializer<T> deserializer) {
        serializationProvider.registerSerializer(type, serializer, deserializer);
        return this;
    }

    /**
     * Register a simple string converter for a class
     * @param type The class type to convert
     * @param toString Function to convert object to string
     * @param fromString Function to convert string to object
     * @param <T> The type parameter
     * @return The builder instance for chaining
     */
    public <T> StorageBuilder<K, V> registerStringConverter(Class<T> type,
                                                            Function<T, String> toString,
                                                            Function<String, T> fromString) {
        serializationProvider.registerStringConverter(type, toString, fromString);
        return this;
    }

    public StorageProvider<K, V> build() {
        validateCommonParameters();
        if (fileName == null) {
            throw new IllegalStateException("File name must be specified for JSON storage");
        }
        return new JSONStorageProvider<>(plugin, keyClass, valueClass, fileName, serializationProvider);
    }

    private void validateCommonParameters() {
        if (keyClass == null || valueClass == null) {
            throw new IllegalStateException("Key and value classes must be specified");
        }
    }
}