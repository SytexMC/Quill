package me.levitate.quill.scoreboard;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for creating new scoreboards
 */
public class BoardBuilder {
    private final Plugin plugin;
    private final List<String> lines = new ArrayList<>();
    private String title = "Scoreboard";
    private int updateInterval = 20;
    private boolean placeholderSupport = true;

    private BoardBuilder(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Set the scoreboard title
     * @param title The title to set
     * @return This builder instance
     */
    public BoardBuilder title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set all lines at once
     * @param lines The lines to set
     * @return This builder instance
     */
    public BoardBuilder lines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        return this;
    }

    /**
     * Add a single line
     * @param line The line to add
     * @return This builder instance
     */
    public BoardBuilder addLine(String line) {
        this.lines.add(line);
        return this;
    }

    /**
     * Set the update interval in ticks
     * @param ticks The interval in ticks
     * @return This builder instance
     */
    public BoardBuilder updateInterval(int ticks) {
        this.updateInterval = ticks;
        return this;
    }

    /**
     * Enable or disable PlaceholderAPI support
     * @param enabled Whether to enable PlaceholderAPI support
     * @return This builder instance
     */
    public BoardBuilder placeholderSupport(boolean enabled) {
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
        board.setUpdateInterval(updateInterval);
        board.setPlaceholderSupport(placeholderSupport);
        return board;
    }
}
