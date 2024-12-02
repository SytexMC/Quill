package me.levitate.quill.storage.adapters.bukkit;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;

public class WorldAdapter extends JsonAdapter<World> {
    @Override
    public World fromJson(JsonReader reader) throws IOException {
        String worldName = reader.nextString();
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new IOException("World not found: " + worldName);
        return world;
    }

    @Override
    public void toJson(JsonWriter writer, World world) throws IOException {
        if (world == null)
            return;

        writer.value(world.getName());
    }
}