package me.levitate.quill.logger;

import lombok.Setter;
import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import me.levitate.quill.injection.annotation.PostConstruct;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

@Module
public class QuillLogger {
    @Inject
    private Plugin plugin;
    private Logger logger;

    @Setter
    private boolean debugLog = false;

    @PostConstruct
    private void init() {
        logger = plugin.getLogger();
    }

    /**
     * Logs a message
     * @param level The severity level
     * @param message The message
     * @param exception The exception to log
     */
    public void log(Level level, String message, Exception exception) {
        logger.log(level, message, exception);
    }

    /**
     * Sends a normal log message, this will only run if debug logging is enabled.
     * @param message The message
     */
    public void info(String message) {
        if (debugLog)
            logger.info(message);
    }

    /**
     * Sends a warning message
     * @param message The message
     */
    public void warn(String message) {
        logger.warning(message);
    }

    /**
     * Sends an error message
     * @param message The error message
     */
    public void error(String message) {
        logger.severe(message);
    }
}
