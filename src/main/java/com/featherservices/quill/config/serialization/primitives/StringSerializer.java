package com.featherservices.quill.config.serialization.primitives;

import com.featherservices.quill.config.serialization.Serializer;
import com.featherservices.quill.config.serialization.TypeReference;

public class StringSerializer implements Serializer<String, String> {

    @Override
    public String deserialize(TypeReference type, String serialized) {
        return serialized;
    }

    @Override
    public String serialize(TypeReference type, String object) {
        return object;
    }
}
