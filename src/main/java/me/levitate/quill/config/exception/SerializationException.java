package me.levitate.quill.config.exception;

public class SerializationException extends ConfigurationException {
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}