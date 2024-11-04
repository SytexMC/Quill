package me.levitate.quill.config.serializer;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class BukkitSerializers {
    /**
     * Location serializer
     */
    public static class LocationSerializer implements TypeSerializer<Location> {
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
        @SuppressWarnings("unchecked")
        public Location deserialize(Object value) {
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("Location must be a map");
            }

            Map<String, Object> map = (Map<String, Object>) value;
            World world = Bukkit.getWorld(String.valueOf(map.get("world")));
            if (world == null) {
                throw new IllegalArgumentException("World not found: " + map.get("world"));
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
    }

    /**
     * ItemStack serializer using Base64
     */
    public static class ItemStackSerializer implements TypeSerializer<ItemStack> {
        @Override
        public Object serialize(ItemStack item) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                dataOutput.writeObject(item);
                dataOutput.close();
                return Base64Coder.encodeLines(outputStream.toByteArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize ItemStack", e);
            }
        }

        @Override
        public ItemStack deserialize(Object value) {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(String.valueOf(value)));
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                ItemStack item = (ItemStack) dataInput.readObject();
                dataInput.close();
                return item;
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize ItemStack", e);
            }
        }
    }

    /**
     * Sound serializer
     */
    public static class SoundSerializer implements TypeSerializer<Sound> {
        @Override
        public Object serialize(Sound sound) {
            return sound.name();
        }

        @Override
        public Sound deserialize(Object value) {
            try {
                return Sound.valueOf(String.valueOf(value));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid sound: " + value);
            }
        }
    }

    /**
     * Material serializer
     */
    public static class MaterialSerializer implements TypeSerializer<Material> {
        @Override
        public Object serialize(Material material) {
            return material.name();
        }

        @Override
        public Material deserialize(Object value) {
            try {
                return Material.valueOf(String.valueOf(value));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid material: " + value);
            }
        }
    }
}