package me.levitate.quill.storage.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import me.levitate.quill.storage.serializers.item.ItemStackDeserializer;
import me.levitate.quill.storage.serializers.item.ItemStackSerializer;
import me.levitate.quill.storage.serializers.location.LocationDeserializer;
import me.levitate.quill.storage.serializers.location.LocationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.UUID;

public class SerializationProvider {
    private final SimpleModule module;
    private final Map<Class<?>, JsonSerializer<?>> serializers;
    private final Map<Class<?>, JsonDeserializer<?>> deserializers;
    private final Map<Class<?>, CustomConverter<?>> stringConverters;

    public SerializationProvider() {
        this.module = new SimpleModule();
        this.serializers = new HashMap<>();
        this.deserializers = new HashMap<>();
        this.stringConverters = new HashMap<>();
        registerDefaultSerializers();
    }

    private void registerDefaultSerializers() {
        // Register built-in serializers
        registerSerializer(org.bukkit.Location.class, new LocationSerializer(), new LocationDeserializer());
        registerSerializer(org.bukkit.inventory.ItemStack.class, new ItemStackSerializer(), new ItemStackDeserializer());
        
        // Register built-in string converters
        registerStringConverter(java.util.UUID.class, UUID::toString, UUID::fromString);
        registerStringConverter(org.bukkit.Material.class, Material::name, Material::valueOf);
        registerStringConverter(org.bukkit.World.class, World::getName, Bukkit::getWorld);
    }

    /**
     * Register a custom serializer and deserializer for a class
     * @param type The class type to serialize/deserialize
     * @param serializer The serializer implementation
     * @param deserializer The deserializer implementation
     * @param <T> The type parameter
     */
    public <T> void registerSerializer(Class<T> type, JsonSerializer<T> serializer, JsonDeserializer<T> deserializer) {
        module.addSerializer(type, serializer);
        module.addDeserializer(type, deserializer);
        serializers.put(type, serializer);
        deserializers.put(type, deserializer);
    }

    /**
     * Register a simple string converter for a class
     * @param type The class type to convert
     * @param toString Function to convert object to string
     * @param fromString Function to convert string to object
     * @param <T> The type parameter
     */
    public <T> void registerStringConverter(Class<T> type, Function<T, String> toString, Function<String, T> fromString) {
        CustomConverter<T> converter = new CustomConverter<>(toString, fromString);
        stringConverters.put(type, converter);
        registerSerializer(type,
            new StringBasedSerializer<>(toString),
            new StringBasedDeserializer<>(type, fromString));
    }

    /**
     * Configure an ObjectMapper with all registered serializers
     * @param mapper The ObjectMapper to configure
     */
    public void configureMapper(ObjectMapper mapper) {
        mapper.registerModule(module);
    }

    /**
     * Get a registered serializer for a type
     * @param type The class type
     * @return The registered serializer or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> JsonSerializer<T> getSerializer(Class<T> type) {
        return (JsonSerializer<T>) serializers.get(type);
    }

    /**
     * Get a registered deserializer for a type
     * @param type The class type
     * @return The registered deserializer or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> JsonDeserializer<T> getDeserializer(Class<T> type) {
        return (JsonDeserializer<T>) deserializers.get(type);
    }

    /**
     * Get a registered string converter for a type
     * @param type The class type
     * @return The registered converter or null if not found
     */
    @SuppressWarnings({"unchecked", "ClassEscapesDefinedScope"})
    public <T> CustomConverter<T> getStringConverter(Class<T> type) {
        return (CustomConverter<T>) stringConverters.get(type);
    }

    private static class CustomConverter<T> {
        private final Function<T, String> toString;
        private final Function<String, T> fromString;

        public CustomConverter(Function<T, String> toString, Function<String, T> fromString) {
            this.toString = toString;
            this.fromString = fromString;
        }

        public String toString(T value) {
            return toString.apply(value);
        }

        public T fromString(String value) {
            return fromString.apply(value);
        }
    }

    private static class StringBasedSerializer<T> extends JsonSerializer<T> {
        private final Function<T, String> toString;

        public StringBasedSerializer(Function<T, String> toString) {
            this.toString = toString;
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(toString.apply(value));
        }
    }

    private static class StringBasedDeserializer<T> extends JsonDeserializer<T> {
        private final Class<T> type;
        private final Function<String, T> fromString;

        public StringBasedDeserializer(Class<T> type, Function<String, T> fromString) {
            this.type = type;
            this.fromString = fromString;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return fromString.apply(p.getText());
        }
    }
}