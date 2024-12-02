package me.levitate.quill.storage.adapters.bukkit;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LocationAdapter extends JsonAdapter<Location> {
    @Override
    public Location fromJson(JsonReader reader) throws IOException {
        reader.beginObject();
        String world = null;
        double x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0;

        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "world" -> world = reader.nextString();
                case "x" -> x = reader.nextDouble();
                case "y" -> y = reader.nextDouble();
                case "z" -> z = reader.nextDouble();
                case "yaw" -> yaw = (float) reader.nextDouble();
                case "pitch" -> pitch = (float) reader.nextDouble();
                default -> reader.skipValue();
            }
        }
        reader.endObject();

        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) throw new IOException("World not found: " + world);
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    @Override
    public void toJson(@NotNull JsonWriter writer, Location location) throws IOException {
        if (location == null)
            return;

        writer.beginObject();
        writer.name("world").value(location.getWorld().getName());
        writer.name("x").value(location.getX());
        writer.name("y").value(location.getY());
        writer.name("z").value(location.getZ());
        writer.name("yaw").value(location.getYaw());
        writer.name("pitch").value(location.getPitch());
        writer.endObject();
    }
}