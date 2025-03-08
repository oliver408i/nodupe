package dev.nitrogendioxide.nodupe;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

public class NoDupeLogger {
    private final Logger logger;
    private final FileConfiguration config;

    public NoDupeLogger(Logger logger, FileConfiguration config) {
        this.logger = logger;
        this.config = config;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warning(String message) {
        logger.warning(message);
    }

    public void severe(String message) {
        logger.severe(message);
    }

    public void debug(String message) {
        if (config.getBoolean("debug", false)) { // Only log debug messages if debug mode is enabled
            logger.log(Level.INFO, "[DEBUG] {0}", message);  // Use INFO level since Paper filters out FINE logs
        }
    }
}