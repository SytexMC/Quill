package com.featherservices.quill.config.serialization.other;


import com.featherservices.quill.config.serialization.Serializer;
import com.featherservices.quill.config.serialization.TypeReference;

import java.util.UUID;

public class UUIDSerializer implements Serializer<UUID, String> {

    @Override
    public UUID deserialize(TypeReference type, String serialized) {
        try {
            return UUID.fromString(serialized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to deserialize UUID: " + serialized + " as it is not a valid UUID");
        }
    }

    @Override
    public String serialize(TypeReference type, UUID uuid) {
        return uuid.toString();
    }
}
