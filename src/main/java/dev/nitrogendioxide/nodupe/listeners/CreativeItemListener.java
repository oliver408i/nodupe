package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class CreativeItemListener implements Listener {

    @EventHandler
    public void onCreativePickup(PlayerPickupItemEvent event) {
        if (!event.getPlayer().getGameMode().toString().equals("CREATIVE")) return;

        ItemStack item = event.getItem().getItemStack();
        assignUniqueID(item);
    }

    @EventHandler
    public void onCreativeInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getGameMode().toString().equals("CREATIVE")) {
            ItemStack item = event.getCurrentItem();
            if (item != null) {
                assignUniqueID(item);
            }
        }
    }

    private void assignUniqueID(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setLore(java.util.Collections.singletonList("§7Unique ID: " + UUID.randomUUID()));
        item.setItemMeta(meta);
    }
}
