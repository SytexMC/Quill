package com.featherservices.quill.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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

    @SuppressWarnings("CallToPrintStackTrace")
    public static double evalEquation(String input) {
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine engine = mgr.getEngineByName("JavaScript");

        try {
            final String stringResult = engine.eval(input).toString();
            return Double.parseDouble(stringResult);
        }
        catch (ScriptException ex) {
            ex.printStackTrace();
        }

        return 0;
    }


    public static int getRandomNumber(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }
}
