package me.levitate.quill.config.serializer;

/**
 * Interface for custom type serialization
 * @param <T> The type to serialize
 */
public interface TypeSerializer<T> {
    /**
     * Serialize an object to a format that can be stored in YAML
     * @param value The object to serialize
     * @return The serialized object
     */
    Object serialize(T value);

    /**
     * Deserialize an object from YAML
     * @param value The value from YAML
     * @return The deserialized object
     */
    T deserialize(Object value);
}