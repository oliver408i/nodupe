package dev.nitrogendioxide.nodupe;

import org.bukkit.plugin.java.JavaPlugin;
import dev.nitrogendioxide.nodupe.commands.CheckItemCommand;
import dev.nitrogendioxide.nodupe.listeners.AnvilEditPrevention;

public class NoDupePlugin extends JavaPlugin {
    private static NoDupePlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (getConfig().getBoolean("enable-simdupe", false)) {
            this.getCommand("simdupe").setExecutor(new SimDupeCommand());
        }

        this.getCommand("checkitem").setExecutor(new CheckItemCommand());
        this.getCommand("nodupe").setExecutor(new NoDupeCommand());

        // Register new event listener
        getServer().getPluginManager().registerEvents(new AnvilEditPrevention(), this);
        getLogger().info("NoDupe Plugin Enabled!");
    }

    public static NoDupePlugin getInstance() {
        return instance;
    }
}
