package me.levitate.quill.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private static final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();

    public static void setCooldown(String id, UUID uuid, long seconds) {
        cooldowns.computeIfAbsent(id, k -> new HashMap<>())
                .put(uuid, System.currentTimeMillis() + (seconds * 1000));
    }

    public static boolean isOnCooldown(String id, UUID uuid) {
        Map<UUID, Long> cooldownMap = cooldowns.get(id);
        if (cooldownMap == null) return false;
        
        Long cooldownTime = cooldownMap.get(uuid);
        return cooldownTime != null && cooldownTime > System.currentTimeMillis();
    }

    public static long getRemainingCooldown(String id, UUID uuid) {
        Map<UUID, Long> cooldownMap = cooldowns.get(id);
        if (cooldownMap == null) return 0;
        
        Long cooldownTime = cooldownMap.get(uuid);
        if (cooldownTime == null) return 0;
        
        long remaining = (cooldownTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public static void clearCooldown(String id, UUID uuid) {
        Map<UUID, Long> cooldownMap = cooldowns.get(id);
        if (cooldownMap != null) {
            cooldownMap.remove(uuid);
        }
    }

    public static void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        cooldowns.values().forEach(map -> 
            map.entrySet().removeIf(entry -> entry.getValue() <= now)
        );
    }
}