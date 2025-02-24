package dev.nitrogendioxide.nodupe.listeners;

import dev.nitrogendioxide.nodupe.ItemTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

public class AnvilEditPrevention implements Listener {
    @EventHandler
    public void onAnvilUse(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result != null && ItemTracker.hasUniqueID(result)) {
            ItemTracker.assignUniqueID(result);
        }
    }
}
