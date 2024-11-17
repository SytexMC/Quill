package me.levitate.quill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.Getter;
import me.levitate.quill.config.annotation.Comment;
import me.levitate.quill.config.annotation.Configuration;
import me.levitate.quill.config.annotation.Path;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Module
public class ConfigManager {
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^( *)(#.*)$");

    @Inject
    private Plugin plugin;
    private ObjectMapper mapper;

    private Map<Class<?>, Object> configCache;
    private Map<Class<?>, List<ReloadListener<?>>> reloadListeners;
    private Map<String, Map<String, List<String>>> pathComments;

    @PostConstruct
    public void onConstruct() {
        this.configCache = new ConcurrentHashMap<>();
        this.reloadListeners = new ConcurrentHashMap<>();
        this.pathComments = new HashMap<>();

        YAMLFactory yamlFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .build();

        this.mapper = new ObjectMapper(yamlFactory)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setVisibility(new ObjectMapper()
                        .getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

        registerBukkitSerializers();
    }

    public static ItemStack deserializeItemStack(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ItemStack", e);
        }
    }

    private void registerBukkitSerializers() {
        mapper.addMixIn(ItemStack.class, ItemStackMixin.class);
        mapper.addMixIn(Location.class, LocationMixin.class);
    }

    public <T> void addReloadListener(Class<T> configClass, Consumer<T> listener) {
        reloadListeners.computeIfAbsent(configClass, k -> new ArrayList<>())
                .add(new ReloadListener<>(configClass, listener));
    }

    public <T> void removeReloadListener(Class<T> configClass, Consumer<T> listener) {
        List<ReloadListener<?>> listeners = reloadListeners.get(configClass);
        if (listeners != null) {
            listeners.removeIf(l -> l.listener() == listener);
        }
    }

    public void clearReloadListeners(Class<?> configClass) {
        reloadListeners.remove(configClass);
    }

    public void clearAllReloadListeners() {
        reloadListeners.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> configClass) {
        return (T) configCache.get(configClass);
    }

    @SuppressWarnings("unchecked")
    public <T> T load(Class<T> configClass) {
        try {
            Configuration config = configClass.getAnnotation(Configuration.class);
            if (config == null) {
                throw new IllegalArgumentException("Class must be annotated with @Configuration");
            }

            T cached = (T) configCache.get(configClass);
            if (cached != null) {
                return cached;
            }

            File configFile = new File(plugin.getDataFolder(), config.value());
            T instance = loadConfiguration(configClass, configFile, config.autoUpdate());

            if (instance != null) {
                configCache.put(configClass, instance);
            }

            return instance;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration: " + configClass.getSimpleName(), e);
            return null;
        }
    }

    private <T> T loadConfiguration(Class<T> configClass, File file, boolean autoUpdate) throws Exception {
        T instance;

        if (!file.exists()) {
            instance = configClass.getDeclaredConstructor().newInstance();
            loadCommentsFromClass(configClass);
            save(instance);
            return instance;
        }

        // Load existing comments before reading the file
        loadExistingComments(file);

        // Create new instance and load values
        instance = mapper.readValue(file, configClass);

        if (autoUpdate) {
            T defaultInstance = configClass.getDeclaredConstructor().newInstance();
            updateConfiguration(instance, defaultInstance);

            // Load comments from class annotations
            loadCommentsFromClass(configClass);

            save(instance);
        }

        return instance;
    }

    private void loadCommentsFromClass(Class<?> configClass) {
        Configuration config = configClass.getAnnotation(Configuration.class);
        if (config == null) return;

        String fileName = config.value();
        Map<String, List<String>> comments = pathComments.computeIfAbsent(fileName, k -> new HashMap<>());

        for (Field field : configClass.getDeclaredFields()) {
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                Path path = field.getAnnotation(Path.class);
                String key = path != null ? path.value() : field.getName();
                comments.put(key, Arrays.asList(comment.value()));
            }
        }
    }

    private void loadExistingComments(File file) {
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();

            String currentPath = "";
            List<String> currentComments = new ArrayList<>();
            int indent = 0;

            for (String currentLine : lines) {
                String trimmed = currentLine.trim();

                // Handle comments
                if (trimmed.startsWith("#")) {
                    currentComments.add(trimmed.substring(1).trim());
                    continue;
                }

                // Handle key-value pairs
                if (trimmed.contains(":")) {
                    String key = trimmed.split(":")[0].trim();

                    // Calculate path based on indentation
                    if (currentLine.startsWith(" ")) {
                        int currentIndent = currentLine.indexOf(key);
                        if (currentIndent > indent) {
                            currentPath += "." + key;
                        } else if (currentIndent < indent) {
                            currentPath = key;
                        } else {
                            currentPath = currentPath.substring(0, currentPath.lastIndexOf(".")) + "." + key;
                        }
                        indent = currentIndent;
                    } else {
                        currentPath = key;
                        indent = 0;
                    }

                    // Store comments if there are any
                    if (!currentComments.isEmpty()) {
                        pathComments.computeIfAbsent(file.getName(), k -> new HashMap<>())
                                .put(currentPath, new ArrayList<>(currentComments));
                        currentComments.clear();
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load existing comments", e);
        }
    }

    private void updateConfiguration(Object current, Object defaults) {
        try {
            for (Field field : defaults.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.get(current) == null) {
                    field.set(current, field.get(defaults));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error updating configuration", e);
        }
    }

    public void save(Object config) {
        try {
            Configuration annotation = config.getClass().getAnnotation(Configuration.class);
            if (annotation == null) return;

            File configFile = new File(plugin.getDataFolder(), annotation.value());
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            // Convert object to YAML
            String yaml = mapper.writeValueAsString(config);
            List<String> lines = new ArrayList<>(Arrays.asList(yaml.split("\n")));

            // Insert comments
            Map<String, List<String>> fileComments = pathComments.get(annotation.value());
            if (fileComments != null) {
                insertComments(lines, fileComments);
            }

            // Write to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration: " + config.getClass().getSimpleName(), e);
        }
    }

    public void reloadAll() {
        for (Class<?> configClass : new HashSet<>(configCache.keySet())) {
            reload(configClass);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T reload(Class<T> configClass) {
        configCache.remove(configClass);
        T config = load(configClass);

        List<ReloadListener<?>> listeners = reloadListeners.get(configClass);
        if (listeners != null) {
            for (ReloadListener<?> listener : listeners) {
                ((ReloadListener<T>) listener).onReload(config);
            }
        }

        return config;
    }

    private Map<String, String[]> getConfigComments(Class<?> configClass) {
        Map<String, String[]> comments = new HashMap<>();
        for (Field field : configClass.getDeclaredFields()) {
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                Path path = field.getAnnotation(Path.class);
                String key = path != null ? path.value() : field.getName();
                comments.put(key, comment.value());
            }
        }
        return comments;
    }

    private void collectComments(List<String> lines, Map<String, String[]> comments) {
        List<String> currentComments = new ArrayList<>();
        String currentPath = null;

        for (String line : lines) {
            if (line.trim().startsWith("#")) {
                currentComments.add(line.trim().substring(1).trim());
            } else if (!line.trim().isEmpty()) {
                if (currentPath != null && !currentComments.isEmpty()) {
                    comments.put(currentPath, currentComments.toArray(new String[0]));
                }
                currentComments.clear();
                currentPath = extractPath(line);
            }
        }
    }

    private String extractPath(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) return null;
        return line.substring(0, colonIndex).trim();
    }

    private void insertComments(List<String> lines, Map<String, List<String>> comments) {
        List<String> result = new ArrayList<>();
        Map<Integer, String> indentationPaths = new HashMap<>();
        String currentPath = "";
        int currentIndent = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                result.add(line);
                continue;
            }

            // Calculate indentation
            int indent = line.indexOf(trimmed);
            String key = trimmed.split(":")[0];

            // Update current path based on indentation
            if (indent > currentIndent) {
                currentPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            } else if (indent < currentIndent) {
                // Go back up the path tree
                while (indent < currentIndent && !currentPath.isEmpty()) {
                    currentPath = currentPath.contains(".") ?
                            currentPath.substring(0, currentPath.lastIndexOf(".")) : "";
                    currentIndent -= 2;
                }
                currentPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            } else if (indent == currentIndent) {
                currentPath = currentPath.contains(".") ?
                        currentPath.substring(0, currentPath.lastIndexOf(".")) + "." + key : key;
            }
            currentIndent = indent;

            // Add comments if they exist for this path
            List<String> pathComments = comments.get(currentPath);
            if (pathComments != null) {
                for (String comment : pathComments) {
                    result.add(" ".repeat(indent) + "# " + comment);
                }
            }

            result.add(line);
        }

        lines.clear();
        lines.addAll(result);
    }

    private abstract static class ItemStackMixin {
        @JsonCreator
        public static ItemStack deserialize(@JsonProperty("data") String data) {
            return ConfigManager.deserializeItemStack(data);
        }

        @JsonProperty("data")
        abstract String serialize();
    }

    private abstract static class LocationMixin {
        @JsonCreator
        public static Location create(
                @JsonProperty("world") String world,
                @JsonProperty("x") double x,
                @JsonProperty("y") double y,
                @JsonProperty("z") double z,
                @JsonProperty("yaw") float yaw,
                @JsonProperty("pitch") float pitch) {
            return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        }

        @JsonProperty
        abstract String getWorld();

        @JsonProperty
        abstract double getX();

        @JsonProperty
        abstract double getY();

        @JsonProperty
        abstract double getZ();

        @JsonProperty
        abstract float getYaw();

        @JsonProperty
        abstract float getPitch();
    }

    private record ReloadListener<T>(Class<T> configClass, @Getter Consumer<T> listener) {
        public void onReload(T config) {
            listener.accept(config);
        }
    }
}