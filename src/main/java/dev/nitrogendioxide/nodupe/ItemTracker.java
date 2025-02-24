package dev.nitrogendioxide.nodupe;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemTracker {
    private static final String ID_PREFIX = ChatColor.GRAY + "" + ChatColor.ITALIC + "ID: ";

    public static void assignUniqueID(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        for (String line : lore) {
            if (line.startsWith(ID_PREFIX)) return;
        }

        if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
            lore.add("");
        }

        lore.add(ID_PREFIX + UUID.randomUUID().toString().substring(0, 8));

        meta.setLore(lore);
        item.setItemMeta(meta);
    }


    public static boolean hasUniqueID(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        return meta.getLore().stream().anyMatch(line -> line.startsWith(ID_PREFIX));
    }

    public static String getUniqueID(ItemStack item) {
        if (!hasUniqueID(item)) return null;

        return item.getItemMeta().getLore().stream()
                .filter(line -> line.startsWith(ID_PREFIX))
                .map(line -> line.replace(ID_PREFIX, ""))
                .findFirst().orElse(null);
    }
}
