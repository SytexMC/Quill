package me.levitate.quill.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration annotation to mark a class as a configuration holder
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configuration {
    /**
     * The name of the configuration file
     * @return The file name
     */
    String value() default "config.yml";
    
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