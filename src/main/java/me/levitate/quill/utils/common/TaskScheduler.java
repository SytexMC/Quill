package me.levitate.quill.utils.common;

import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

@Module
public class TaskScheduler {

    @Inject
    private Plugin hostPlugin;

    /**
     * Runs a task synchronously if not already on main thread
     */
    public void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTask(hostPlugin, runnable);
    }

    /**
     * Runs a task asynchronously if not already on async thread
     */
    public void runAsync(Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(hostPlugin, runnable);
    }

    /**
     * Runs a task after a delay
     */
    public BukkitTask runLater(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(hostPlugin, runnable, delayTicks);
    }

    /**
     * Runs a task repeatedly
     */
    public BukkitTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(hostPlugin, runnable, delayTicks, periodTicks);
    }

    /**
     * Runs a task asynchronously after a delay
     */
    public BukkitTask runLaterAsync(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(hostPlugin, runnable, delayTicks);
    }
}