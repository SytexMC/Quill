package me.levitate.quill.config;

import me.levitate.quill.config.annotation.Comment;
import me.levitate.quill.config.annotation.Configuration;
import me.levitate.quill.config.annotation.Path;
import me.levitate.quill.config.exception.ConfigurationException;
import me.levitate.quill.config.serializer.ConfigurationSerializer;
import me.levitate.quill.config.serializer.registry.SerializerRegistry;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Module
public class ConfigManager {
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*#.*$");
    private final Map<Class<?>, Object> configurations = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Consumer<?>>> reloadListeners = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> configComments = new ConcurrentHashMap<>();
    private final SerializerRegistry serializerRegistry;
    private final Yaml yaml;
    @Inject
    private Plugin plugin;

    public ConfigManager() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(new LoaderOptions());
        Representer representer = new Representer(options);

        this.yaml = new Yaml(constructor, representer, options);
        this.serializerRegistry = new SerializerRegistry();
    }

    @PostConstruct
    public void init() {
        // Scan for and load all @Configuration classes
        scanForConfigurations();
    }

    public <T> void registerSerializer(ConfigurationSerializer<T> serializer) {
        serializerRegistry.registerSerializer(serializer);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(Class<T> configClass) {
        return (T) Optional.ofNullable(configurations.get(configClass))
                .orElseThrow(() -> new ConfigurationException("Configuration not loaded: " + configClass.getName()));
    }

    public <T> void addReloadListener(Class<T> configClass, Consumer<T> listener) {
        reloadListeners.computeIfAbsent(configClass, k -> new ArrayList<>()).add(listener);
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

    // todo ~ not working.
    private void scanForConfigurations() {
        try {
            String packageName = plugin.getClass().getPackage().getName();
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            try (JarFile jar = new JarFile(jarFile)) {
                jar.stream()
                        .filter(entry -> entry.getName().endsWith(".class"))
                        .filter(entry -> entry.getName().startsWith(packageName.replace('.', '/')))
                        .forEach(entry -> {
                            String className = entry.getName()
                                    .replace('/', '.')
                                    .substring(0, entry.getName().length() - 6);

                            try {
                                Class<?> clazz = Class.forName(className);
                                if (clazz.isAnnotationPresent(Configuration.class)) {
                                    load(clazz);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING,
                                        "Failed to load configuration class: " + className, e);
                            }
                        });
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to scan for configurations", e);
        }
    }

    public <T> T load(Class<T> configClass) throws Exception {
        Configuration config = configClass.getAnnotation(Configuration.class);
        if (config == null) {
            throw new ConfigurationException("Class is not annotated with @Configuration: " + configClass.getName());
        }

        File configFile = new File(plugin.getDataFolder(), config.value());
        if (!configFile.exists()) {
            // Create new configuration
            T instance = configClass.getDeclaredConstructor().newInstance();
            saveConfiguration(instance, configFile);
            return instance;
        }

        // Load existing configuration
        Map<String, Object> data = loadYamlFile(configFile);
        T instance = configClass.getDeclaredConstructor().newInstance();

        // Update fields
        for (Field field : configClass.getDeclaredFields()) {
            field.setAccessible(true);
            String path = getConfigurationPath(field);
            Object value = getValueFromPath(data, path);

            if (value != null) {
                setFieldValue(instance, field, value);
            }
        }

        // If auto-update is enabled, save any new fields
        if (config.autoUpdate()) {
            saveConfiguration(instance, configFile);
        }

        return instance;
    }

    private void saveConfiguration(Object config, File file) throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        Class<?> configClass = config.getClass();

        // Collect all configuration data
        for (Field field : configClass.getDeclaredFields()) {
            field.setAccessible(true);
            String path = getConfigurationPath(field);
            Object value = field.get(config);

            if (value != null) {
                setValueAtPath(data, path, serializeValue(value));
            }

            // Store comments
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                String fileName = file.getName();
                configComments.computeIfAbsent(fileName, k -> new HashMap<>())
                        .put(path, Arrays.asList(comment.value()));
            }
        }

        // Ensure parent directory exists
        file.getParentFile().mkdirs();

        // Write configuration with comments
        writeConfigurationFile(file, data);
    }

    private void writeConfigurationFile(File file, Map<String, Object> data) throws IOException {
        List<String> lines = new ArrayList<>();
        String yamlContent = yaml.dump(data);

        // Split YAML content into lines
        String[] contentLines = yamlContent.split("\n");

        // Track current path for comment insertion
        String currentPath = "";
        int currentIndent = 0;

        for (String line : contentLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                lines.add("");
                continue;
            }

            // Calculate path and check for comments
            if (trimmed.contains(":")) {
                int indent = line.indexOf(trimmed);
                String key = trimmed.split(":")[0];

                if (indent > currentIndent) {
                    currentPath = currentPath.isEmpty() ? key : currentPath + "." + key;
                } else if (indent < currentIndent) {
                    currentPath = key;
                } else {
                    currentPath = currentPath.contains(".") ?
                            currentPath.substring(0, currentPath.lastIndexOf(".")) + "." + key : key;
                }
                currentIndent = indent;

                // Add comments for this path
                Map<String, List<String>> fileComments = configComments.get(file.getName());
                if (fileComments != null) {
                    List<String> comments = fileComments.get(currentPath);
                    if (comments != null) {
                        for (String comment : comments) {
                            lines.add(" ".repeat(indent) + "# " + comment);
                        }
                    }
                }
            }

            lines.add(line);
        }

        // Write the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private Map<String, Object> loadYamlFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
            return new HashMap<>();
        }
    }

    private String getConfigurationPath(Field field) {
        Path path = field.getAnnotation(Path.class);
        return path != null ? path.value() : field.getName();
    }

    private void setFieldValue(Object instance, Field field, Object value) {
        try {
            Class<?> fieldType = field.getType();
            Object convertedValue;

            if (serializerRegistry.hasSerializer(fieldType)) {
                convertedValue = serializerRegistry.getSerializer(fieldType).deserialize(value);
            } else {
                convertedValue = convertValue(value, fieldType);
            }

            field.set(instance, convertedValue);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to set field value: " + field.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object serializeValue(Object value) {
        if (value == null) return null;

        Class<?> type = value.getClass();
        if (serializerRegistry.hasSerializer(type)) {
            ConfigurationSerializer serializer = serializerRegistry.getSerializer(type);
            return serializer.serialize(value);
        }
        return value;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        // Handle primitive conversions
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        } else if (targetType == double.class || targetType == Double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == float.class || targetType == Float.class) {
            return ((Number) value).floatValue();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(value.toString());
        } else if (targetType == String.class) {
            return value.toString();
        } else if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType, value.toString());
        }

        throw new ConfigurationException("Unsupported type conversion: " + value.getClass() + " to " + targetType);
    }

    private Object getValueFromPath(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            Object obj = current.get(parts[i]);
            if (!(obj instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) obj;
        }

        return current.get(parts[parts.length - 1]);
    }

    private void setValueAtPath(Map<String, Object> data, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new LinkedHashMap<>());
        }

        current.put(parts[parts.length - 1], value);
    }
}