package me.levitate.quill.storage.serializers;

import me.levitate.quill.storage.serializers.item.ItemStackDeserializer;
import me.levitate.quill.storage.serializers.item.ItemStackSerializer;
import me.levitate.quill.storage.serializers.location.BasicLocationDeserializer;
import me.levitate.quill.storage.serializers.location.BasicLocationSerializer;
import me.levitate.quill.storage.serializers.location.LocationDeserializer;
import me.levitate.quill.storage.serializers.location.LocationSerializer;
import me.levitate.quill.storage.JSONStorage;
import me.levitate.quill.utils.common.SimpleLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@Deprecated
public class JSONSerializers {
    public static void registerDefaults(JSONStorage<?, ?> storage) {
        // Register basic type converters
        storage.addStringConverter(UUID.class, UUID::toString, UUID::fromString);
        storage.addStringConverter(Material.class, Material::name, Material::valueOf);
        storage.addStringConverter(World.class, World::getName, Bukkit::getWorld);
        
        // Register location serializers
        storage.addConverter(Location.class, new LocationSerializer(), new LocationDeserializer());
        storage.addConverter(SimpleLocation.class, new BasicLocationSerializer(), new BasicLocationDeserializer());
        
        // Register item serializers
        storage.addConverter(ItemStack.class, new ItemStackSerializer(), new ItemStackDeserializer());
    }
}