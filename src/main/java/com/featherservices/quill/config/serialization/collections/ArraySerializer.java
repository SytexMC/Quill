package com.featherservices.quill.config.serialization.collections;

import com.featherservices.quill.config.serialization.Serializer;
import com.featherservices.quill.config.serialization.TypeReference;
import org.json.simple.JSONArray;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArraySerializer implements Serializer<Object[], Collection<?>> {

    @Override
    public Collection<?> serialize(TypeReference typeReference, Object[] object) {
        final List<?> list = Arrays.asList(object);
        final Class<?> targetClass = typeReference.clazz().getComponentType();
        final JSONArray json = new JSONArray();

        //noinspection unchecked
        list.forEach(value -> json.add(Serializer.serialize(targetClass, value)));
        return json;
    }

    @Override
    public Object[] deserialize(TypeReference typeReference, Collection<?> serialized) {
        final Class<?> targetClass = typeReference.clazz().getComponentType();
        return serialized.stream().map(value -> Serializer.deserialize(targetClass, value)).toArray();
    }

    @Override
    public boolean isCompatibleWith(Class<?> clazz) {
        return clazz.isArray();
    }
}
