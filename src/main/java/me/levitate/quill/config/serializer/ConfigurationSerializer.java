package me.levitate.quill.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.function.Function;

public class ConfigurationSerializer<T> extends StdSerializer<T> {
    private final Function<T, Object> serializer;

    public ConfigurationSerializer(Class<T> type, Function<T, Object> serializer) {
        super(type);
        this.serializer = serializer;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Object serialized = serializer.apply(value);
        provider.defaultSerializeValue(serialized, gen);
    }
}