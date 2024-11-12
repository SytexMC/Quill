package me.levitate.quill.utils.bukkit;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SoundHelper {
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
    
    public static void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public static void broadcastSound(Collection<Player> players, Sound sound, float volume, float pitch) {
        players.forEach(player -> playSound(player, sound, volume, pitch));
    }

    // Predefined sound combinations
    public static void playSuccess(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public static void playError(Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    public static void playClick(Player player) {
        playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}