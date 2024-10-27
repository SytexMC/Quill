package me.levitate.quill.scoreboard;

import me.levitate.quill.chat.Chat;
import me.levitate.quill.hook.PlaceholderFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuillBoard {
    private final Plugin plugin;
    private final Set<UUID> viewers;
    private final List<String> lines;
    private String title;
    private int updateInterval;
    private boolean placeholderSupport;
    private int taskId = -1;

    private QuillBoard(Plugin plugin, String title) {
        this.plugin = plugin;
        this.title = title;
        this.viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.lines = new ArrayList<>();
        this.updateInterval = 20;
        this.placeholderSupport = true;
    }

    /**
     * Create a new scoreboard
     */
    public static Builder builder(Plugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Add a line to the scoreboard
     */
    public QuillBoard addLine(String line) {
        lines.add(line);
        update();
        return this;
    }

    /**
     * Set all lines at once
     */
    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        update();
    }

    /**
     * Remove a specific line
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
     */
    public QuillBoard clearLines() {
        lines.clear();
        update();
        return this;
    }

    /**
     * Set the title
     */
    public QuillBoard setTitle(String title) {
        this.title = title;
        update();
        return this;
    }

    /**
     * Add a viewer to the scoreboard
     */
    public QuillBoard addViewer(Player player) {
        viewers.add(player.getUniqueId());
        updatePlayer(player);
        return this;
    }

    /**
     * Add multiple viewers
     */
    public QuillBoard addViewers(Collection<? extends Player> players) {
        players.forEach(player -> viewers.add(player.getUniqueId()));
        update();
        return this;
    }

    /**
     * Remove a viewer
     */
    public void removeViewer(Player player) {
        viewers.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * Remove multiple viewers
     */
    public QuillBoard removeViewers(Collection<? extends Player> players) {
        players.forEach(this::removeViewer);
        return this;
    }

    /**
     * Get current viewers
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
        getViewers().forEach(this::updatePlayer);
    }

    /**
     * Start auto-updating
     */
    public void start() {
        if (taskId == -1) {
            taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0, updateInterval).getTaskId();
        }
    }

    /**
     * Stop auto-updating
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * Destroy this scoreboard
     */
    public void destroy() {
        stop();
        new HashSet<>(getViewers()).forEach(this::removeViewer);
        viewers.clear();
        lines.clear();
    }

    private void updatePlayer(Player player) {
        if (!player.isOnline()) return;

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "sidebar",
                Criteria.DUMMY,
                Chat.translate(processText(player, title))
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        for (String line : lines) {
            String processedLine = processText(player, line);
            objective.getScore(processedLine).setScore(score--);
        }

        player.setScoreboard(scoreboard);
    }

    private String processText(Player player, String text) {
        return placeholderSupport ? PlaceholderFactory.setPlaceholders(player, text) : text;
    }

    public static class Builder {
        private final Plugin plugin;
        private String title = "Scoreboard";
        private List<String> lines = new ArrayList<>();
        private int updateInterval = 20;
        private boolean placeholderSupport = true;

        private Builder(Plugin plugin) {
            this.plugin = plugin;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder lines(List<String> lines) {
            this.lines = new ArrayList<>(lines);
            return this;
        }

        public Builder addLine(String line) {
            this.lines.add(line);
            return this;
        }

        public Builder updateInterval(int ticks) {
            this.updateInterval = ticks;
            return this;
        }

        public Builder placeholderSupport(boolean enabled) {
            this.placeholderSupport = enabled;
            return this;
        }

        public QuillBoard build() {
            QuillBoard quillBoard = new QuillBoard(plugin, title);
            quillBoard.setLines(lines);
            quillBoard.updateInterval = updateInterval;
            quillBoard.placeholderSupport = placeholderSupport;
            return quillBoard;
        }
    }
}