package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import dev.nitrogendioxide.nodupe.NoDupePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InventoryCheckTask extends BukkitRunnable {

    private final NoDupePlugin plugin;

    public InventoryCheckTask(NoDupePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || !item.getType().isItem()) continue;

                ItemMeta meta = item.getItemMeta();
                if (meta == null) continue;

                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                boolean hasID = lore.stream().anyMatch(line -> line.startsWith(ChatColor.GRAY + "" + ChatColor.ITALIC + "ID: "));

                if (!hasID) {
                    // Ensure a blank line before the ID
                    if (!lore.isEmpty()) lore.add("");

                    lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "ID: " + UUID.randomUUID());
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
        }
    }
}
