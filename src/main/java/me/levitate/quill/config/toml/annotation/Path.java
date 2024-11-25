package me.levitate.quill.config.toml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Path {
    /**
     * The custom path in the configuration
     * @return The path
     */
    String value();
}