package com.featherservices.quill.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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

    public static int getRandomNumber(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }
}
