package dev.nitrogendioxide.nodupe.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public class ConfigManager {
    private final JavaPlugin plugin;
    private boolean removeDuplicates;
    private boolean logDuplicateAlerts;
    private boolean debug;
    private List<String> highVulnerabilityItems;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        removeDuplicates = config.getBoolean("remove-duplicates", false);
        logDuplicateAlerts = config.getBoolean("log-duplicate-alerts", true);
        debug = config.getBoolean("debug", false);
        highVulnerabilityItems = config.getStringList("high-vulnerability-items");
    }

    public boolean shouldRemoveDuplicates() {
        return removeDuplicates;
    }

    public boolean shouldLogDuplicateAlerts() {
        return logDuplicateAlerts;
    }

    public boolean isDebugMode() {
        return debug;
    }

    public List<String> getHighVulnerabilityItems() {
        return highVulnerabilityItems;
    }
}
