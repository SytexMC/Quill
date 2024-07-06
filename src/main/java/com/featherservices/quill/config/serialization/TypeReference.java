package com.featherservices.quill.config.serialization;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;

@Getter
@Accessors(fluent = true)
public class TypeReference {

    private final @NonNull Class<?> clazz;
    private final Field field;

    public TypeReference(Class<?> clazz) {
        this.clazz = clazz;
        this.field = null;
    }

    public TypeReference(Field field) {
        this.clazz = field.getType();
        this.field = field;
    }

    public TypeReference(Object object) {
        this.clazz = object.getClass();
        this.field = null;
    }

}
