package me.levitate.quill.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Random;
import java.util.UUID;

public class Util {
    @SuppressWarnings("deprecation")
    public static ItemStack getHeadFromValue(String value) {
        final UUID id = UUID.nameUUIDFromBytes(value.getBytes());
        final int less = (int) id.getLeastSignificantBits();
        final int most = (int) id.getMostSignificantBits();

        return Bukkit.getUnsafe().modifyItemStack(
                new ItemStack(Material.PLAYER_HEAD),
                "{SkullOwner:{Id:[I;" + (less * most) + "," + (less >> 23) + "," + (most / less) + "," + (most * 8731) + "],Properties:{textures:[{Value:\"" + value + "\"}]}}}"
        );
    }

    @Deprecated
    @SuppressWarnings("CallToPrintStackTrace")
    public static double evalEquation(String input) {
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine engine = mgr.getEngineByName("JavaScript");

        try {
            final String stringResult = engine.eval(input).toString();
            return Double.parseDouble(stringResult);
        } catch (ScriptException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    public static int getRandomNumber(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static void runSync(Plugin plugin, Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTask(plugin);
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskAsynchronously(plugin);
    }

    public static void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack stack : contents) {
            if (stack == null || stack.getType() != material)
                continue;

            int stackAmount = stack.getAmount();
            int toRemove = Math.min(remaining, stackAmount);
            remaining -= toRemove;
            stack.setAmount(stackAmount - toRemove);

            // If the stack is now empty, remove it from the inventory
            if (stack.getAmount() == 0) {
                player.getInventory().remove(stack);
            }

            // If we've removed enough, exit the loop
            if (remaining <= 0) return;
        }
    }

    public static String getFormattedMaterialName(Material material) {
        // Convert the Material enum name to a readable format
        String name = material.name().toLowerCase().replace('_', ' ');

        // Capitalize the first letter of each word
        String[] words = name.split(" ");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            formattedName.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }

        return formattedName.toString().trim();
    }
}
