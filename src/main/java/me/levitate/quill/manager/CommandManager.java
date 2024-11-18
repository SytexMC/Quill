package me.levitate.quill.manager;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import me.levitate.quill.injection.annotation.PreDestroy;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages command registration and handling using ACF (Annotation Command Framework).
 * Automatically discovers and registers command classes that extend BaseCommand.
 */
@Module
public class CommandManager {
    private final Set<BaseCommand> registeredCommands = new HashSet<>();
    @Inject
    private Plugin plugin;
    private PaperCommandManager manager;

    @PostConstruct
    public void init() {
        try {
            initializeManager();
            discoverAndRegisterCommands();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize command system", e);
        }
    }

    private void initializeManager() {
        this.manager = new PaperCommandManager(plugin);
        manager.enableUnstableAPI("help");
    }

    @PreDestroy
    public void shutdown() {
        if (manager != null) {
            unregisterAllCommands();
            manager = null;
        }
    }

    /**
     * Gets the ACF command manager instance
     * @return The PaperCommandManager instance
     * @throws IllegalStateException if the manager hasn't been initialized
     */
    public PaperCommandManager getManager() {
        if (manager == null) {
            throw new IllegalStateException("CommandManager hasn't been initialized yet");
        }
        return manager;
    }

    /**
     * Register a command class
     * @param command The command class to register
     * @throws IllegalArgumentException if the command is null
     */
    public void registerCommand(BaseCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        try {
            manager.registerCommand(command);
            registeredCommands.add(command);
            plugin.getLogger().info("Registered command: " + command.getClass().getSimpleName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register command: " + command.getClass().getName(), e);
        }
    }

    /**
     * Register multiple command classes
     * @param commands Collection of command classes to register
     * @throws IllegalArgumentException if the commands collection is null
     */
    public void registerCommands(Collection<BaseCommand> commands) {
        Objects.requireNonNull(commands, "Commands collection cannot be null");
        commands.forEach(this::registerCommand);
    }

    /**
     * Unregister a command class
     * @param command The command class to unregister
     * @throws IllegalArgumentException if the command is null
     */
    public void unregisterCommand(BaseCommand command) {
        Objects.requireNonNull(command, "Command cannot be null");

        try {
            manager.unregisterCommand(command);
            registeredCommands.remove(command);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to unregister command: " + command.getClass().getName(), e);
        }
    }

    private void unregisterAllCommands() {
        new HashSet<>(registeredCommands).forEach(this::unregisterCommand);
        registeredCommands.clear();
    }

    /**
     * Get all registered commands
     * @return Unmodifiable set of registered BaseCommand instances
     */
    public Set<BaseCommand> getRegisteredCommands() {
        return Collections.unmodifiableSet(registeredCommands);
    }

    private void discoverAndRegisterCommands() {
        try {
            // Get the plugin's jar file
            Path jarPath = new File(plugin.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toPath();

            if (!jarPath.toFile().exists()) {
                plugin.getLogger().warning("Could not locate plugin JAR file for command discovery");
                return;
            }

            String packageName = plugin.getClass().getPackage().getName();
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                findCommandClasses(jarFile, packageName)
                        .forEach(this::instantiateAndRegisterCommand);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to discover commands", e);
        }
    }

    private Set<Class<?>> findCommandClasses(JarFile jarFile, String packageName) {
        return jarFile.stream()
                .filter(entry -> isValidClassFile(entry, packageName))
                .map(this::loadClass)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(this::isCommandClass)
                .collect(Collectors.toSet());
    }

    private void instantiateAndRegisterCommand(Class<?> commandClass) {
        try {
            Object instance = commandClass.getDeclaredConstructor().newInstance();
            if (instance instanceof BaseCommand command) {
                registerCommand(command);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to instantiate command class: " + commandClass.getName(), e);
        }
    }

    private boolean isValidClassFile(JarEntry entry, String packageName) {
        return !entry.isDirectory() &&
                entry.getName().endsWith(".class") &&
                !entry.getName().contains("$") &&
                entry.getName().startsWith(packageName.replace('.', '/'));
    }

    private Optional<Class<?>> loadClass(JarEntry entry) {
        try {
            String className = entry.getName()
                    .substring(0, entry.getName().length() - 6)
                    .replace('/', '.');
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }

    private boolean isCommandClass(Class<?> clazz) {
        return BaseCommand.class.isAssignableFrom(clazz) &&
                !clazz.equals(BaseCommand.class) &&
                !clazz.isInterface() &&
                !clazz.isAnnotation();
    }
}