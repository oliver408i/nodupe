package dev.nitrogendioxide.nodupe.utils;

import org.bukkit.Bukkit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    private static final File logFile = new File("plugins/NoDupe/dupe-alerts.log");

    public static void log(String message) {
        Bukkit.getLogger().info("[NoDupe] " + message);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(message + "\n");
        } catch (IOException e) {
            Bukkit.getLogger().warning("[NoDupe] Failed to write to log file.");
        }
    }
}
