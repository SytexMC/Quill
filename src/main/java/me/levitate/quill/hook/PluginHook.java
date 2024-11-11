package me.levitate.quill.hook;

/**
 * Base interface for all plugin hooks
 */
public interface PluginHook {
    /**
     * Initialize the hook
     * @return true if initialization was successful
     */
    boolean init();

    /**
     * Check if the hook is enabled and working
     * @return true if the hook is enabled
     */
    boolean isEnabled();

    /**
     * Get the name of the plugin this hook is for
     * @return plugin name
     */
    String getPluginName();
}