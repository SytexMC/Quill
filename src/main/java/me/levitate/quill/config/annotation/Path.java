package me.levitate.quill.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Path annotation to specify a custom path for a field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Path {
    /**
     * The custom path in the configuration
     * @return The path
     */
    String value();
}