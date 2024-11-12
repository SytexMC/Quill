package me.levitate.quill.config;

import lombok.Getter;
import me.levitate.quill.config.annotation.Configuration;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.bukkit.Bukkit.getServer;

/**
 * Manager class for handling configurations with reload support
 */
@Module
public class ConfigManager {
    @Inject
    private Plugin hostPlugin;

    @Getter
    @Inject
    private ConfigurationProcessor processor;

    private final Map<Class<?>, Object> configs = new HashMap<>();
    private final Map<Class<?>, List<ReloadListener<?>>> reloadListeners = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            registerConfigurations();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register configurations", e);
        }
    }

    /**
     * Register and load a configuration class
     * @param configClass The configuration class to register
     * @param <T> The configuration type
     * @return The loaded configuration instance
     */
    public <T> T register(Class<T> configClass) {
        // Skip if already registered
        if (configs.containsKey(configClass)) {
            return get(configClass);
        }

        T config = processor.load(configClass);
        if (config != null) {
            configs.put(configClass, config);
        }

        return config;
    }

    /**
     * Get a registered configuration
     * @param configClass The configuration class
     * @param <T> The configuration type
     * @return The configuration instance
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> configClass) {
        return (T) configs.get(configClass);
    }

    /**
     * Add a reload listener for a specific configuration
     * @param configClass The configuration class to listen for
     * @param listener The listener to call when config is reloaded
     * @param <T> The configuration type
     */
    public <T> void addReloadListener(Class<T> configClass, Consumer<T> listener) {
        reloadListeners.computeIfAbsent(configClass, k -> new ArrayList<>())
                .add(new ReloadListener<>(configClass, listener));
    }

    /**
     * Remove a reload listener
     * @param configClass The configuration class
     * @param listener The listener to remove
     * @param <T> The configuration type
     */
    public <T> void removeReloadListener(Class<T> configClass, Consumer<T> listener) {
        List<ReloadListener<?>> listeners = reloadListeners.get(configClass);
        if (listeners != null) {
            listeners.removeIf(l -> l.getListener() == listener);
        }
    }

    /**
     * Reload all registered configurations
     */
    public void reloadAll() {
        for (Class<?> configClass : new HashSet<>(configs.keySet())) {
            reload(configClass);
        }
    }

    /**
     * Reload a specific configuration
     * @param configClass The configuration class to reload
     * @param <T> The configuration type
     * @return The reloaded configuration instance
     */
    @SuppressWarnings("unchecked")
    public <T> T reload(Class<T> configClass) {
        T config = processor.reload(configClass);
        configs.put(configClass, config);

        // Notify listeners
        List<ReloadListener<?>> listeners = reloadListeners.get(configClass);
        if (listeners != null) {
            for (ReloadListener<?> listener : listeners) {
                ((ReloadListener<T>) listener).onReload(config);
            }
        }

        return config;
    }

    /**
     * Save all registered configurations
     */
    public void saveAll() {
        configs.values().forEach(processor::save);
    }

    /**
     * Save a specific configuration
     * @param configClass The configuration class to save
     */
    public void save(Class<?> configClass) {
        Object config = configs.get(configClass);
        if (config != null) {
            processor.save(config);
        }
    }

    /**
     * Clear all reload listeners for a specific configuration
     * @param configClass The configuration class
     */
    public void clearReloadListeners(Class<?> configClass) {
        reloadListeners.remove(configClass);
    }

    /**
     * Clear all reload listeners
     */
    public void clearAllReloadListeners() {
        reloadListeners.clear();
    }

    private void registerConfigurations() throws Exception {
        String packageName = hostPlugin.getClass().getPackage().getName();

        // Get the plugin's JAR file
        final Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
        getFileMethod.setAccessible(true);

        File pluginFile = (File) getFileMethod.invoke(hostPlugin);

        // Scan JAR for classes
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Check if it's a class file in the plugin's package
                if (name.endsWith(".class") && name.startsWith(packageName.replace('.', '/'))) {
                    // Convert file path to class name
                    String className = name.substring(0, name.length() - 6).replace('/', '.');

                    try {
                        Class<?> clazz = Class.forName(className, false, hostPlugin.getClass().getClassLoader());

                        // Check if class has @Configuration annotation
                        if (clazz.isAnnotationPresent(Configuration.class)) {
                            register(clazz);
                            hostPlugin.getLogger().info("Registered configuration: " + clazz.getSimpleName());
                        }
                    } catch (Throwable ignored) {
                        // Skip problematic classes
                    }
                }
            }
        }
    }

    /**
     * Reload listener class to maintain type safety
     * @param <T> The configuration type
     */
    private static class ReloadListener<T> {
        private final Class<T> configClass;
        @Getter
        private final Consumer<T> listener;

        public ReloadListener(Class<T> configClass, Consumer<T> listener) {
            this.configClass = configClass;
            this.listener = listener;
        }

        public void onReload(T config) {
            listener.accept(config);
        }
    }
}