package dev.nitrogendioxide.nodupe;

import org.bukkit.plugin.java.JavaPlugin;
import dev.nitrogendioxide.nodupe.commands.CheckItemCommand;
import dev.nitrogendioxide.nodupe.commands.NoDupeCommand;
import dev.nitrogendioxide.nodupe.listeners.AnvilEditPrevention;
import dev.nitrogendioxide.nodupe.listeners.CreativeItemListener;
import dev.nitrogendioxide.nodupe.listeners.SurvivalItemListener;

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

        getServer().getPluginManager().registerEvents(new AnvilEditPrevention(), this);
        getServer().getPluginManager().registerEvents(new CreativeItemListener(), this);
        getServer().getPluginManager().registerEvents(new SurvivalItemListener(), this);

        getLogger().info("NoDupe Plugin Enabled!");
}

}
