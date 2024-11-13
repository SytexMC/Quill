package me.levitate.quill.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.levitate.quill.config.annotation.Comment;
import me.levitate.quill.config.annotation.Configuration;
import me.levitate.quill.config.annotation.Path;
import me.levitate.quill.config.serializer.TypeSerializer;
import me.levitate.quill.config.serializer.TypeSerializerRegistry;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

@Module
public class ConfigurationProcessor {
    @Inject
    private Plugin hostPlugin;

    private static final Gson GSON = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    // Skip fields from java.util.TimeZone and similar system classes
                    return f.getDeclaringClass().getName().startsWith("java.util");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();

    private final Map<Class<?>, Object> loadedConfigs = new HashMap<>();

    /**
     * Load a configuration class
     * @param configClass The class to load
     * @param <T> The configuration type
     * @return The loaded configuration instance
     */
    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> configClass) {
        try {
            // Check if already loaded
            if (loadedConfigs.containsKey(configClass)) {
                return (T) loadedConfigs.get(configClass);
            }

            // Get configuration annotation
            Configuration config = configClass.getAnnotation(Configuration.class);
            if (config == null) {
                throw new IllegalArgumentException("Class must be annotated with @Configuration");
            }

            // Create config instance
            T instance = configClass.getDeclaredConstructor().newInstance();

            // Load configuration
            File configFile = new File(hostPlugin.getDataFolder(), config.value());
            loadConfiguration(instance, configFile, config.autoUpdate());

            // Store in loaded configs
            loadedConfigs.put(configClass, instance);

            return instance;
        } catch (Exception e) {
            hostPlugin.getLogger().log(Level.SEVERE, "Failed to load configuration: " + configClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Save a configuration class
     * @param config The configuration instance to save
     */
    public void save(Object config) {
        try {
            Configuration annotation = config.getClass().getAnnotation(Configuration.class);
            if (annotation == null) return;

            File configFile = new File(hostPlugin.getDataFolder(), annotation.value());
            saveConfiguration(config, configFile);
        } catch (Exception e) {
            hostPlugin.getLogger().log(Level.SEVERE, "Failed to save configuration: " + config.getClass().getSimpleName(), e);
        }
    }

    /**
     * Reload a configuration class
     * @param configClass The class to reload
     * @param <T> The configuration type
     * @return The reloaded configuration instance
     */
    public <T> T reload(Class<T> configClass) {
        loadedConfigs.remove(configClass);
        return load(configClass);
    }

    private void loadConfiguration(Object instance, File file, boolean autoUpdate) throws Exception {
        // Create parent directories if needed
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        // Create default configuration if file doesn't exist
        if (!file.exists()) {
            saveConfiguration(instance, file);
            return;
        }

        // Load existing configuration
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean needsSave = false;

        // Process all fields
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // Get path from annotation or field name
            String path = getPath(field);

            // Check if value exists
            if (yaml.contains(path)) {
                // Load existing value
                Object value = yaml.get(path);
                field.set(instance, convertValue(value, field.getType(), field.getGenericType()));
            } else if (autoUpdate) {
                // Add default value if auto-update is enabled
                Object defaultValue = field.get(instance);
                yaml.set(path, serializeValue(defaultValue));
                needsSave = true;
            }
        }

        // Save if new values were added
        if (needsSave) {
            saveWithComments(yaml, instance, file);
        }
    }

    private void saveConfiguration(Object instance, File file) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();

        // Save all fields
        for (Field field : instance.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String path = getPath(field);
            Object value = field.get(instance);
            yaml.set(path, serializeValue(value));
        }

        saveWithComments(yaml, instance, file);
    }

    private void saveWithComments(YamlConfiguration yaml, Object instance, File file) throws Exception {
        // First, convert to string
        String contents = yaml.saveToString();

        // Process comments
        Map<String, String[]> comments = new LinkedHashMap<>();
        for (Field field : instance.getClass().getDeclaredFields()) {
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                comments.put(getPath(field), comment.value());
            }
        }

        // Add comments to the file
        if (!comments.isEmpty()) {
            List<String> lines = new ArrayList<>(Arrays.asList(contents.split("\n")));
            for (Map.Entry<String, String[]> entry : comments.entrySet()) {
                int index = findPathIndex(lines, entry.getKey());
                if (index != -1) {
                    for (int i = entry.getValue().length - 1; i >= 0; i--) {
                        lines.add(index, "# " + entry.getValue()[i]);
                    }
                }
            }
            contents = String.join("\n", lines);
        }

        // Write to file
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(contents);
        }
    }

    private String getPath(Field field) {
        Path path = field.getAnnotation(Path.class);
        return path != null ? path.value() : field.getName();
    }

    private int findPathIndex(List<String> lines, String path) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(path + ":")) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(Object value, Class<?> type, Type genericType) {
        if (value == null) return null;
        if (type.isInstance(value)) return value;

        // Check for registered type serializer
        TypeSerializer serializer = TypeSerializerRegistry.get(type).orElse(null);
        if (serializer != null) {
            return serializer.deserialize(value);
        }

        // Handle collections
        if (Collection.class.isAssignableFrom(type)) {
            if (!(value instanceof Collection)) {
                value = Collections.singletonList(value);
            }

            Collection<?> collection = (Collection<?>) value;
            Collection result;

            // Create appropriate collection type
            if (List.class.isAssignableFrom(type)) {
                result = new ArrayList();
            } else if (Set.class.isAssignableFrom(type)) {
                result = new HashSet();
            } else {
                result = new ArrayList();
            }

            // Get the generic type if available
            Class<?> elementType = Object.class;
            if (genericType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    elementType = (Class<?>) typeArgs[0];
                }
            }

            // Convert each element
            for (Object element : collection) {
                result.add(convertValue(element, elementType, elementType));
            }

            return result;
        }

        // Handle maps
        if (Map.class.isAssignableFrom(type)) {
            if (value instanceof ConfigurationSection section) {
                Map<String, Object> result = new HashMap<>();

                for (String key : section.getKeys(false)) {
                    result.put(key, convertValue(section.get(key), Object.class, Object.class));
                }

                return result;
            }
        }

        // Handle enums
        if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type, value.toString());
        }

        // Use GSON for complex type conversion
        return GSON.fromJson(GSON.toJson(value), genericType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object serializeValue(Object value) {
        if (value == null) return null;

        // Check for registered type serializer
        TypeSerializer serializer = TypeSerializerRegistry.get(value.getClass()).orElse(null);
        if (serializer != null) {
            return serializer.serialize(value);
        }

        // Handle primitive types and strings
        if (value instanceof String || value.getClass().isPrimitive() || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        // Handle collections
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();

            for (Object element : collection) {
                result.add(serializeValue(element));
            }

            return result;
        }

        // Handle maps
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey().toString(), serializeValue(entry.getValue()));
            }

            return result;
        }

        // Handle enums
        if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }

        // Convert complex objects to map
        return GSON.fromJson(GSON.toJson(value), Map.class);
    }
}