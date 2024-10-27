package me.levitate.quill.utils;

import org.bukkit.entity.Player;

public class ExperienceUtils {
    public static int getTotalExperience(int level) {
        if (level <= 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
    }

    public static void setTotalExperience(Player player, int exp) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        player.giveExp(exp);
    }

    public static void giveLevels(Player player, int levels) {
        player.giveExpLevels(levels);
    }
}