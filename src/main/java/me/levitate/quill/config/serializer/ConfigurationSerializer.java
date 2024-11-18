package me.levitate.quill.config.serializer;

public interface ConfigurationSerializer<T> {
    /**
     * Serialize an object to a YAML-compatible format
     * @param value The value to serialize
     * @return A YAML-compatible object
     */
    Object serialize(T value);

    /**
     * Deserialize an object from YAML
     * @param value The YAML value
     * @return The deserialized object
     */
    T deserialize(Object value);

    /**
     * Get the type this serializer handles
     * @return The class this serializer handles
     */
    Class<T> getType();
}
