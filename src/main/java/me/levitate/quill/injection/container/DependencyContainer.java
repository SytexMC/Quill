package me.levitate.quill.injection.container;

import lombok.Getter;
import me.levitate.quill.cache.CacheManager;
import me.levitate.quill.config.ConfigManager;
import me.levitate.quill.database.DatabaseManager;
import me.levitate.quill.event.EventManager;
import me.levitate.quill.hook.HookManager;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import me.levitate.quill.injection.annotation.PreDestroy;
import me.levitate.quill.injection.exception.DependencyException;
import me.levitate.quill.manager.CommandManager;
import me.levitate.quill.utils.common.TaskScheduler;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyContainer {
    @Getter
    private final Map<Class<?>, Object> modules = new ConcurrentHashMap<>();

    private final Map<Class<?>, List<Method>> postConstructMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();

    @Getter
    private final Plugin hostPlugin;

    @Getter
    private final Plugin quillPlugin;

    private final Logger logger;

    public DependencyContainer(Plugin hostPlugin, Plugin quillPlugin) {
        this.hostPlugin = hostPlugin;
        this.quillPlugin = quillPlugin;
        this.logger = hostPlugin.getLogger();
        modules.put(Plugin.class, hostPlugin);

        registerCoreModules();
    }

    private void registerCoreModules() {
        try {
            registerModule(CacheManager.class);
            registerModule(DatabaseManager.class);
            registerModule(ConfigManager.class);
            registerModule(HookManager.class);
            registerModule(TaskScheduler.class);
            registerModule(EventManager.class);
            registerModule(CommandManager.class);
        } catch (DependencyException e) {
            logger.log(Level.SEVERE, "Failed to register core modules:", e);
        }
    }

    public void registerModule(Class<?> moduleClass) {
        try {
            validateModuleClass(moduleClass);

            // Special case: if this is the main plugin class, use the existing instance
            if (moduleClass == hostPlugin.getClass()) {
                processPluginClass(moduleClass);
                return;
            }

            // Regular module registration
            Object instance = createInstance(moduleClass);

            injectDependencies(instance);
            modules.put(moduleClass, instance);
            scanLifecycleMethods(moduleClass);
            invokePostConstruct(instance);

            logger.fine("Successfully registered module: " + moduleClass.getName());
        } catch (Exception e) {
            throw new DependencyException("Failed to register module: " + moduleClass.getName(), e);
        }
    }

    private void validateModuleClass(Class<?> moduleClass) {
        if (moduleClass == null) {
            throw new DependencyException("Module class cannot be null");
        }

        if (!moduleClass.isAnnotationPresent(Module.class) && moduleClass != hostPlugin.getClass()) {
            throw new DependencyException("Class must be annotated with @Module: " + moduleClass.getName());
        }
    }

    private void processPluginClass(Class<?> pluginClass) {
        modules.put(pluginClass, hostPlugin);
        injectDependencies(hostPlugin);
        scanLifecycleMethods(pluginClass);
        invokePostConstruct(hostPlugin);
    }

    @SuppressWarnings("unchecked")
    public <T> T getModule(Class<T> moduleClass) {
        T module = (T) modules.get(moduleClass);
        if (module == null) {
            throw new DependencyException("Module not found: " + moduleClass.getName() +
                    ". Make sure it's registered and annotated with @Module");
        }
        return module;
    }

    public void shutdown() {
        List<Class<?>> moduleClasses = new ArrayList<>(modules.keySet());
        Collections.reverse(moduleClasses);

        for (Class<?> moduleClass : moduleClasses) {
            try {
                Object instance = modules.get(moduleClass);
                invokePreDestroy(instance);
            } catch (Exception e) {
                logger.warning("Error during shutdown of module " + moduleClass.getName() + ": " + e.getMessage());
            }
        }

        modules.clear();
        postConstructMethods.clear();
        preDestroyMethods.clear();
    }

    private Object createInstance(Class<?> clazz) {
        try {
            Constructor<?> constructor = findSuitableConstructor(clazz);
            constructor.setAccessible(true);

            // For no-args constructor, just create instance
            if (constructor.getParameterCount() == 0) {
                return constructor.newInstance();
            }

            // For parameterized constructor, resolve dependencies
            Object[] dependencies = resolveDependencies(constructor);
            return constructor.newInstance(dependencies);
        } catch (Exception e) {
            throw new DependencyException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private Constructor<?> findSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        // First try to find @Inject constructor
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                return constructor;
            }
        }

        // Then try no-args constructor
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            // If no suitable constructor found and there are constructors available, use the first one
            if (constructors.length > 0) {
                return constructors[0];
            }
            throw new DependencyException("No suitable constructor found for: " + clazz.getName());
        }
    }

    private void injectDependencies(Object instance) {
        try {
            for (Field field : instance.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    Object dependency = resolveDependency(field.getType());
                    if (dependency == null) {
                        throw new DependencyException("Could not resolve dependency for field: " + field);
                    }
                    field.set(instance, dependency);
                }
            }
        } catch (Exception e) {
            throw new DependencyException("Failed to inject dependencies for " + instance.getClass().getName(), e);
        }
    }

    private Object resolveDependency(Class<?> dependencyType) {
        // Special case for Plugin - return host plugin
        if (dependencyType == Plugin.class) {
            return hostPlugin;
        }

        // Check if dependency is already registered
        Object dependency = modules.get(dependencyType);
        if (dependency != null) {
            return dependency;
        }

        // If not found, and it's a module, try to register it
        if (dependencyType.isAnnotationPresent(Module.class)) {
            registerModule(dependencyType);
            return modules.get(dependencyType);
        }

        return null;
    }

    private Object[] resolveDependencies(Constructor<?> constructor) {
        Parameter[] parameters = constructor.getParameters();
        Object[] dependencies = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            Object dependency = resolveDependency(paramType);

            if (dependency == null) {
                throw new DependencyException("Could not resolve dependency: " + paramType.getName() +
                        " for constructor: " + constructor.getDeclaringClass().getName());
            }

            dependencies[i] = dependency;
        }

        return dependencies;
    }

    private void scanLifecycleMethods(Class<?> clazz) {
        List<Method> postConstructs = new ArrayList<>();
        List<Method> preDestroys = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                validateLifecycleMethod(method);
                postConstructs.add(method);
            }
            if (method.isAnnotationPresent(PreDestroy.class)) {
                validateLifecycleMethod(method);
                preDestroys.add(method);
            }
        }

        if (!postConstructs.isEmpty()) {
            postConstructMethods.put(clazz, postConstructs);
        }
        if (!preDestroys.isEmpty()) {
            preDestroyMethods.put(clazz, preDestroys);
        }
    }

    private void validateLifecycleMethod(Method method) {
        if (method.getParameterCount() > 0) {
            throw new DependencyException("Lifecycle method must have no parameters: " +
                    method.getDeclaringClass().getName() + "#" + method.getName());
        }
    }

    private void invokePostConstruct(Object instance) {
        List<Method> methods = postConstructMethods.get(instance.getClass());
        if (methods != null) {
            for (Method method : methods) {
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Exception e) {
                    throw new DependencyException("Failed to invoke @PostConstruct method: " +
                            method.getDeclaringClass().getName() + "#" + method.getName(), e);
                }
            }
        }
    }

    private void invokePreDestroy(Object instance) {
        List<Method> methods = preDestroyMethods.get(instance.getClass());
        if (methods != null) {
            for (Method method : methods) {
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Exception e) {
                    logger.warning("Failed to invoke @PreDestroy method: " +
                            method.getDeclaringClass().getName() + "#" + method.getName() +
                            " - " + e.getMessage());
                }
            }
        }
    }
}