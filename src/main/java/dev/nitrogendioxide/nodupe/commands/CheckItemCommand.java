package dev.nitrogendioxide.nodupe.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CheckItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (mainHandItem.getType() == Material.AIR || offHandItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item in both your main hand and offhand to compare!");
            return true;
        }

        // Compare item types
        if (mainHandItem.getType() != offHandItem.getType()) {
            player.sendMessage(ChatColor.RED + "The items are not the same type.");
            return true;
        }

        // Ensure both items have a unique ID
        ensureItemHasID(mainHandItem);
        ensureItemHasID(offHandItem);

        // Retrieve unique IDs after ensuring they exist
        String mainID = getItemID(mainHandItem);
        String offID = getItemID(offHandItem);

        if (!Objects.equals(mainID, offID)) {
            player.sendMessage(ChatColor.RED + "The items are different (unique IDs do not match).");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "The items are identical!");
        return true;
    }

    private String getItemID(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return null;

        // ID should always be the last line, with a blank line above it
        return lore.get(lore.size() - 1).startsWith(ChatColor.GRAY.toString()) ? lore.get(lore.size() - 1) : null;
    }

    private void ensureItemHasID(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // If no ID exists, add a properly formatted one
        boolean hasID = !lore.isEmpty() && lore.get(lore.size() - 1).startsWith(ChatColor.GRAY.toString());

        if (!hasID) {
            if (!lore.isEmpty()) {
                lore.add(""); // Add a blank line if there is existing lore
            }
            lore.add(ChatColor.GRAY.toString() + ChatColor.ITALIC + "ID: " + UUID.randomUUID());

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }
}
