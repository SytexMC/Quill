package me.levitate.quill.hook.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.hook.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PlaceholderFactory implements PluginHook {
    private static PlaceholderFactory instance;
    private boolean enabled = false;

    public PlaceholderFactory() {}

    public static PlaceholderFactory getInstance() {
        if (instance == null) {
            instance = new PlaceholderFactory();
        }
        return instance;
    }

    @Override
    public boolean init() {
        if (Bukkit.getPluginManager().getPlugin(getPluginName()) != null) {
            enabled = true;
            instance = this;
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

    private static class DynamicExpansion extends PlaceholderExpansion {
        private final String identifier;
        private final String author;
        private final String version;
        private final Map<String, Function<Player, String>> placeholders = new ConcurrentHashMap<>();

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
        public String onPlaceholderRequest(Player player, @NotNull String params) {
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

    /**
     * Creates a new PlaceholderAPI expansion for your plugin.
     *
     * @param plugin The plugin instance
     * @param identifier The identifier for your placeholders
     * @return A builder to register placeholders
     */
    public static PlaceholderBuilder create(Plugin plugin, String identifier) {
        if (instance == null || !instance.enabled) return null;

        final String author = plugin.getDescription().getAuthors().get(0);
        return create(identifier, author != null ? author : "Quill", plugin.getDescription().getVersion());
    }

    /**
     * Creates a new PlaceholderAPI expansion with custom details.
     *
     * @param identifier The identifier for your placeholders
     * @param author The author of the expansion
     * @param version The version of the expansion
     * @return A builder to register placeholders
     */
    public static PlaceholderBuilder create(String identifier, String author, String version) {
        if (instance == null || !instance.enabled) return null;
        return new PlaceholderBuilder(identifier, author, version);
    }

    /**
     * Sets placeholders in a text for a player.
     * @param player The player to set placeholders for
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    public static String setPlaceholders(Player player, String text) {
        if (instance == null || !instance.enabled || text == null) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /**
     * Sets relational placeholders in a text between two players.
     * @param one The first player
     * @param two The second player
     * @param text The text containing placeholders
     * @return The text with relational placeholders replaced
     */
    public static String setRelationalPlaceholders(Player one, Player two, String text) {
        if (instance == null || !instance.enabled || text == null) return text;
        return PlaceholderAPI.setRelationalPlaceholders(one, two, text);
    }

    public static class PlaceholderBuilder {
        private final DynamicExpansion expansion;

        private PlaceholderBuilder(String identifier, String author, String version) {
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
            return expansion.register();
        }
    }
}