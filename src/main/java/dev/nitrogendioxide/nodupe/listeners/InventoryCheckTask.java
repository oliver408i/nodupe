package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.nitrogendioxide.nodupe.NoDupePlugin;

public class InventoryCheckTask extends BukkitRunnable {

    private final NoDupePlugin plugin;

    public InventoryCheckTask(NoDupePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                    // Check if the last line contains an ID
                    boolean hasID = lore.size() >= 2 && lore.get(lore.size() - 1).startsWith(ChatColor.GRAY.toString());

                    if (!hasID) {
                        // Ensure proper spacing: add a blank line if there's existing lore
                        if (!lore.isEmpty()) {
                            lore.add(""); // Add a blank line
                        }

                        // Add the new unique ID
                        lore.add(ChatColor.GRAY.toString() + ChatColor.ITALIC + "ID: " + UUID.randomUUID());

                        // Update the item's meta
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }
}
