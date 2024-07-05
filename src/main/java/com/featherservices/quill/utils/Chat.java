package com.featherservices.quill.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Chat {
    public static Component translate(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    public static Component translateLegacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static List<Component> translate(List<String> text) {
        final List<Component> componentList = new ArrayList<>();

        for (String line : text) {
            componentList.add(Chat.translate(line));
        }

        return componentList;
    }

    public static void sendMessage(Player player, String text, Function<String, String> replace) {
        if (text == null || text.isEmpty())
            return;

        player.sendMessage(Chat.translate(replace.apply(text)));
    }

    public static void sendMessage(Player player, String text) {
        if (text == null || text.isEmpty())
            return;

        player.sendMessage(Chat.translate(text));
    }

    public static void sendMessage(CommandSender sender, String text, Function<String, String> replace) {
        if (text == null || text.isEmpty())
            return;

        sender.sendMessage(Chat.translate(replace.apply(text)));
    }

    public static void sendMessage(CommandSender sender, String text) {
        if (text == null || text.isEmpty())
            return;

        sender.sendMessage(Chat.translate(text));
    }
}
