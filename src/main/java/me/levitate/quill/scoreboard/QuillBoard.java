package me.levitate.quill.scoreboard;

import me.levitate.quill.chat.Chat;
import me.levitate.quill.hook.hooks.PlaceholderFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A powerful and flexible scoreboard manager for Bukkit/Spigot plugins.
 * Supports dynamic updates, PlaceholderAPI, and automatic cleanup.
 */
public class QuillBoard {
    private final Plugin plugin;
    private final Set<UUID> viewers;
    private final List<String> lines;
    private String title;
    private int updateInterval;
    private int taskId = -1;
    private boolean placeholderSupport;
    private final Map<UUID, Scoreboard> playerPreviousScoreboards = new ConcurrentHashMap<>();

    private QuillBoard(Plugin plugin, String title) {
        this.plugin = plugin;
        this.title = title;
        this.viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.lines = new ArrayList<>();
        this.updateInterval = 20;
        this.placeholderSupport = true;
    }

    /**
     * Create a new scoreboard builder
     * @param plugin Your plugin instance
     * @return A new builder instance
     */
    public static Builder builder(Plugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Add a line to the scoreboard
     * @param line The text to add
     * @return This instance for chaining
     */
    public QuillBoard addLine(String line) {
        lines.add(line);
        update();
        return this;
    }

    /**
     * Set all lines at once
     * @param lines The list of lines to set
     */
    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        update();
    }

    /**
     * Remove a specific line
     * @param index The index of the line to remove
     * @return This instance for chaining
     */
    public QuillBoard removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            update();
        }
        return this;
    }

    /**
     * Clear all lines
     * @return This instance for chaining
     */
    public QuillBoard clearLines() {
        lines.clear();
        update();
        return this;
    }

    /**
     * Set the scoreboard title
     * @param title The new title
     * @return This instance for chaining
     */
    public QuillBoard setTitle(String title) {
        this.title = title;
        update();
        return this;
    }

    /**
     * Add a viewer to the scoreboard
     * @param player The player to add
     * @return This instance for chaining
     */
    public QuillBoard addViewer(Player player) {
        UUID uuid = player.getUniqueId();

        // Store the player's current scoreboard if they have one
        Scoreboard currentScoreboard = player.getScoreboard();

        // Only set their previous scoreboard if it's not the main/default one
        if (currentScoreboard != Bukkit.getScoreboardManager().getMainScoreboard()) {
            playerPreviousScoreboards.put(uuid, currentScoreboard);
        }

        viewers.add(uuid);
        updatePlayer(player);
        return this;
    }

    /**
     * Add multiple viewers to the scoreboard
     * @param players Collection of players to add
     * @return This instance for chaining
     */
    public QuillBoard addViewers(Collection<? extends Player> players) {
        players.forEach(this::addViewer);
        return this;
    }

    /**
     * Remove a viewer from the scoreboard
     * @param player The player to remove
     */
    public void removeViewer(Player player) {
        UUID uuid = player.getUniqueId();
        viewers.remove(uuid);

        // Restore their previous scoreboard if they had one
        Scoreboard previousScoreboard = playerPreviousScoreboards.remove(uuid);
        if (previousScoreboard != null) {
            player.setScoreboard(previousScoreboard);
        } else {
            // If no previous scoreboard, set to main scoreboard
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * Remove multiple viewers from the scoreboard
     * @param players Collection of players to remove
     * @return This instance for chaining
     */
    public QuillBoard removeViewers(Collection<? extends Player> players) {
        players.forEach(this::removeViewer);
        return this;
    }

    /**
     * Get all current viewers of this scoreboard
     * @return Set of players currently viewing the scoreboard
     */
    public Set<Player> getViewers() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return Collections.unmodifiableSet(players);
    }

    /**
     * Update the scoreboard for all viewers
     */
    public void update() {
        for (UUID uuid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updatePlayer(player);
            } else {
                viewers.remove(uuid);
                playerPreviousScoreboards.remove(uuid);
            }
        }
    }

    private void updatePlayer(Player player) {
        if (!player.isOnline()) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "sidebar",
                Criteria.DUMMY,
                Chat.translate(processText(player, title))
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Use a reverse counter to ensure correct order
        int score = lines.size();
        for (String line : new ArrayList<>(lines)) {
            String processedLine = processText(player, line);
            // Ensure unique lines by adding invisible characters if needed
            if (processedLine.length() > 40) {
                processedLine = processedLine.substring(0, 40);
            }
            Team team = scoreboard.registerNewTeam("line_" + score);
            String entry = getUniqueEntry(score);
            team.addEntry(entry);
            team.prefix(Chat.translate(processedLine));
            objective.getScore(entry).setScore(score);
            score--;
        }

        player.setScoreboard(scoreboard);
    }

    private String processText(Player player, String text) {
        return placeholderSupport ? PlaceholderFactory.setPlaceholders(player, text) : text;
    }

    private String getUniqueEntry(int score) {
        return "§" + (score > 9 ? String.valueOf((char)('a' + score - 10)) : String.valueOf(score));
    }

    /**
     * Start automatic updates
     */
    public void start() {
        if (taskId == -1) {
            taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0, updateInterval).getTaskId();
        }
    }

    /**
     * Stop automatic updates
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * Destroy this scoreboard and clean up resources
     */
    public void destroy() {
        stop();
        for (UUID uuid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeViewer(player);
            }
        }
        viewers.clear();
        lines.clear();
        playerPreviousScoreboards.clear();
    }

    /**
     * Builder class for creating new scoreboards
     */
    public static class Builder {
        private final Plugin plugin;
        private String title = "Scoreboard";
        private final List<String> lines = new ArrayList<>();
        private int updateInterval = 20;
        private boolean placeholderSupport = true;

        private Builder(Plugin plugin) {
            this.plugin = plugin;
        }

        /**
         * Set the scoreboard title
         * @param title The title to set
         * @return This builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set all lines at once
         * @param lines The lines to set
         * @return This builder instance
         */
        public Builder lines(List<String> lines) {
            this.lines.clear();
            this.lines.addAll(lines);
            return this;
        }

        /**
         * Add a single line
         * @param line The line to add
         * @return This builder instance
         */
        public Builder addLine(String line) {
            this.lines.add(line);
            return this;
        }

        /**
         * Set the update interval in ticks
         * @param ticks The interval in ticks
         * @return This builder instance
         */
        public Builder updateInterval(int ticks) {
            this.updateInterval = ticks;
            return this;
        }

        /**
         * Enable or disable PlaceholderAPI support
         * @param enabled Whether to enable PlaceholderAPI support
         * @return This builder instance
         */
        public Builder placeholderSupport(boolean enabled) {
            this.placeholderSupport = enabled;
            return this;
        }

        /**
         * Build the scoreboard
         * @return A new QuillBoard instance
         */
        public QuillBoard build() {
            QuillBoard board = new QuillBoard(plugin, title);
            board.setLines(lines);
            board.updateInterval = updateInterval;
            board.placeholderSupport = placeholderSupport;
            return board;
        }
    }
}