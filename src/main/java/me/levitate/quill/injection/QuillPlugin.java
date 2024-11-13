package me.levitate.quill.injection;

import lombok.Getter;
import me.levitate.quill.Quill;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.exception.DependencyException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

@Getter
@Module
public abstract class QuillPlugin extends JavaPlugin {
    protected DependencyContainer container;

    @Override
    public final void onLoad() {
        onPluginLoad();
    }

    @Override
    public final void onEnable() {
        try {
            // Initialize container
            Plugin quillPlugin = getServer().getPluginManager().getPlugin("Quill");
            if (!(quillPlugin instanceof Quill)) {
                getLogger().severe("Quill plugin not found! Make sure it's installed.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            this.container = new DependencyContainer(this, quillPlugin);

            // Register the plugin itself as a module first
            container.registerModule(getClass());

            // Then discover and register other modules
            registerModules();

            onPluginEnable();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            if (getLogger().isLoggable(Level.FINE)) {
                e.printStackTrace();
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        if (container != null) {
            try {
                container.shutdown();
            } catch (Exception e) {
                getLogger().warning("Error during plugin shutdown: " + e.getMessage());
            }
        }
        onPluginDisable();
    }

    private void registerModules() {
        try {
            List<Class<?>> moduleClasses = findModules();

            for (Class<?> moduleClass : moduleClasses) {
                try {
                    container.registerModule(moduleClass);
                    getLogger().info("Registered module: " + moduleClass.getSimpleName());
                } catch (DependencyException e) {
                    getLogger().severe("Failed to register module " + moduleClass.getName() + ": " + e.getMessage());
                    if (getLogger().isLoggable(Level.FINE)) {
                        e.printStackTrace();
                    }
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
    protected void onPluginLoad() {}
    protected void onPluginEnable() {}
    protected void onPluginDisable() {}
}