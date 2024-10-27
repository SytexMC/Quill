package me.levitate.quill.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TaskUtils {
    /**
     * Runs a task synchronously if not already on main thread
     */
    public static void runSync(Plugin plugin, Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /**
     * Runs a task asynchronously if not already on async thread
     */
    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Runs a task after a delay
     */
    public static BukkitTask runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    /**
     * Runs a task repeatedly
     */
    public static BukkitTask runTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    /**
     * Runs a task asynchronously after a delay
     */
    public static BukkitTask runLaterAsync(Plugin plugin, Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
    }
}