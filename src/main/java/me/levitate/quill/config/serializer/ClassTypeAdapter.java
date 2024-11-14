package me.levitate.quill.config.serializer;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * GSON TypeAdapter for Class objects
 */
public class ClassTypeAdapter extends TypeAdapter<Class> {
    @Override
    public void write(JsonWriter out, Class value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.getName());
    }

    @Override
    public Class read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String className = in.nextString();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load class: " + className, e);
        }
    }
}