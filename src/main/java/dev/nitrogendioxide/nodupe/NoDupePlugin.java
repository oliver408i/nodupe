package dev.nitrogendioxide.nodupe;

import org.bukkit.plugin.java.JavaPlugin;
import dev.nitrogendioxide.nodupe.commands.CheckItemCommand;
import dev.nitrogendioxide.nodupe.commands.NoDupeCommand;
import dev.nitrogendioxide.nodupe.listeners.AnvilEditPrevention;
import dev.nitrogendioxide.nodupe.listeners.InventoryCheckTask;
import dev.nitrogendioxide.nodupe.config.ConfigManager;
import dev.nitrogendioxide.nodupe.utils.DuplicateChecker;

public class NoDupePlugin extends JavaPlugin {
    private static NoDupePlugin instance;
    private ConfigManager configManager;
    private DuplicateChecker duplicateChecker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize configuration manager and duplicate checker
        configManager = new ConfigManager(this);
        duplicateChecker = new DuplicateChecker(configManager);

        // Register commands
        getCommand("checkitem").setExecutor(new CheckItemCommand());
        getCommand("nodupe").setExecutor(new NoDupeCommand());

        // Register event listeners
        getServer().getPluginManager().registerEvents(new AnvilEditPrevention(), this);

        // Schedule the inventory check task with auto-removal if enabled
        new InventoryCheckTask(this, duplicateChecker).runTaskTimer(this, 100L, 100L);

        getLogger().info("NoDupe Plugin Enabled!");
    }

    public static NoDupePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DuplicateChecker getDuplicateChecker() {
        return duplicateChecker;
    }
}
