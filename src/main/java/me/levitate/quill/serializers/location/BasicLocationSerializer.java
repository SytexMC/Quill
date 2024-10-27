package me.levitate.quill.serializers.location;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.levitate.quill.wrapper.BasicLocation;

import java.io.IOException;

public class BasicLocationSerializer extends JsonSerializer<BasicLocation> {
    @Override
    public void serialize(BasicLocation location, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("world", location.getWorld());
        gen.writeNumberField("x", location.getX());
        gen.writeNumberField("y", location.getY());
        gen.writeNumberField("z", location.getZ());
        gen.writeEndObject();
    }
}