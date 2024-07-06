package com.featherservices.quill.config.serialization.primitives;

import com.featherservices.quill.config.serialization.Serializer;
import com.featherservices.quill.config.serialization.TypeReference;

public class BooleanSerializer implements Serializer<Boolean, Boolean> {

    @Override
    public Boolean deserialize(TypeReference typeReference, Boolean serialized) {
        return serialized;
    }

    @Override
    public Boolean serialize(TypeReference typeReference, Boolean object) {
        return object;
    }

    @Override
    public boolean isCompatibleWith(Class clazz) {
        return clazz.equals(Boolean.class) || clazz.equals(boolean.class);
    }

}

