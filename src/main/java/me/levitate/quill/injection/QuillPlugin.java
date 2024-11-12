package me.levitate.quill.injection;

import lombok.Getter;
import me.levitate.quill.Quill;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
        // Initialize container
        Plugin quillPlugin = getServer().getPluginManager().getPlugin("Quill");
        if (!(quillPlugin instanceof Quill)) {
            getLogger().severe("Quill plugin not found! Make sure it's installed.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.container = new DependencyContainer(this, quillPlugin);
        
        try {
            // Register the plugin itself as a module first
            container.registerModule(getClass());
            
            // Then discover and register other modules
            registerModules();
            
            onPluginEnable();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public final void onDisable() {
        if (container != null) {
            container.shutdown();
        }
        onPluginDisable();
    }
    
    private void registerModules() throws Exception {
        List<Class<?>> moduleClasses = findModules();
        
        for (Class<?> moduleClass : moduleClasses) {
            container.registerModule(moduleClass);
            getLogger().info("Registered module: " + moduleClass.getSimpleName());
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
                    // Convert file path to class name
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    
                    try {
                        Class<?> clazz = Class.forName(className, false, getClassLoader());
                        
                        // Check if class has @Module annotation
                        if (clazz.isAnnotationPresent(Module.class)) {
                            modules.add(clazz);
                        }
                    } catch (Throwable ignored) {
                        // Skip problematic classes
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