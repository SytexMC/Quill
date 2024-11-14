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
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Module
public class ConfigManager {
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^( *)(#.*)$");
    private final Plugin plugin;
    private final ObjectMapper mapper;
    private final Map<Class<?>, Object> configCache;
    private final Map<Class<?>, List<ReloadListener<?>>> reloadListeners;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configCache = new ConcurrentHashMap<>();
        this.reloadListeners = new ConcurrentHashMap<>();

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

        // Register custom serializers for Bukkit types
        registerBukkitSerializers();
    }

    public static String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ItemStack", e);
        }
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

    public static Map<String, Object> serializeLocation(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    public static Location deserializeLocation(Map<String, Object> map) {
        return new Location(
                Bukkit.getWorld((String) map.get("world")),
                ((Number) map.get("x")).doubleValue(),
                ((Number) map.get("y")).doubleValue(),
                ((Number) map.get("z")).doubleValue(),
                ((Number) map.get("yaw")).floatValue(),
                ((Number) map.get("pitch")).floatValue()
        );
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
            save(instance);
            return instance;
        }

        Map<String, String[]> comments = new HashMap<>();
        List<String> fileContent = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        collectComments(fileContent, comments);

        instance = mapper.readValue(file, configClass);

        if (autoUpdate) {
            T defaultInstance = configClass.getDeclaredConstructor().newInstance();
            updateConfiguration(instance, defaultInstance);
            save(instance);
        }

        return instance;
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

            Map<String, String[]> comments = getConfigComments(config.getClass());

            String yamlContent = mapper.writeValueAsString(config);

            List<String> lines = new ArrayList<>(Arrays.asList(yamlContent.split("\n")));
            insertComments(lines, comments);

            Files.writeString(configFile.toPath(), String.join("\n", lines));

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

    private void insertComments(List<String> lines, Map<String, String[]> comments) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String path = extractPath(line);

            if (path != null && comments.containsKey(path)) {
                String[] commentLines = comments.get(path);
                List<String> formattedComments = new ArrayList<>();
                for (String comment : commentLines) {
                    formattedComments.add("# " + comment);
                }
                lines.addAll(i, formattedComments);
                i += commentLines.length;
            }
        }
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