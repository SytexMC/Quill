package me.levitate.quill.storage.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.levitate.quill.storage.provider.AbstractStorageProvider;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class JSONStorageProvider<K, V> extends AbstractStorageProvider<K, V> {
    private final ObjectMapper mapper;
    private final File file;
    private final Object fileLock = new Object();

    public JSONStorageProvider(Plugin plugin, Class<K> keyClass, Class<V> valueClass,
                               String fileName, SerializationProvider serializationProvider) {
        super(plugin, keyClass, valueClass, serializationProvider);
        final String finalFileName = fileName + (fileName.endsWith(".json") ? "" : ".json");
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        serializationProvider.configureMapper(this.mapper);
        this.file = new File(plugin.getDataFolder(), finalFileName);

        try {
            connect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to connect to JSON storage.", e);
        }
    }

    @Override
    public void connect() throws Exception {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new Exception("Failed to create data folder");
        }
        connected = true;
        load();
    }

    @Override
    public void disconnect() throws Exception {
        save();
        cache.clear();
        connected = false;
    }

    @Override
    public void save() throws Exception {
        synchronized (fileLock) {
            Map<K, V> saveMap = new HashMap<>();
            for (K key : cache.keys()) {
                cache.get(key).ifPresent(value -> saveMap.put(key, value));
            }

            File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
            mapper.writeValue(tempFile, saveMap);

            if (file.exists() && !file.delete()) {
                throw new Exception("Failed to delete existing file during save");
            }
            if (!tempFile.renameTo(file)) {
                throw new Exception("Failed to rename temporary file during save");
            }
        }
    }

    @Override
    public void load() throws Exception {
        synchronized (fileLock) {
            if (!file.exists()) return;

            JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            Map<K, V> loadedData = mapper.readValue(file, mapType);

            cache.clear();
            if (loadedData != null) {
                loadedData.forEach(cache::put);
            }
        }
    }

    @Override
    public void saveKey(K key) throws Exception {
        synchronized (fileLock) {
            Map<K, V> saveMap = new HashMap<>();

            if (file.exists()) {
                JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
                Map<K, V> existingData = mapper.readValue(file, mapType);
                if (existingData != null) {
                    saveMap.putAll(existingData);
                }
            }

            cache.get(key).ifPresent(value -> saveMap.put(key, value));

            File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
            mapper.writeValue(tempFile, saveMap);

            if (file.exists() && !file.delete()) {
                throw new Exception("Failed to delete existing file during save");
            }
            if (!tempFile.renameTo(file)) {
                throw new Exception("Failed to rename temporary file during save");
            }
        }
    }

    @Override
    public void loadKey(K key) throws Exception {
        synchronized (fileLock) {
            if (!file.exists()) return;

            JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            Map<K, V> loadedData = mapper.readValue(file, mapType);

            if (loadedData != null && loadedData.containsKey(key)) {
                cache.put(key, loadedData.get(key));
            }
        }
    }

    @Override
    public CompletableFuture<Void> loadKeyAsync(K key) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                loadKey(key);
                runSync(() -> future.complete(null));
            } catch (Exception e) {
                runSync(() -> future.completeExceptionally(e));
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> saveKeyAsync(K key) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                saveKey(key);
                runSync(() -> future.complete(null));
            } catch (Exception e) {
                runSync(() -> future.completeExceptionally(e));
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> saveAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                save();
                runSync(() -> future.complete(null));
            } catch (Exception e) {
                runSync(() -> future.completeExceptionally(e));
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> loadAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                load();
                runSync(() -> future.complete(null));
            } catch (Exception e) {
                runSync(() -> future.completeExceptionally(e));
            }
        });

        return future;
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    @Override
    public boolean remove(K key) {
        boolean removed = cache.remove(key);
        if (removed) {
            try {
                synchronized (fileLock) {
                    Map<K, V> saveMap = new HashMap<>();

                    if (file.exists()) {
                        JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
                        Map<K, V> existingData = mapper.readValue(file, mapType);
                        if (existingData != null) {
                            saveMap.putAll(existingData);
                        }
                    }

                    saveMap.remove(key);

                    File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
                    mapper.writeValue(tempFile, saveMap);

                    if (file.exists() && !file.delete()) {
                        throw new Exception("Failed to delete existing file during save");
                    }
                    if (!tempFile.renameTo(file)) {
                        throw new Exception("Failed to rename temporary file during save");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove key from storage: " + key, e);
                return false;
            }
        }
        return removed;
    }
}