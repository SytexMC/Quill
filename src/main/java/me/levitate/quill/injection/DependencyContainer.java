package me.levitate.quill.injection;

import lombok.Getter;
import me.levitate.quill.config.ConfigManager;
import me.levitate.quill.config.ConfigurationProcessor;
import me.levitate.quill.event.EventManager;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import me.levitate.quill.injection.annotation.PreDestroy;
import me.levitate.quill.hook.HookManager;
import me.levitate.quill.utils.common.TaskScheduler;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DependencyContainer {
    private final Map<Class<?>, Object> modules = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> postConstructMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Method>> preDestroyMethods = new ConcurrentHashMap<>();
    @Getter private final Plugin hostPlugin;
    @Getter private final Plugin quillPlugin;

    public DependencyContainer(Plugin hostPlugin, Plugin quillPlugin) {
        this.hostPlugin = hostPlugin;
        this.quillPlugin = quillPlugin;
        modules.put(Plugin.class, hostPlugin);

        // Register core modules
        registerCoreModules();
    }

    private void registerCoreModules() {
        try {
            // Register core modules in dependency order
            registerModule(ConfigurationProcessor.class);
            registerModule(ConfigManager.class);
            registerModule(EventManager.class);
            registerModule(HookManager.class);
            registerModule(TaskScheduler.class);
        } catch (Exception e) {
            hostPlugin.getLogger().severe("Failed to register core modules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void registerModule(Class<?> moduleClass) {
        try {
            // Special case: if this is the main plugin class, use the existing instance
            if (moduleClass == hostPlugin.getClass()) {
                modules.put(moduleClass, hostPlugin);
                injectFields(hostPlugin);
                scanLifecycleMethods(moduleClass);
                invokePostConstruct(hostPlugin);
                return;
            }

            // Regular module registration
            if (!moduleClass.isAnnotationPresent(Module.class)) {
                throw new IllegalArgumentException("Class must be annotated with @Module: " + moduleClass.getName());
            }

            Object instance = createInstance(moduleClass);
            injectFields(instance);
            modules.put(moduleClass, instance);
            scanLifecycleMethods(moduleClass);
            invokePostConstruct(instance);
        } catch (Exception e) {
            hostPlugin.getLogger().log(Level.SEVERE, "Failed to register module: " + moduleClass.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getModule(Class<T> moduleClass) {
        T module = (T) modules.get(moduleClass);
        if (module == null) {
            throw new IllegalStateException("Module not found: " + moduleClass.getName() +
                    ". Make sure it's registered and annotated with @Module");
        }
        return module;
    }

    public void shutdown() {
        // Call @PreDestroy methods in reverse registration order
        List<Class<?>> moduleClasses = new ArrayList<>(modules.keySet());
        Collections.reverse(moduleClasses);
        
        for (Class<?> moduleClass : moduleClasses) {
            Object instance = modules.get(moduleClass);
            invokePreDestroy(instance);
        }
        
        modules.clear();
        postConstructMethods.clear();
        preDestroyMethods.clear();
    }

    private Object createInstance(Class<?> clazz) throws Exception {
        Constructor<?> constructor = getInjectableConstructor(clazz);
        constructor.setAccessible(true);
        
        // For no-args constructor, just create instance
        if (constructor.getParameterCount() == 0) {
            return constructor.newInstance();
        }
        
        // For parameterized constructor, resolve dependencies
        Object[] dependencies = resolveDependencies(constructor);
        return constructor.newInstance(dependencies);
    }

    private void injectFields(Object instance) throws Exception {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                Object dependency = resolveDependency(field.getType());
                if (dependency == null) {
                    throw new IllegalStateException("Could not resolve dependency for field: " + field);
                }
                field.set(instance, dependency);
            }
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

        // If not found and it's a module, try to register it
        if (dependencyType.isAnnotationPresent(Module.class)) {
            registerModule(dependencyType);
            return modules.get(dependencyType);
        }

        return null;
    }

    private Constructor<?> getInjectableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        
        // First try to find no-args constructor
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            // If no no-args constructor, look for @Inject constructor
            for (Constructor<?> constructor : constructors) {
                if (constructor.isAnnotationPresent(Inject.class)) {
                    return constructor;
                }
            }
            // If no @Inject constructor, use first available
            if (constructors.length > 0) {
                return constructors[0];
            }
            throw new IllegalStateException("No suitable constructor found for: " + clazz.getName());
        }
    }

    private Object[] resolveDependencies(Constructor<?> constructor) {
        Parameter[] parameters = constructor.getParameters();
        Object[] dependencies = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            Object dependency = resolveDependency(paramType);
            
            if (dependency == null) {
                throw new IllegalStateException("Could not resolve dependency: " + paramType.getName());
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
            throw new IllegalStateException("Lifecycle method must have no parameters: " + method);
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
                    hostPlugin.getLogger().log(Level.SEVERE, "Failed to invoke @PostConstruct method: " + method, e);
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
                    hostPlugin.getLogger().log(Level.SEVERE, "Failed to invoke @PreDestroy method: " + method, e);
                }
            }
        }
    }
}