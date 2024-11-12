package me.levitate.quill.utils.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PlayerHelper {

    /**
     * Converts a collection of UUIDs to a list of online players.
     * Only online players will be included in the returned list.
     *
     * @param uuids Collection of UUIDs to convert
     * @return List of online players corresponding to the UUIDs
     */
    public static List<Player> getOnlinePlayers(List<UUID> uuids) {
        Objects.requireNonNull(uuids, "UUIDs list cannot be null");

        return uuids.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

}
