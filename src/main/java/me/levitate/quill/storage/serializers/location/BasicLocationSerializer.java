package me.levitate.quill.storage.serializers.location;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.levitate.quill.utils.common.SimpleLocation;

import java.io.IOException;

public class BasicLocationSerializer extends JsonSerializer<SimpleLocation> {
    @Override
    public void serialize(SimpleLocation location, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("world", location.getWorld());
        gen.writeNumberField("x", location.getX());
        gen.writeNumberField("y", location.getY());
        gen.writeNumberField("z", location.getZ());
        gen.writeEndObject();
    }
}