package me.levitate.quill.hook;

import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import me.levitate.quill.hook.hooks.*;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages all plugin hooks and their initialization
 */
@Module
public class HookManager {
    @Inject
    private Plugin plugin;

    private final Map<String, PluginHook> hooks = new HashMap<>();

    @PostConstruct
    public void init() {
        registerHooks();
    }

    private void registerHooks() {
        registerHook(new VaultHook());
        registerHook(new PlaceholderFactory());
        registerHook(new LuckPermsHook());
    }

    public void registerHook(PluginHook hook) {
        if (plugin.getServer().getPluginManager().getPlugin(hook.getPluginName()) != null) {
            if (hook.init()) {
                hooks.put(hook.getPluginName(), hook);
                plugin.getLogger().info("Successfully hooked into " + hook.getPluginName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginHook> Optional<T> getHook(Class<T> hookClass) {
        for (PluginHook hook : hooks.values()) {
            if (hookClass.isInstance(hook) && hook.isEnabled()) {
                return Optional.of((T) hook);
            }
        }
        return Optional.empty();
    }
}