package dev.nitrogendioxide.nodupe;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class NoDupePlugin extends JavaPlugin {

    private static NoDupePlugin instance;

    @Override
    public void onEnable() {
        
        instance = this;

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Set logger level based on config
        if (getConfig().getBoolean("debug", false)) {
            getLogger().setLevel(Level.FINE);
        } else {
            getLogger().setLevel(Level.INFO);
        }

        if (getConfig().getBoolean("enable-simdupe", false)) {
            this.getCommand("simdupe").setExecutor(new SimDupeCommand());
        }

        this.getCommand("checkitem").setExecutor(new CheckItemCommand());
        this.getCommand("nodupe").setExecutor(new NoDupeCommand());


        // Register the inventory listener
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getLogger().info("NoDupePlugin has been enabled.");
    }

    public static NoDupePlugin getInstance() {
        return instance;
    }
}