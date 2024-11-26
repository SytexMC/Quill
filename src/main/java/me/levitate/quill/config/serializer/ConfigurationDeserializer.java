package me.levitate.quill.config.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.function.Function;

public class ConfigurationDeserializer<T> extends StdDeserializer<T> {
    private final Function<Object, T> deserializer;

    public ConfigurationDeserializer(Class<T> type, Function<Object, T> deserializer) {
        super(type);
        this.deserializer = deserializer;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object value = p.readValueAs(Object.class);
        return deserializer.apply(value);
    }
}