package me.levitate.quill.config.serializer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeSerializerRegistry {
    private static final Map<Class<?>, TypeSerializer<?>> SERIALIZERS = new HashMap<>();

    static {
        // Register default Bukkit serializers
        register(Location.class, new BukkitTypeSerializers.LocationSerializer());
        register(ItemStack.class, new BukkitTypeSerializers.ItemStackSerializer());
        register(Sound.class, new BukkitTypeSerializers.SoundSerializer());
        register(Material.class, new BukkitTypeSerializers.MaterialSerializer());
    }

    /**
     * Register a new type serializer
     * @param type The class type to register for
     * @param serializer The serializer implementation
     * @param <T> The type parameter
     */
    public static <T> void register(Class<T> type, TypeSerializer<T> serializer) {
        SERIALIZERS.put(type, serializer);
    }

    /**
     * Get a serializer for a specific type
     * @param type The class type
     * @param <T> The type parameter
     * @return Optional containing the serializer if found
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<TypeSerializer<T>> get(Class<T> type) {
        return Optional.ofNullable((TypeSerializer<T>) SERIALIZERS.get(type));
    }

    /**
     * Check if a type has a registered serializer
     * @param type The class type
     * @return True if a serializer exists
     */
    public static boolean hasSerializer(Class<?> type) {
        return SERIALIZERS.containsKey(type);
    }

    /**
     * Remove a type serializer
     * @param type The class type to remove
     */
    public static void unregister(Class<?> type) {
        SERIALIZERS.remove(type);
    }
}