package me.levitate.quill.config.toml;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import me.levitate.quill.config.annotation.Comment;
import me.levitate.quill.config.exception.ConfigurationException;
import me.levitate.quill.config.toml.annotation.Path;
import me.levitate.quill.config.toml.annotation.TomlConfig;
import me.levitate.quill.config.toml.exception.ConfigValidationException;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

@Module
@SuppressWarnings("unused")
public class TomlConfigManager {
    private final Map<Class<?>, Object> configurations = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Consumer<?>>> reloadListeners = new ConcurrentHashMap<>();

    @Inject
    private Plugin plugin;

    public <T> void addReloadListener(Class<T> configClass, Consumer<T> listener) {
        reloadListeners.computeIfAbsent(configClass, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(Class<T> configClass) {
        // First check if already loaded
        T config = (T) configurations.get(configClass);
        if (config != null) {
            return config;
        }

        // Verify class has TomlConfig annotation
        TomlConfig annotation = configClass.getAnnotation(TomlConfig.class);
        if (annotation == null) {
            throw new ConfigurationException("Class is not annotated with @TomlConfig: " + configClass.getName());
        }

        // Load the configuration
        try {
            config = load(configClass);
            configurations.put(configClass, config);
            return config;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load configuration: " + configClass.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void reload(Class<T> configClass) {
        try {
            T config = load(configClass);
            configurations.put(configClass, config);

            // Notify listeners
            List<Consumer<?>> listeners = reloadListeners.get(configClass);
            if (listeners != null) {
                for (Consumer<?> listener : listeners) {
                    ((Consumer<T>) listener).accept(config);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configuration: " + configClass.getName(), e);
            throw new ConfigurationException("Failed to reload configuration", e);
        }
    }

    private <T> T load(Class<T> configClass) {
        TomlConfig config = configClass.getAnnotation(TomlConfig.class);
        if (config == null) {
            throw new ConfigurationException("Class is not annotated with @TomlConfig: " + configClass.getName());
        }

        File configFile = new File(plugin.getDataFolder(), config.value());
        try {
            // Create a new instance of the configuration class
            T instance = configClass.getDeclaredConstructor().newInstance();

            // If file doesn't exist, save default configuration
            if (!configFile.exists()) {
                save(instance, configFile);
                return instance;
            }

            // Load existing configuration
            try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configFile)
                    .sync()
                    .autosave()
                    .preserveInsertionOrder()
                    .onFileNotFound(FileNotFoundAction.CREATE_EMPTY)
                    .build()) {

                fileConfig.load();

                // Convert the loaded TOML data to our configuration class
                ObjectDeserializer.standard().deserializeFields(fileConfig, instance);

                // If auto-update is enabled, check for new fields and save
                if (config.autoUpdate()) {
                    updateConfig(instance, fileConfig, configFile);
                }

                return instance;
            }
        } catch (ParsingException e) {
            throw new ConfigurationException("Error parsing TOML configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load configuration: " + configClass.getName(), e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void save(Object config, File file) {
        try {
            file.getParentFile().mkdirs();

            try (CommentedFileConfig commentedConfig = CommentedFileConfig.builder(file)
                    .sync()
                    .preserveInsertionOrder()
                    .parsingMode(ParsingMode.REPLACE)
                    .build()) {

                // Convert object to TOML
                ObjectSerializer.standard().serializeFields(config, commentedConfig);

                // Save the file
                commentedConfig.save();
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to save configuration", e);
        }
    }

    private void validateConfig(Object config) {
        Class<?> configClass = config.getClass();
        for (Field field : configClass.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(config);

                // Check for null values on non-nullable fields
                if (value == null) {
                    throw new ConfigValidationException("Field " + field.getName() + " cannot be null");
                }

                // Validate nested objects
                if (!field.getType().isPrimitive() &&
                        !field.getType().getName().startsWith("java.lang.") &&
                        !field.getType().isEnum()) {
                    validateConfig(value);
                }
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Failed to validate field: " + field.getName(), e);
            }
        }
    }

    private String getConfigPath(Field field) {
        Path path = field.getAnnotation(Path.class);
        return path != null ? path.value() : field.getName();
    }

    private void updateConfig(Object instance, CommentedFileConfig config, File file) {
        boolean updated = false;

        for (Field field : instance.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String path = getConfigPath(field);
                Object value = field.get(instance);

                if (!config.contains(path) && value != null) {
                    config.set(path, value);
                    updated = true;
                }

                // Handle nested objects recursively
                if (value != null &&
                        !field.getType().isPrimitive() &&
                        !field.getType().getName().startsWith("java.lang.") &&
                        !field.getType().isEnum() &&
                        !Collection.class.isAssignableFrom(field.getType()) &&
                        !Map.class.isAssignableFrom(field.getType())) {
                    String basePath = path + ".";
                    updateNestedConfig(value, config, basePath);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update config value for " + field.getName());
            }
        }

        if (updated) {
            config.save();
        }
    }

    private void updateNestedConfig(Object instance, CommentedFileConfig config, String basePath) {
        for (Field field : instance.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String path = basePath + getConfigPath(field);
                Object value = field.get(instance);

                if (!config.contains(path) && value != null) {
                    config.set(path, value);

                    Comment comment = field.getAnnotation(Comment.class);
                    if (comment != null) {
                        config.setComment(path, String.join("\n", comment.value()));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update nested config value for " + field.getName());
            }
        }
    }
}