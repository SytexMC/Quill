package me.levitate.quill.storage.serializers.location;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;

public class LocationDeserializer extends JsonDeserializer<Location> {
    @Override
    public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        World world = Bukkit.getWorld(node.get("world").asText());
        if (world == null) {
            throw new IOException("World not found: " + node.get("world").asText());
        }
        return new Location(
                world,
                node.get("x").asDouble(),
                node.get("y").asDouble(),
                node.get("z").asDouble(),
                node.has("yaw") ? (float) node.get("yaw").asDouble() : 0f,
                node.has("pitch") ? (float) node.get("pitch").asDouble() : 0f
        );
    }
}