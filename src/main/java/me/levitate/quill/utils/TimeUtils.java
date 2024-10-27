package me.levitate.quill.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static final int SECONDS_PER_DAY = 86400;
    public static final int SECONDS_PER_HOUR = 3600;
    public static final int SECONDS_PER_MINUTE = 60;

    private static final NavigableMap<Long, String> TIME_UNITS = new TreeMap<>(Collections.reverseOrder());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        TIME_UNITS.put(TimeUnit.DAYS.toMillis(365), "year");
        TIME_UNITS.put(TimeUnit.DAYS.toMillis(30), "month");
        TIME_UNITS.put(TimeUnit.DAYS.toMillis(7), "week");
        TIME_UNITS.put(TimeUnit.DAYS.toMillis(1), "day");
        TIME_UNITS.put(TimeUnit.HOURS.toMillis(1), "hour");
        TIME_UNITS.put(TimeUnit.MINUTES.toMillis(1), "minute");
        TIME_UNITS.put(TimeUnit.SECONDS.toMillis(1), "second");
    }

    // Time retrieval methods
    public static int getCurrentTimeSeconds() {
        return (int) (LocalTime.now().toSecondOfDay());
    }

    public static int getCurrentTimeSeconds(ZoneId zoneId) {
        return (int) (LocalTime.now(zoneId).toSecondOfDay());
    }

    // Remaining time calculations
    public static int getRemainingSeconds(int targetSeconds) {
        return getRemainingSeconds(getCurrentTimeSeconds(), targetSeconds);
    }

    public static int getRemainingSeconds(int currentSeconds, int targetSeconds) {
        return (SECONDS_PER_DAY - currentSeconds + targetSeconds) % SECONDS_PER_DAY;
    }

    public static int getRemainingSeconds(ZoneId zoneId, int targetSeconds) {
        return getRemainingSeconds(getCurrentTimeSeconds(zoneId), targetSeconds);
    }

    // Duration formatting methods
    public static String formatDuration(long seconds) {
        return formatDuration(seconds, 1);
    }

    public static String formatDuration(long seconds, int maxUnits) {
        if (seconds == 0) return "0 seconds";

        long milliseconds = seconds * 1000;
        List<String> parts = new ArrayList<>();

        for (Map.Entry<Long, String> entry : TIME_UNITS.entrySet()) {
            long unit = entry.getKey();
            String name = entry.getValue();

            if (milliseconds >= unit) {
                long count = milliseconds / unit;
                milliseconds %= unit;

                parts.add(count + " " + name + (count != 1 ? "s" : ""));

                if (parts.size() >= maxUnits) {
                    break;
                }
            }
        }

        return String.join(", ", parts);
    }

    public static String formatDurationShort(long seconds) {
        if (seconds == 0) return "0s";

        long days = seconds / SECONDS_PER_DAY;
        seconds %= SECONDS_PER_DAY;
        long hours = seconds / SECONDS_PER_HOUR;
        seconds %= SECONDS_PER_HOUR;
        long minutes = seconds / SECONDS_PER_MINUTE;
        seconds %= SECONDS_PER_MINUTE;

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d");
        if (hours > 0) result.append(hours).append("h");
        if (minutes > 0) result.append(minutes).append("m");
        if (seconds > 0) result.append(seconds).append("s");

        return result.toString();
    }

    // Time formatting methods
    public static String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    // Duration parsing methods
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Duration string cannot be empty");
        }

        input = input.toLowerCase().replaceAll("\\s+", "");
        long totalSeconds = 0;
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                if (number.length() == 0) {
                    throw new IllegalArgumentException("Invalid duration format");
                }

                long value = Long.parseLong(number.toString());
                number.setLength(0);

                switch (c) {
                    case 'y' -> totalSeconds += value * 365 * 24 * 60 * 60;
                    case 'w' -> totalSeconds += value * 7 * 24 * 60 * 60;
                    case 'd' -> totalSeconds += value * 24 * 60 * 60;
                    case 'h' -> totalSeconds += value * 60 * 60;
                    case 'm' -> totalSeconds += value * 60;
                    case 's' -> totalSeconds += value;
                    default -> throw new IllegalArgumentException("Invalid time unit: " + c);
                }
            }
        }

        if (number.length() > 0) {
            throw new IllegalArgumentException("Duration must end with a unit");
        }

        return totalSeconds;
    }

    // Utility methods
    public static long getTimeBetween(LocalDateTime start, LocalDateTime end, ChronoUnit unit) {
        return unit.between(start, end);
    }

    public static long getMillisUntilNextHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.plusHours(1).truncatedTo(ChronoUnit.HOURS);
        return Duration.between(now, nextHour).toMillis();
    }

    public static Duration getDurationUntil(LocalTime target) {
        LocalTime now = LocalTime.now();
        long secondsUntil;

        if (now.isAfter(target)) {
            // If target is tomorrow, add full day of seconds
            secondsUntil = SECONDS_PER_DAY - now.toSecondOfDay() + target.toSecondOfDay();
        } else {
            secondsUntil = target.toSecondOfDay() - now.toSecondOfDay();
        }

        return Duration.ofSeconds(secondsUntil);
    }

    public static String getTimeUntil(LocalTime target) {
        return formatDuration(getDurationUntil(target).getSeconds());
    }

    /**
     * Gets the next occurrence of a specific time
     * @param targetTime The time to get next occurrence of
     * @return LocalDateTime of the next occurrence
     */
    public static LocalDateTime getNextOccurrence(LocalTime targetTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.with(targetTime);

        if (now.toLocalTime().isAfter(targetTime)) {
            target = target.plusDays(1);
        }

        return target;
    }

    /**
     * Checks if a time falls within a range, handling overnight ranges correctly
     * @param time The time to check
     * @param start Start of the range
     * @param end End of the range
     * @return true if time is within the range
     */
    public static boolean isTimeBetween(LocalTime time, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return time.equals(start);
        }

        if (start.isBefore(end)) {
            // Simple case: start and end on same day
            return !time.isBefore(start) && !time.isAfter(end);
        } else {
            // Overnight case: e.g., 23:00 to 02:00
            return !time.isBefore(start) || !time.isAfter(end);
        }
    }

    /**
     * Gets a formatted string of the duration until a specific time
     * @param targetTime The target time
     * @param includeSeconds Whether to include seconds in the output
     * @return Formatted string of the duration
     */
    public static String getFormattedTimeUntil(LocalTime targetTime, boolean includeSeconds) {
        Duration duration = getDurationUntil(targetTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (includeSeconds) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", hours, minutes);
        }
    }

    /**
     * Checks if a time is within a specified number of minutes from now
     * @param time The time to check
     * @param minutes The number of minutes
     * @return true if the time is within the specified minutes
     */
    public static boolean isTimeWithinMinutes(LocalTime time, int minutes) {
        Duration duration = getDurationUntil(time);
        return Math.abs(duration.toMinutes()) <= minutes;
    }

    /**
     * Gets the next time from a list of times
     * @param times List of times to check
     * @return The next occurring time
     */
    public static LocalTime getNextTime(List<LocalTime> times) {
        if (times == null || times.isEmpty()) {
            throw new IllegalArgumentException("Times list cannot be empty");
        }

        LocalTime now = LocalTime.now();
        return times.stream()
                .filter(time -> time.isAfter(now))
                .min(LocalTime::compareTo)
                .orElse(times.stream().min(LocalTime::compareTo).get());
    }

    /**
     * Formats a duration in a countdown style (HH:MM:SS)
     * @param seconds The number of seconds
     * @return Formatted countdown string
     */
    public static String formatCountdown(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    public static String getRelativeTimeString(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now).abs();

        if (duration.toMinutes() < 1) return "just now";
        if (duration.toMinutes() < 60) return duration.toMinutes() + " minutes ago";
        if (duration.toHours() < 24) return duration.toHours() + " hours ago";
        if (duration.toDays() < 30) return duration.toDays() + " days ago";
        if (duration.toDays() < 365) return (duration.toDays() / 30) + " months ago";
        return (duration.toDays() / 365) + " years ago";
    }
}