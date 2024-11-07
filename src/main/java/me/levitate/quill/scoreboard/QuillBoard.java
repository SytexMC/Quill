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
    private int taskId = -1;
    private final Map<UUID, Scoreboard> playerPreviousScoreboards = new ConcurrentHashMap<>();

    private QuillBoard(Plugin plugin, String title) {
        this.plugin = plugin;
        this.title = title;
        this.viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.lines = new ArrayList<>();
        this.updateInterval = 20;
    }

    public static Builder builder(Plugin plugin) {
        return new Builder(plugin);
    }

    public QuillBoard addLine(String line) {
        lines.add(line);
        update();
        return this;
    }

    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        update();
    }

    public QuillBoard removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            update();
        }
        return this;
    }

    public QuillBoard clearLines() {
        lines.clear();
        update();
        return this;
    }

    public QuillBoard setTitle(String title) {
        this.title = title;
        update();
        return this;
    }

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

    public QuillBoard addViewers(Collection<? extends Player> players) {
        players.forEach(this::addViewer);
        return this;
    }

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

    public QuillBoard removeViewers(Collection<? extends Player> players) {
        players.forEach(this::removeViewer);
        return this;
    }

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

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "sidebar",
                Criteria.DUMMY,
                Chat.translate(title)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Use a reverse counter to ensure correct order
        int score = lines.size();
        for (String line : new ArrayList<>(lines)) {
            String coloredLine = Chat.colorize(line);
            // Ensure unique lines by adding invisible characters if needed
            if (coloredLine.length() > 40) {
                coloredLine = coloredLine.substring(0, 40);
            }
            Team team = scoreboard.registerNewTeam("line_" + score);
            String entry = getUniqueEntry(score);
            team.addEntry(entry);
            team.prefix(Chat.translate(line));
            objective.getScore(entry).setScore(score);
            score--;
        }

        player.setScoreboard(scoreboard);
    }

    private String getUniqueEntry(int score) {
        // Create unique entries using color codes
        return "§" + (score > 9 ? String.valueOf((char)('a' + score - 10)) : String.valueOf(score));
    }

    public void start() {
        if (taskId == -1) {
            taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0, updateInterval).getTaskId();
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

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

    public static class Builder {
        private final Plugin plugin;
        private String title = "Scoreboard";
        private final List<String> lines = new ArrayList<>();
        private int updateInterval = 20;

        private Builder(Plugin plugin) {
            this.plugin = plugin;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder lines(List<String> lines) {
            this.lines.clear();
            this.lines.addAll(lines);
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

        public QuillBoard build() {
            QuillBoard board = new QuillBoard(plugin, title);
            board.setLines(lines);
            board.updateInterval = updateInterval;
            return board;
        }
    }
}