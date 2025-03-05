package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import dev.nitrogendioxide.nodupe.utils.DuplicateChecker;

import org.bukkit.entity.Player;

public class InventoryListener implements Listener {
    private final DuplicateChecker duplicateChecker;

    public InventoryListener(DuplicateChecker duplicateChecker) {
        this.duplicateChecker = duplicateChecker;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            duplicateChecker.checkInventory(player);
        }
    }
}
