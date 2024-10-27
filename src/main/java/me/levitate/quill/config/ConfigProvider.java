package me.levitate.quill.config;

import de.exlll.configlib.YamlConfigurations;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigProvider {
    private final Plugin plugin;
    private final Map<Class<?>, Object> configs = new HashMap<>();
    private final Map<Class<?>, String> fileNames = new HashMap<>();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public ConfigProvider(Plugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    public <T> void add(Class<T> configClass, String fileName) {
        fileNames.put(configClass, fileName);
        T config = createConfig(configClass);
        configs.put(configClass, config);
    }

    private <T> T createConfig(Class<T> configClass) {
        try {
            String fileName = fileNames.get(configClass);
            Path filePath = new File(plugin.getDataFolder(), fileName).toPath();
            T config = configClass.getDeclaredConstructor().newInstance();

            if (!filePath.toFile().exists()) {
                YamlConfigurations.save(filePath, configClass, config);
            }

            return YamlConfigurations.load(filePath, configClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T get(Class<T> configClass) {
        return configClass.cast(configs.get(configClass));
    }

    public void reloadAll() {
        configs.clear();
        fileNames.forEach((configClass, fileName) -> {
            Object config = createConfig(configClass);
            configs.put(configClass, config);
        });
    }

    public <T> void reload(Class<T> configClass) {
        T config = createConfig(configClass);
        configs.put(configClass, config);
    }
}