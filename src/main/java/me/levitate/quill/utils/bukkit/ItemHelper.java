package me.levitate.quill.utils.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemHelper {
    @SuppressWarnings("deprecation")
    public static ItemStack createCustomHead(String base64Value) {
        Objects.requireNonNull(base64Value, "Base64 value cannot be null");

        UUID id = UUID.nameUUIDFromBytes(base64Value.getBytes());
        int less = (int) id.getLeastSignificantBits();
        int most = (int) id.getMostSignificantBits();

        return Bukkit.getUnsafe().modifyItemStack(
                new ItemStack(Material.PLAYER_HEAD),
                "{SkullOwner:{Id:[I;" + (less * most) + "," + (less >> 23) + "," +
                        (most / less) + "," + (most * 8731) + "],Properties:{textures:[{Value:\"" +
                        base64Value + "\"}]}}}"
        );
    }

    public static boolean removeItems(Player player, Material material, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(material, "Material cannot be null");

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (!hasEnoughItems(player, material, amount)) {
            return false;
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }

            int stackAmount = stack.getAmount();
            int toRemove = Math.min(remaining, stackAmount);
            remaining -= toRemove;

            if (stackAmount - toRemove <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stackAmount - toRemove);
            }
        }

        player.updateInventory();
        return true;
    }

    public static boolean hasEnoughItems(Player player, Material material, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(material, "Material cannot be null");

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    public static String formatMaterialName(Material material) {
        Objects.requireNonNull(material, "Material cannot be null");
        return Arrays.stream(material.name().toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}