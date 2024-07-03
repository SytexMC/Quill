package com.featherservices.quill.config.serialization.minecraft;

import com.featherservices.quill.config.serialization.Serializer;
import com.featherservices.quill.config.serialization.TypeReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ComponentSerializer implements Serializer<Component, String> {

    @Override
    public Component deserialize(TypeReference typeReference, String object) {
        return MiniMessage.miniMessage().deserialize(object);
    }

    @Override
    public String serialize(TypeReference typeReference, Component object) {
        return MiniMessage.miniMessage().serialize(object);
    }
}
