package me.levitate.quill.config.toml.exception;

public class ConfigValidationException extends ConfigurationException {
    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}