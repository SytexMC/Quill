package me.levitate.quill.serializers;

import me.levitate.quill.serializers.item.ItemStackDeserializer;
import me.levitate.quill.serializers.item.ItemStackSerializer;
import me.levitate.quill.serializers.location.BasicLocationDeserializer;
import me.levitate.quill.serializers.location.BasicLocationSerializer;
import me.levitate.quill.serializers.location.LocationDeserializer;
import me.levitate.quill.serializers.location.LocationSerializer;
import me.levitate.quill.storage.JSONStorage;
import me.levitate.quill.wrapper.BasicLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class JSONSerializers {
    public static void registerDefaults(JSONStorage<?, ?> storage) {
        // Register basic type converters
        storage.addStringConverter(UUID.class, UUID::toString, UUID::fromString);
        storage.addStringConverter(Material.class, Material::name, Material::valueOf);
        storage.addStringConverter(World.class, World::getName, Bukkit::getWorld);
        
        // Register location serializers
        storage.addConverter(Location.class, new LocationSerializer(), new LocationDeserializer());
        storage.addConverter(BasicLocation.class, new BasicLocationSerializer(), new BasicLocationDeserializer());
        
        // Register item serializers
        storage.addConverter(ItemStack.class, new ItemStackSerializer(), new ItemStackDeserializer());
    }
}