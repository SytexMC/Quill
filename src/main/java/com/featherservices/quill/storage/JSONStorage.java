package com.featherservices.quill.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class JSONStorage<K, V> {
    private final Plugin plugin;
    private final ObjectMapper objectMapper;
    private final File file;

    private final HashMap<K, V> cache;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public JSONStorage(Plugin plugin, ObjectMapper objectMapper, String fileName) {
        this.plugin = plugin;
        this.objectMapper = objectMapper;
        this.file = new File(plugin.getDataFolder(), fileName);
        this.cache = new HashMap<>();

        // Create data folder if it does not exist
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdir();

        // Load all the data to the cache
        load();
    }

    public void save() {
        try {
            objectMapper.writeValue(file, cache);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving JSON storage: " + e.getMessage());
        }
    }

    public void load() {
        if (!file.exists()) {
            return;
        }

        try {
            final Map<K, V> loadedData = objectMapper.readValue(file, new TypeReference<>() {});

            cache.clear();
            cache.putAll(loadedData);
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading JSON storage: " + e.getMessage());
        }
    }
}
