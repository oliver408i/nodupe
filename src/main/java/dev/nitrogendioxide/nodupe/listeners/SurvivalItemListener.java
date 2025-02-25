package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.UUID;

public class SurvivalItemListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack drop = new ItemStack(event.getBlock().getType());
        assignUniqueID(drop);
        event.getBlock().getDrops().clear(); 
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
    }

    private void assignUniqueID(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setLore(Collections.singletonList("§7Unique ID: " + UUID.randomUUID()));
        item.setItemMeta(meta);
    }
}
