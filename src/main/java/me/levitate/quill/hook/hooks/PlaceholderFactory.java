package me.levitate.quill.hook.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.levitate.quill.hook.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class PlaceholderFactory implements PluginHook {
    private static PlaceholderFactory instance;
    private final Map<String, DynamicExpansion> registeredExpansions = new HashMap<>();
    private boolean enabled = false;

    public PlaceholderFactory() {
        instance = this;
    }

    public static PlaceholderFactory getInstance() {
        if (instance == null) {
            instance = new PlaceholderFactory();
        }
        return instance;
    }

    /**
     * Sets placeholders in a text for a player
     * @param player The player to set placeholders for
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    public static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public boolean init() {
        if (Bukkit.getPluginManager().getPlugin(getPluginName()) != null) {
            enabled = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getPluginName() {
        return "PlaceholderAPI";
    }

    /**
     * Sets relational placeholders in a text between two players
     * @param one The first player
     * @param two The second player
     * @param text The text containing placeholders
     * @return The text with relational placeholders replaced
     */
    public String setRelationalPlaceholders(Player one, Player two, String text) {
        if (!enabled || text == null) return text;
        return PlaceholderAPI.setRelationalPlaceholders(one, two, text);
    }

    /**
     * Creates a new expansion builder for the specified plugin
     * @param plugin The plugin instance
     * @param identifier The identifier for the expansion
     * @return A builder to create the expansion
     */
    public PlaceholderBuilder createExpansion(Plugin plugin, String identifier) {
        if (!enabled) return null;
        return new PlaceholderBuilder(this, plugin, identifier);
    }

    /**
     * Creates a new expansion builder with custom details
     * @param identifier The identifier for the expansion
     * @param author The author name
     * @param version The version string
     * @return A builder to create the expansion
     */
    public PlaceholderBuilder createExpansion(String identifier, String author, String version) {
        if (!enabled) return null;
        return new PlaceholderBuilder(this, identifier, author, version);
    }

    /**
     * Unregisters all expansions created by this hook
     */
    public void unregisterAll() {
        registeredExpansions.values().forEach(PlaceholderExpansion::unregister);
        registeredExpansions.clear();
    }

    private static class DynamicExpansion extends PlaceholderExpansion {
        private final String identifier;
        private final String author;
        private final String version;
        private final Map<String, Function<Player, String>> placeholders = new HashMap<>();

        public DynamicExpansion(String identifier, String author, String version) {
            this.identifier = identifier.toLowerCase();
            this.author = author;
            this.version = version;
        }

        @Override
        public @NotNull String getIdentifier() {
            return identifier;
        }

        @Override
        public @NotNull String getAuthor() {
            return author;
        }

        @Override
        public @NotNull String getVersion() {
            return version;
        }

        @Override
        public String onPlaceholderRequest(Player player, String params) {
            if (player == null) return "";

            Function<Player, String> resolver = placeholders.get(params.toLowerCase());
            return resolver != null ? resolver.apply(player) : null;
        }

        public void register(String placeholder, Function<Player, String> resolver) {
            placeholders.put(placeholder.toLowerCase(), resolver);
        }

        public void registerRelational(String placeholder, Function<Player, Function<Player, String>> resolver) {
            String relationalId = "rel_" + placeholder.toLowerCase();
            placeholders.put(relationalId, player -> {
                Function<Player, String> playerResolver = resolver.apply(player);
                return playerResolver.apply(player);
            });
        }
    }

    public static class PlaceholderBuilder {
        private final PlaceholderFactory hook;
        private final DynamicExpansion expansion;

        private PlaceholderBuilder(PlaceholderFactory hook, Plugin plugin, String identifier) {
            this.hook = hook;
            String author = plugin.getDescription().getAuthors().get(0);
            this.expansion = new DynamicExpansion(
                    identifier,
                    author != null ? author : "Unknown",
                    plugin.getDescription().getVersion()
            );
        }

        private PlaceholderBuilder(PlaceholderFactory hook, String identifier, String author, String version) {
            this.hook = hook;
            this.expansion = new DynamicExpansion(identifier, author, version);
        }

        /**
         * Registers a new placeholder.
         *
         * @param placeholder The placeholder name (without the identifier prefix)
         * @param resolver The function to resolve the placeholder value
         * @return The builder instance for chaining
         */
        public PlaceholderBuilder register(String placeholder, Function<Player, String> resolver) {
            Objects.requireNonNull(placeholder, "Placeholder cannot be null");
            Objects.requireNonNull(resolver, "Resolver cannot be null");

            expansion.register(placeholder, resolver);
            return this;
        }

        /**
         * Registers a new relational placeholder (player to player).
         *
         * @param placeholder The placeholder name (without the identifier prefix)
         * @param resolver The function to resolve the relational placeholder value
         * @return The builder instance for chaining
         */
        public PlaceholderBuilder registerRelational(String placeholder, Function<Player, Function<Player, String>> resolver) {
            Objects.requireNonNull(placeholder, "Placeholder cannot be null");
            Objects.requireNonNull(resolver, "Resolver cannot be null");

            expansion.registerRelational(placeholder, resolver);
            return this;
        }

        /**
         * Builds and registers the expansion with PlaceholderAPI.
         *
         * @return true if registration was successful
         */
        public boolean register() {
            if (expansion.register()) {
                hook.registeredExpansions.put(expansion.getIdentifier(), expansion);
                return true;
            }
            return false;
        }
    }
}