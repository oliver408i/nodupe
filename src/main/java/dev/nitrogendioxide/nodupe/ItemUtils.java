package dev.nitrogendioxide.nodupe;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {
    private static final NamespacedKey UNIQUE_ID_KEY = new NamespacedKey(NoDupePlugin.getInstance(), "unique_id");

    // Cache the list of high-vulnerability items
    private static List<String> highVulnerabilityItems = NoDupePlugin.getInstance().getConfig().getStringList("high-vulnerability-items");

    public static void reloadConfig() {
        // Reload the configuration in case it has changed
        highVulnerabilityItems = NoDupePlugin.getInstance().getConfig().getStringList("high-vulnerability-items");
    }

    public static void assignUniqueId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        // Always get the ItemMeta, creating it if necessary
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                // Can't assign meta to this item type
                return;
            }
        }

        // Assign unique ID if it doesn't already have one
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(UNIQUE_ID_KEY, PersistentDataType.STRING)) {
            // Assign a new unique ID
            String uuid = UUID.randomUUID().toString();
            data.set(UNIQUE_ID_KEY, PersistentDataType.STRING, uuid);
            // Set the meta back onto the item
            item.setItemMeta(meta);
        }
    }

    public static String getUniqueId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.get(UNIQUE_ID_KEY, PersistentDataType.STRING);
    }

    public static boolean isHighVulnerabilityItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();
        return highVulnerabilityItems.contains(type.name());
    }
}