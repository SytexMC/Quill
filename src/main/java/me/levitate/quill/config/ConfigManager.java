package me.levitate.quill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import me.levitate.quill.config.annotation.Config;
import me.levitate.quill.config.comment.CommentedConfigurationSerializer;
import me.levitate.quill.config.exception.ConfigurationException;
import me.levitate.quill.config.reload.ConfigReloadListener;
import me.levitate.quill.config.serializer.ConfigurationDeserializer;
import me.levitate.quill.config.serializer.ConfigurationSerializer;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import me.levitate.quill.item.ItemSerializer;
import me.levitate.quill.logger.QuillLogger;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Module
@SuppressWarnings("unused")
public class ConfigManager {
    private final Map<Class<?>, CommentedConfigurationSerializer> commentSerializers = new HashMap<>();

    private Map<Class<?>, Object> configInstances;
    private List<ConfigReloadListener> reloadListeners;
    private ObjectMapper mapper;
    private SimpleModule serializerModule;

    @Inject
    private QuillLogger logger;

    @Inject
    private Plugin plugin;

    @PostConstruct
    private void init() {
        this.configInstances = new ConcurrentHashMap<>();
        this.reloadListeners = new ArrayList<>();

        // Configure Jackson
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .build();

        this.mapper = new ObjectMapper(yamlFactory)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.serializerModule = new SimpleModule();
        registerBukkitSerializers();
        mapper.registerModule(serializerModule);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(Class<T> configClass) {
        return (T) configInstances.computeIfAbsent(configClass, this::loadConfig);
    }

    public void reloadAll() {
        Set<Class<?>> configClasses = new HashSet<>(configInstances.keySet());
        configClasses.forEach(this::reloadConfig);
        notifyReloadListeners();
    }

    public void reloadConfig(Class<?> configClass) {
        configInstances.put(configClass, loadConfig(configClass));
        notifyReloadListeners();
    }

    public void addReloadListener(ConfigReloadListener listener) {
        reloadListeners.add(listener);
    }

    public <T> void registerSerializer(Class<T> type, ConfigurationSerializer<T> serializer) {
        serializerModule.addSerializer(type, serializer);
        mapper.registerModule(serializerModule);
    }

    public <T> void registerDeserializer(Class<T> type, ConfigurationDeserializer<T> deserializer) {
        serializerModule.addDeserializer(type, deserializer);
        mapper.registerModule(serializerModule);
    }

    @SuppressWarnings("unchecked")
    private void registerBukkitSerializers() {
        // ItemStack serializer
        registerSerializer(org.bukkit.inventory.ItemStack.class,
                new ConfigurationSerializer<>(org.bukkit.inventory.ItemStack.class,
                        ItemSerializer::itemStackToBase64));

        registerDeserializer(org.bukkit.inventory.ItemStack.class,
                new ConfigurationDeserializer<>(org.bukkit.inventory.ItemStack.class,
                        map -> org.bukkit.inventory.ItemStack.deserialize((Map<String, Object>) map)));

        // Location serializer
        registerSerializer(org.bukkit.Location.class,
                new ConfigurationSerializer<>(org.bukkit.Location.class,
                        location -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("world", location.getWorld().getName());
                            map.put("x", location.getX());
                            map.put("y", location.getY());
                            map.put("z", location.getZ());
                            map.put("yaw", location.getYaw());
                            map.put("pitch", location.getPitch());
                            return map;
                        }));

        registerDeserializer(org.bukkit.Location.class,
                new ConfigurationDeserializer<>(org.bukkit.Location.class,
                        map -> {
                            Map<String, Object> locationMap = (Map<String, Object>) map;
                            org.bukkit.World world = org.bukkit.Bukkit.getWorld((String) locationMap.get("world"));
                            return new org.bukkit.Location(
                                    world,
                                    ((Number) locationMap.get("x")).doubleValue(),
                                    ((Number) locationMap.get("y")).doubleValue(),
                                    ((Number) locationMap.get("z")).doubleValue(),
                                    ((Number) locationMap.get("yaw")).floatValue(),
                                    ((Number) locationMap.get("pitch")).floatValue()
                            );
                        }));
    }

    private void notifyReloadListeners() {
        reloadListeners.forEach(ConfigReloadListener::onConfigReload);
    }

    private <T> T loadConfig(Class<T> configClass) {
        Config configAnnotation = configClass.getAnnotation(Config.class);
        if (configAnnotation == null) {
            throw new ConfigurationException("Class " + configClass.getName() + " is not annotated with @Config");
        }

        File configFile = new File(plugin.getDataFolder(), configAnnotation.value());

        try {
            ObjectMapper configMapper = getMapperForConfig(configClass);
            T instance = configClass.getDeclaredConstructor().newInstance();

            if (!configFile.exists()) {
                saveDefaultConfig(configFile, instance, configMapper);
                return instance;
            }

            T loadedInstance = configMapper.readValue(configFile, configClass);
            updateMissingValues(loadedInstance, instance);
            saveConfig(configFile, loadedInstance, configMapper);
            return loadedInstance;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load configuration: " + configFile.getName(), e);
            throw new ConfigurationException("Failed to load configuration", e);
        }
    }

    private <T> void updateMissingValues(T loadedInstance, T defaultInstance) {
        try {
            for (Field field : loadedInstance.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object loadedValue = field.get(loadedInstance);
                if (loadedValue == null) {
                    Object defaultValue = field.get(defaultInstance);
                    field.set(loadedInstance, defaultValue);
                }
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to update missing values", e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveDefaultConfig(File file, Object instance, ObjectMapper configMapper) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            saveConfig(file, instance, configMapper);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to save default configuration", e);
        }
    }

    private void saveConfig(File file, Object instance, ObjectMapper configMapper) {
        try {
            configMapper.writeValue(file, instance);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to save configuration", e);
        }
    }

    private void configureCommentSerializer(Class<?> configClass) {
        CommentedConfigurationSerializer serializer = new CommentedConfigurationSerializer(configClass);
        commentSerializers.put(configClass, serializer);

        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("commentFilter", serializer);
        mapper.setFilterProvider(filterProvider);
    }

    private ObjectMapper getMapperForConfig(Class<?> configClass) {
        CommentedConfigurationSerializer serializer = commentSerializers.computeIfAbsent(
                configClass,
                CommentedConfigurationSerializer::new
        );

        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("commentFilter", serializer);

        return mapper.copy().setFilterProvider(filterProvider);
    }
}