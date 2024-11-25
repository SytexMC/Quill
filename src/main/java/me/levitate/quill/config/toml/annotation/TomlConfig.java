package me.levitate.quill.config.toml.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TomlConfig {
    /**
     * The name of the configuration file
     * @return The file name
     */
    String value();

    /**
     * Whether to automatically update the configuration when new fields are added
     * @return True to auto update
     */
    boolean autoUpdate() default true;

    /**
     * The configuration version, used for migrations
     * @return The version number
     */
    int version() default 1;
}