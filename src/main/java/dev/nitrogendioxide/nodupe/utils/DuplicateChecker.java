package dev.nitrogendioxide.nodupe.utils;

import dev.nitrogendioxide.nodupe.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DuplicateChecker {
    private final ConfigManager configManager;

    public DuplicateChecker(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void checkInventory(Player player) {
        Map<String, Integer> itemCounts = new HashMap<>();
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            String itemKey = item.getType().name();
            
            if (configManager.getHighVulnerabilityItems().contains(itemKey)) {
                itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if (entry.getValue() > 1) {
                if (configManager.shouldRemoveDuplicates()) {
                    removeDuplicateItems(player, entry.getKey());
                }
                if (configManager.shouldLogDuplicateAlerts()) {
                    Logger.log("Duplicate detected: " + player.getName() + " had multiple " + entry.getKey());
                }
            }
        }
    }

    private void removeDuplicateItems(Player player, String materialName) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean removed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            if (item.getType().name().equals(materialName)) {
                player.getInventory().setItem(i, null);
                removed = true;
            }
        }

        if (removed) {
            Logger.log("Removed duplicate item: " + materialName + " from " + player.getName());
            player.sendMessage("§cDuplicate item detected and removed: " + materialName);
        }
    }
}
