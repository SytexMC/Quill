package me.levitate.quill.config.annotation;

import com.fasterxml.jackson.annotation.JsonFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JsonFilter("commentFilter")
public @interface Config {
    String value();
}