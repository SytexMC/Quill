package me.levitate.quill.config.serializer.registry;

import me.levitate.quill.config.exception.SerializationException;
import me.levitate.quill.config.serializer.ConfigurationSerializer;
import me.levitate.quill.config.serializer.standard.ItemStackSerializer;
import me.levitate.quill.config.serializer.standard.LocationSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SerializerRegistry {
    private final Map<Class<?>, ConfigurationSerializer<?>> serializers = new HashMap<>();

    public SerializerRegistry() {
        registerSerializer(new ItemStackSerializer());
        registerSerializer(new LocationSerializer());
    }

    public <T> void registerSerializer(ConfigurationSerializer<T> serializer) {
        serializers.put(serializer.getType(), serializer);
    }

    public boolean hasSerializer(Class<?> type) {
        return serializers.containsKey(type);
    }

    @SuppressWarnings("unchecked")
    public <T> ConfigurationSerializer<T> getSerializer(Class<T> type) {
        return (ConfigurationSerializer<T>) Optional.ofNullable(serializers.get(type))
                .orElseThrow(() -> new SerializationException("No serializer found for type: " + type.getName()));
    }
}