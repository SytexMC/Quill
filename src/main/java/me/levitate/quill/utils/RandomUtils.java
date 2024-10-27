package me.levitate.quill.utils;

import org.bukkit.Location;

import java.util.*;

public class RandomUtils {
    private static final Random random = new Random();

    /**
     * Get a random integer between min and max (inclusive)
     */
    public static int getRandomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return random.nextInt((max - min) + 1) + min;
    }

    /**
     * Get a random double between min and max
     */
    public static double getRandomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    /**
     * Get a random element from a list
     */
    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }

    /**
     * Get a random element from an array
     */
    public static <T> T getRandomElement(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[random.nextInt(array.length)];
    }

    /**
     * Get a weighted random element
     */
    public static <T> T getWeightedRandom(Map<T, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = RandomUtils.getRandomDouble(0, totalWeight);
        double currentWeight = 0;

        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (random <= currentWeight) {
                return entry.getKey();
            }
        }
        
        return null;
    }

    /**
     * Get multiple random elements from a list without duplicates
     */
    public static <T> List<T> getRandomElements(List<T> list, int count) {
        if (list == null || list.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        
        List<T> copy = new ArrayList<>(list);
        List<T> result = new ArrayList<>();
        int size = Math.min(count, copy.size());
        
        for (int i = 0; i < size; i++) {
            result.add(copy.remove(random.nextInt(copy.size())));
        }
        
        return result;
    }

    /**
     * Get a random location within bounds
     */
    public static Location getRandomLocation(Location center, double radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = Math.sqrt(random.nextDouble()) * radius;
        double x = center.getX() + r * Math.cos(angle);
        double z = center.getZ() + r * Math.sin(angle);
        
        return new Location(center.getWorld(), x, center.getY(), z);
    }
}
