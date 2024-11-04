package me.levitate.quill.config;

import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for handling configurations
 */
public class ConfigManager {
    @Getter
    private final ConfigurationProcessor processor;
    private final Map<Class<?>, Object> configs = new HashMap<>();
    
    public ConfigManager(Plugin plugin) {
        this.processor = new ConfigurationProcessor(plugin);
    }
    
    /**
     * Register and load a configuration class
     * @param configClass The configuration class to register
     * @param <T> The configuration type
     * @return The loaded configuration instance
     */
    public <T> T register(Class<T> configClass) {
        T config = processor.load(configClass);
        configs.put(configClass, config);
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
     * Reload all registered configurations
     */
    public void reloadAll() {
        configs.replaceAll((c, v) -> processor.reload(c));
    }
    
    /**
     * Reload a specific configuration
     * @param configClass The configuration class to reload
     * @param <T> The configuration type
     * @return The reloaded configuration instance
     */
    public <T> T reload(Class<T> configClass) {
        T config = processor.reload(configClass);
        configs.put(configClass, config);
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
}