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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Module
public class TaskScheduler {
    private final Map<String, BukkitTask> namedTasks = new ConcurrentHashMap<>();
    private final Map<BukkitTask, TaskInfo> taskInfo = new ConcurrentHashMap<>();
    private final DelayQueue<ScheduledTask> scheduledTasks = new DelayQueue<>();

    @Inject
    private Plugin hostPlugin;
    private BukkitTask cleanupTask;

    @PostConstruct
    public void init() {
        // Start cleanup task for scheduled tasks
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(hostPlugin, () -> {
            if (!scheduledTasks.isEmpty()) {
                ScheduledTask nextTask = scheduledTasks.peek();
                if (nextTask != null && nextTask.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                    scheduledTasks.poll();

                    if (Bukkit.isPrimaryThread()) {
                        nextTask.run();
                    } else {
                        Bukkit.getScheduler().runTask(hostPlugin, nextTask::run);
                    }
                }
            }
        }, 20L, 20L);
    }

    @PreDestroy
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        cancelAllTasks();
        scheduledTasks.clear();
    }

    public void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(hostPlugin, runnable);
    }

    public void runAsync(Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(hostPlugin, runnable);
    }

    public BukkitTask runLater(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(hostPlugin, runnable, delayTicks);
    }

    public BukkitTask runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(hostPlugin, runnable, delayTicks, periodTicks);
    }

    public BukkitTask runLaterAsync(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(hostPlugin, runnable, delayTicks);
    }

    // Time-based scheduling methods
    public void scheduleAt(String name, Runnable task, TimeSchedule schedule) {
        schedule.getNextExecutionTime().ifPresent(time -> {
            scheduledTasks.offer(new ScheduledTask(name, task, time, schedule));
        });
    }

    public boolean cancelScheduledTask(String name) {
        return scheduledTasks.removeIf(task -> task.name.equals(name));
    }

    /**
     * Cancels all tasks scheduled by this scheduler
     */
    public void cancelAllTasks() {
        namedTasks.values().forEach(BukkitTask::cancel);
        namedTasks.clear();
        taskInfo.clear();
        scheduledTasks.clear();
        Bukkit.getScheduler().cancelTasks(hostPlugin);
    }

    public Map<String, LocalDateTime> getScheduledTasks() {
        Map<String, LocalDateTime> tasks = new HashMap<>();
        scheduledTasks.forEach(task -> tasks.put(task.name, task.executionTime));
        return Collections.unmodifiableMap(tasks);
    }

    private record TaskInfo(String name, Runnable runnable, long period, long executionTime) {
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

            // If no specific times set, default to every hour at 0 minutes
            if (hours.isEmpty()) hours.add(now.getHour());
            if (minutes.isEmpty()) minutes.add(0);

            // If no days specified and not repeating, assume today
            if (days.isEmpty() && !repeating) {
                days.add(now.getDayOfWeek());
            }

            LocalDateTime nextTime = findNextTime(now);
            return Optional.ofNullable(nextTime);
        }

        private LocalDateTime findNextTime(LocalDateTime fromTime) {
            LocalDateTime candidate = fromTime;
            int maxAttempts = 8; // Prevent infinite loops
            int attempts = 0;

            while (attempts < maxAttempts) {
                // Check each hour/minute combination for the current day
                for (int hour : hours) {
                    for (int minute : minutes) {
                        LocalDateTime potential = candidate
                                .withHour(hour)
                                .withMinute(minute)
                                .withSecond(0)
                                .withNano(0);

                        // If time is in the past, try next day
                        if (potential.isBefore(fromTime)) {
                            continue;
                        }

                        // If no days specified or if this day is valid
                        if (days.isEmpty() || days.contains(potential.getDayOfWeek())) {
                            return potential;
                        }
                    }
                }

                // Move to next day
                candidate = candidate.plusDays(1);
                attempts++;
            }

            return null;
        }
    }

    private class ScheduledTask implements Delayed {
        private final String name;
        private final Runnable task;
        private final LocalDateTime executionTime;
        private final TimeSchedule schedule;

        ScheduledTask(String name, Runnable task, LocalDateTime executionTime, TimeSchedule schedule) {
            this.name = name;
            this.task = task;
            this.executionTime = executionTime;
            this.schedule = schedule;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = LocalDateTime.now().until(executionTime, ChronoUnit.MILLIS);
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }

        void run() {
            runSync(task);

            // If task is repeating, schedule next execution
            if (schedule.isRepeating()) {
                schedule.getNextExecutionTime().ifPresent(nextTime ->
                        scheduledTasks.offer(new ScheduledTask(name, task, nextTime, schedule)));
            }
        }
    }
}