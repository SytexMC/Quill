package com.featherservices.quill.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public class Chat {
    public static Component translate(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    public static String translateLegacy(Component text) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(text);
    }

    public static List<Component> translate(List<String> text) {
        final List<Component> componentList = new ArrayList<>();

        for (String line : text) {
            componentList.add(Chat.translate(line));
        }

        return componentList;
    }
}
