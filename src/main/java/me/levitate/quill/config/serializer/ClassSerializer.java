package me.levitate.quill.config.serializer;

/**
 * TypeSerializer implementation for Class objects
 */
public class ClassSerializer implements TypeSerializer<Class> {
    @Override
    public Object serialize(Class value) {
        return value != null ? value.getName() : null;
    }

    @Override
    public Class deserialize(Object value) {
        if (value == null) return null;
        try {
            return Class.forName(String.valueOf(value));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to load class: " + value, e);
        }
    }
}

