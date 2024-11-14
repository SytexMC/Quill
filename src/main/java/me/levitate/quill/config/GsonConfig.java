package me.levitate.quill.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.levitate.quill.config.serializer.ClassTypeAdapter;

/**
 * Utility class for configuring GSON instances with common settings
 */
public class GsonConfig {
    /**
     * Creates a configured GSON instance with all necessary type adapters and settings
     */
    public static Gson createConfiguredGson() {
        return new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass().getName().startsWith("java.util");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .registerTypeAdapter(Class.class, new ClassTypeAdapter().nullSafe())
            .create();
    }
}