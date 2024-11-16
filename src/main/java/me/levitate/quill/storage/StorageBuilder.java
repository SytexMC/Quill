package me.levitate.quill.storage;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;

public class StorageBuilder<K, V> {
    private final Plugin plugin;
    private Class<K> keyClass;
    private Class<V> valueClass;
    private String fileName;
    private String mongoUri;
    private String databaseName;
    private String collectionName;
    private final SerializationProvider serializationProvider;

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

    public StorageBuilder<K, V> mongoUri(String mongoUri) {
        this.mongoUri = mongoUri;
        return this;
    }

    public StorageBuilder<K, V> databaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public StorageBuilder<K, V> collectionName(String collectionName) {
        this.collectionName = collectionName;
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

    public StorageProvider<K, V> buildJson() {
        validateCommonParameters();
        if (fileName == null) {
            throw new IllegalStateException("File name must be specified for JSON storage");
        }
        return new JsonStorageProvider<>(plugin, keyClass, valueClass, fileName, serializationProvider);
    }

    public StorageProvider<K, V> buildMongo() {
        validateCommonParameters();
        validateMongoParameters();
        return new MongoStorageProvider<>(plugin, keyClass, valueClass,
                mongoUri, databaseName, collectionName, serializationProvider);
    }

    private void validateCommonParameters() {
        if (keyClass == null || valueClass == null) {
            throw new IllegalStateException("Key and value classes must be specified");
        }
    }

    private void validateMongoParameters() {
        if (mongoUri == null || databaseName == null || collectionName == null) {
            throw new IllegalStateException("MongoDB connection parameters must be specified");
        }
    }
}