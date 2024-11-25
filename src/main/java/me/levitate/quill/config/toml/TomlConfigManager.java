package me.levitate.quill.config.toml;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.Path;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import me.levitate.quill.config.annotation.Comment;
import me.levitate.quill.config.exception.ConfigurationException;
import me.levitate.quill.config.toml.annotation.Nullable;
import me.levitate.quill.config.toml.annotation.TomlConfig;
import me.levitate.quill.config.toml.exception.ConfigValidationException;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

@Module
public class TomlConfigManager {
    private final Map<Class<?>, Object> configurations = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Consumer<?>>> reloadListeners = new ConcurrentHashMap<>();

    @Inject
    private Plugin plugin;

    @PostConstruct
    public void init() {
        scanForConfigurations();
    }

    public <T> void addReloadListener(Class<T> configClass, Consumer<T> listener) {
        reloadListeners.computeIfAbsent(configClass, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(Class<T> configClass) {
        return (T) Optional.ofNullable(configurations.get(configClass))
                .orElseThrow(() -> new ConfigurationException("Configuration not loaded: " + configClass.getName()));
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

    private void scanForConfigurations() {
        try {
            String packageName = plugin.getClass().getPackage().getName();
            Set<Class<?>> classes = findConfigurationClasses(packageName);

            for (Class<?> clazz : classes) {
                try {
                    plugin.getLogger().info("Found configuration class: " + clazz.getName());
                    Object config = load(clazz);
                    configurations.put(clazz, config);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load configuration class: " + clazz.getName(), e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to scan for configurations", e);
        }
    }

    private Set<Class<?>> findConfigurationClasses(String basePackage) {
        Set<Class<?>> configs = new HashSet<>();
        try {
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());

                if (directory.exists()) {
                    findConfigClasses(basePackage, directory, configs);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error scanning for configuration classes", e);
        }
        return configs;
    }

    private void findConfigClasses(String basePackage, File directory, Set<Class<?>> configs) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                findConfigClasses(basePackage + "." + fileName, file, configs);
            } else if (fileName.endsWith(".class")) {
                String className = basePackage + '.' + fileName.substring(0, fileName.length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(TomlConfig.class)) {
                        configs.add(clazz);
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
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

                // Create a new ObjectConverter for this specific configuration
                ObjectConverter converter = new ObjectConverter();

                // Convert the loaded TOML data to our configuration class
                converter.toObject(fileConfig, instance);

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

    private void save(Object config, File file) {
        try {
            file.getParentFile().mkdirs();

            try (CommentedFileConfig commentedConfig = CommentedFileConfig.builder(file)
                    .sync()
                    .preserveInsertionOrder()
                    .parsingMode(ParsingMode.REPLACE) // Add this line
                    .build()) {

                // Convert object to TOML
                new ObjectConverter().toConfig(config, commentedConfig);

                // Process comments for all fields including nested objects
                processComments(config.getClass(), commentedConfig);

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
                if (value == null && !field.isAnnotationPresent(Nullable.class)) {
                    throw new ConfigValidationException("Field " + field.getName() + " cannot be null");
                }

                // Validate nested objects
                if (value != null &&
                        !field.getType().isPrimitive() &&
                        !field.getType().getName().startsWith("java.lang.") &&
                        !field.getType().isEnum()) {
                    validateConfig(value);
                }
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Failed to validate field: " + field.getName(), e);
            }
        }
    }

    private void processComments(Class<?> configClass, CommentedFileConfig config) {
        // Process field comments
        for (Field field : configClass.getDeclaredFields()) {
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                String path = getConfigPath(field);
                String[] comments = comment.value();
                if (comments.length > 0) {
                    // For TOML, we join with \n to create proper multi-line comments
                    config.setComment(path, String.join("\n", comments));
                }
            }

            // Handle nested objects
            try {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (!fieldType.isPrimitive() &&
                        !fieldType.getName().startsWith("java.lang.") &&
                        !fieldType.isEnum() &&
                        !Collection.class.isAssignableFrom(fieldType) &&
                        !Map.class.isAssignableFrom(fieldType)) {
                    processComments(fieldType, config);
                }
            } catch (Exception e) {
                // Log but continue processing other fields
                plugin.getLogger().warning("Failed to process comments for field: " + field.getName());
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

                    // Process comments for new fields
                    Comment comment = field.getAnnotation(Comment.class);
                    if (comment != null) {
                        config.setComment(path, String.join("\n", comment.value()));
                    }
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