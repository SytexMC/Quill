package me.levitate.quill.utils.common;

import lombok.Getter;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import me.levitate.quill.injection.annotation.PreDestroy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Module
public class TaskScheduler {
    private static final int MAX_QUEUE_SIZE = 1000;
    private final Map<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();
    private final Set<String> canceledTasks = ConcurrentHashMap.newKeySet();
    private BukkitTask mainTask;

    @Inject
    private Plugin plugin;
    private volatile boolean isShutdown = false;

    @PostConstruct
    public void init() {
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processScheduledTasks, 20L, 20L);
    }

    @PreDestroy
    public void shutdown() {
        isShutdown = true;
        if (mainTask != null) {
            mainTask.cancel();
        }
        cancelAllTasks();
    }

    public void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runAsync(Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public BukkitTask runLater(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    public BukkitTask runLaterAsync(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
    }

    public BukkitTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public BukkitTask runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
    }

    public void scheduleAt(String name, Runnable task, TimeSchedule schedule) {
        if (isShutdown) {
            throw new IllegalStateException("TaskScheduler is shutdown");
        }

        cancelScheduledTask(name);
        schedule.getNextExecutionTime().ifPresent(time -> {
            if (scheduledTasks.size() >= MAX_QUEUE_SIZE) {
                throw new IllegalStateException("Task queue is full");
            }

            ScheduledTask scheduledTask = new ScheduledTask(name, task, time, schedule);
            scheduledTasks.put(name, scheduledTask);
            canceledTasks.remove(name);
        });
    }

    public boolean cancelScheduledTask(String name) {
        if (name == null) return false;
        ScheduledTask task = scheduledTasks.remove(name);
        if (task != null) {
            canceledTasks.add(name);
            return true;
        }
        return false;
    }

    public void cancelAllTasks() {
        scheduledTasks.keySet().forEach(this::cancelScheduledTask);
        scheduledTasks.clear();
        canceledTasks.clear();
    }

    public Map<String, LocalDateTime> getScheduledTasks() {
        Map<String, LocalDateTime> tasks = new HashMap<>();
        scheduledTasks.forEach((name, task) -> {
            if (!canceledTasks.contains(name)) {
                tasks.put(name, task.executionTime);
            }
        });
        return Collections.unmodifiableMap(tasks);
    }

    private void processScheduledTasks() {
        if (isShutdown) return;

        LocalDateTime now = LocalDateTime.now();
        new ArrayList<>(scheduledTasks.values()).forEach(task -> {
            if (!canceledTasks.contains(task.name) && !now.isBefore(task.executionTime)) {
                executeTask(task);
            }
        });
    }

    private void executeTask(ScheduledTask task) {
        try {
            task.run();

            if (task.schedule.isRepeating()) {
                rescheduleTask(task);
            } else {
                scheduledTasks.remove(task.name);
                canceledTasks.remove(task.name);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing scheduled task: " + task.name + " - " + e.getMessage());
            scheduledTasks.remove(task.name);
            canceledTasks.remove(task.name);
        }
    }

    private void rescheduleTask(ScheduledTask task) {
        task.schedule.getNextExecutionTime().ifPresent(nextTime -> {
            if (!isShutdown && !canceledTasks.contains(task.name)) {
                ScheduledTask newTask = new ScheduledTask(task.name, task.task, nextTime, task.schedule);
                scheduledTasks.put(task.name, newTask);
            }
        });
    }

    private record ScheduledTask(String name, Runnable task, LocalDateTime executionTime, TimeSchedule schedule) {
        void run() {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Quill"), task);
            }
        }
    }

    public static class TimeSchedule {
        private final Set<Integer> hours = new HashSet<>();
        private final Set<Integer> minutes = new HashSet<>();
        private final Set<DayOfWeek> days = new HashSet<>();
        @Getter
        private boolean repeating = false;

        public static TimeSchedule create() {
            return new TimeSchedule();
        }

        public TimeSchedule at(int... hours) {
            for (int hour : hours) {
                if (hour < 0 || hour > 23) {
                    throw new IllegalArgumentException("Hour must be between 0 and 23");
                }
                this.hours.add(hour);
            }
            return this;
        }

        public TimeSchedule minute(int... minutes) {
            for (int minute : minutes) {
                if (minute < 0 || minute > 59) {
                    throw new IllegalArgumentException("Minute must be between 0 and 59");
                }
                this.minutes.add(minute);
            }
            return this;
        }

        public TimeSchedule on(DayOfWeek... days) {
            this.days.addAll(Arrays.asList(days));
            return this;
        }

        public TimeSchedule daily() {
            this.days.addAll(Arrays.asList(DayOfWeek.values()));
            this.repeating = true;
            return this;
        }

        public TimeSchedule weekdays() {
            this.days.addAll(Arrays.asList(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
            ));
            this.repeating = true;
            return this;
        }

        public TimeSchedule weekends() {
            this.days.add(DayOfWeek.SATURDAY);
            this.days.add(DayOfWeek.SUNDAY);
            this.repeating = true;
            return this;
        }

        public Optional<LocalDateTime> getNextExecutionTime() {
            LocalDateTime now = LocalDateTime.now();

            if (hours.isEmpty()) hours.add(now.getHour());
            if (minutes.isEmpty()) minutes.add(0);

            if (days.isEmpty() && !repeating) {
                days.add(now.getDayOfWeek());
            }

            LocalDateTime nextTime = findNextTime(now);
            return Optional.ofNullable(nextTime);
        }

        private LocalDateTime findNextTime(LocalDateTime fromTime) {
            LocalDateTime candidate = fromTime;
            int maxAttempts = 8;
            int attempts = 0;

            while (attempts < maxAttempts) {
                for (int hour : hours) {
                    for (int minute : minutes) {
                        LocalDateTime potential = candidate
                                .withHour(hour)
                                .withMinute(minute)
                                .withSecond(0)
                                .withNano(0);

                        if (potential.isBefore(fromTime)) {
                            continue;
                        }

                        if (days.isEmpty() || days.contains(potential.getDayOfWeek())) {
                            return potential;
                        }
                    }
                }

                candidate = candidate.plusDays(1);
                attempts++;
            }

            return null;
        }
    }
}