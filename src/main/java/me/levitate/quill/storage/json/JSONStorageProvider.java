package me.levitate.quill.storage.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.levitate.quill.storage.provider.AbstractStorageProvider;
import me.levitate.quill.storage.serializers.SerializationProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class JSONStorageProvider<K, V> extends AbstractStorageProvider<K, V> {
    private final ObjectMapper mapper;
    private final File storageFile;
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    private final File backupFile;

    public JSONStorageProvider(Plugin plugin, Class<K> keyClass, Class<V> valueClass,
                               String fileName, SerializationProvider serializationProvider) {
        super(plugin, keyClass, valueClass, serializationProvider);
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        serializationProvider.configureMapper(this.mapper);

        String finalFileName = fileName.endsWith(".json") ? fileName : fileName + ".json";
        this.storageFile = new File(plugin.getDataFolder(), finalFileName);
        this.backupFile = new File(plugin.getDataFolder(), finalFileName + ".backup");

        connect();
    }

    @Override
    public void connect() {
        if (isConnected()) {
            plugin.getLogger().warning("The storage is already connected.");
            return;
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().severe("Failed to create plugin data folder");
                return;
            }
            connected = true;
            load();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to storage", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            save();
            cache.clear();
            connected = false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during storage disconnect", e);
        }
    }

    public void save() {
        if (!connected) {
            plugin.getLogger().warning("Attempted to save while storage is not connected");
            return;
        }

        fileLock.writeLock().lock();
        try {
            // Create backup of existing file
            if (storageFile.exists()) {
                Files.copy(storageFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Prepare data
            Map<K, V> saveMap = new HashMap<>();
            for (K key : cache.keys()) {
                cache.get(key).ifPresent(value -> saveMap.put(key, value));
            }

            // Write to file
            mapper.writeValue(storageFile, saveMap);

            // Clean up backup
            if (backupFile.exists()) {
                Files.delete(backupFile.toPath());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data", e);
            // Attempt to restore backup
            try {
                if (backupFile.exists()) {
                    Files.copy(backupFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception restoreException) {
                plugin.getLogger().log(Level.SEVERE, "Failed to restore backup after save failure", restoreException);
            }
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    public void load() {
        if (!connected) {
            plugin.getLogger().warning("Attempted to load while storage is not connected");
            return;
        }

        fileLock.readLock().lock();
        try {
            if (!storageFile.exists()) {
                if (backupFile.exists()) {
                    Files.copy(backupFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    return; // No data to load
                }
            }

            JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            Map<K, V> loadedData = mapper.readValue(storageFile, mapType);

            cache.clear();
            if (loadedData != null) {
                loadedData.forEach(cache::put);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load data", e);
        } finally {
            fileLock.readLock().unlock();
        }
    }

    @Override
    public boolean remove(K key) {
        if (!connected) {
            plugin.getLogger().warning("Attempted to remove key while storage is not connected");
            return false;
        }

        boolean removed = cache.remove(key);
        if (removed) {
            save();
        }
        return removed;
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public CompletableFuture<Void> saveAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            save();
            runSync(() -> future.complete(null));
        });

        return future;
    }

    public CompletableFuture<Void> loadAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            load();
            runSync(() -> future.complete(null));
        });

        return future;
    }
}