package me.levitate.quill.injection;

import co.aikar.commands.BaseCommand;
import lombok.Getter;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.container.DependencyContainer;
import me.levitate.quill.injection.exception.DependencyException;
import me.levitate.quill.logger.QuillLogger;
import me.levitate.quill.manager.CommandManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

@Module
public abstract class QuillPlugin extends JavaPlugin {
    @Getter
    protected DependencyContainer container;

    protected QuillLogger logger;

    @Override
    public final void onLoad() {
        onPluginLoad();
    }

    @Override
    public final void onEnable() {
        try {
            // Initialize container
            Plugin quillPlugin = getServer().getPluginManager().getPlugin("Quill");
            if (quillPlugin == null) {
                getLogger().severe("Quill plugin not found! Make sure it's installed.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // Create a dependency container for this plugin.
            container = new DependencyContainer(this, quillPlugin);

            // Register the plugin itself as a module first
            container.registerModule(getClass());

            // Set whether Quill should log information, false by default.
            container.getModule(QuillLogger.class).setDebugLog(getDebug());

            // Store the logger
            logger = container.getModule(QuillLogger.class);

            // Then discover and register other modules
            registerModules();

            // Register all the commands, this is done after to ensure all the modules have loaded.
            registerCommands(container.getModule(CommandManager.class));

            onPluginEnable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize plugin: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        if (container != null) {
            try {
                container.shutdown();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error during plugin shutdown: " + e.getMessage(), e);
            }
        }
        onPluginDisable();
    }

    private void registerCommands(CommandManager commandManager) {
        container.getModules().values().forEach(module -> {
            if (module instanceof BaseCommand command) {
                try {
                    commandManager.registerCommand(command);
                    logger.info("Registered command module: " + command.getClass().getSimpleName());
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to register command module: " + command.getClass().getName(), e);
                }
            }
        });
    }

    private void registerModules() {
        try {
            List<Class<?>> moduleClasses = findModules();

            for (Class<?> moduleClass : moduleClasses) {
                try {
                    container.registerModule(moduleClass);
                    logger.info("Registered module: " + moduleClass.getSimpleName());
                } catch (DependencyException e) {
                    getLogger().log(Level.SEVERE, "Failed to register module " + moduleClass.getName() + ": " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            throw new DependencyException("Failed to register modules", e);
        }
    }

    private List<Class<?>> findModules() throws Exception {
        List<Class<?>> modules = new ArrayList<>();
        String packageName = getClass().getPackage().getName();

        // Get the plugin's JAR file
        File pluginFile = getFile();

        // Scan JAR for classes
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                // Check if it's a class file in the plugin's package
                if (name.endsWith(".class") && name.startsWith(packageName.replace('.', '/'))) {
                    String className = name.substring(0, name.length() - 6).replace('/', '.');

                    try {
                        Class<?> clazz = Class.forName(className, false, getClassLoader());
                        if (clazz.isAnnotationPresent(Module.class)) {
                            modules.add(clazz);
                        }
                    } catch (Throwable t) {
                        getLogger().warning("Failed to load class " + className + ": " + t.getMessage());
                    }
                }
            }
        }

        return modules;
    }

    // Optional methods for plugins to override
    protected void onPluginLoad() {
    }

    protected void onPluginEnable() {
    }

    protected void onPluginDisable() {
    }

    /**
     * Determines whether debug logs will or will not be enabled.
     * @return True or false, depending on if you want to log information. (Warnings and errors will be logged regardless)
     */
    protected boolean getDebug() {
        return false;
    }
}