package me.levitate.quill.storage.serializers.location;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bukkit.Location;

import java.io.IOException;

public class LocationSerializer extends JsonSerializer<Location> {
    @Override
    public void serialize(Location location, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("world", location.getWorld().getName());
        gen.writeNumberField("x", location.getX());
        gen.writeNumberField("y", location.getY());
        gen.writeNumberField("z", location.getZ());
        gen.writeNumberField("yaw", location.getYaw());
        gen.writeNumberField("pitch", location.getPitch());
        gen.writeEndObject();
    }
}