package me.levitate.quill.config.serializer.standard;

import me.levitate.quill.config.exception.SerializationException;
import me.levitate.quill.config.serializer.ConfigurationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class LocationSerializer implements ConfigurationSerializer<Location> {
    @Override
    public Object serialize(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    @Override
    public Location deserialize(Object value) {
        if (!(value instanceof Map)) {
            throw new SerializationException("Invalid location format");
        }

        Map<?, ?> map = (Map<?, ?>) value;
        World world = Bukkit.getWorld(String.valueOf(map.get("world")));
        if (world == null) {
            throw new SerializationException("World not found: " + map.get("world"));
        }

        return new Location(
                world,
                ((Number) map.get("x")).doubleValue(),
                ((Number) map.get("y")).doubleValue(),
                ((Number) map.get("z")).doubleValue(),
                ((Number) map.get("yaw")).floatValue(),
                ((Number) map.get("pitch")).floatValue()
        );
    }

    @Override
    public Class<Location> getType() {
        return Location.class;
    }
}