package dev.nitrogendioxide.nodupe.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;

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

        // Compare unique ID from lore
        String mainID = getItemID(mainHandItem);
        String offID = getItemID(offHandItem);

        if (mainID == null || offID == null) {
            player.sendMessage(ChatColor.RED + "One or both items are missing a unique identifier.");
            return true;
        }

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
        if (lore == null) return null;

        return lore.stream()
                .filter(line -> line.startsWith(ChatColor.DARK_GRAY + "ID: "))
                .map(line -> line.replace(ChatColor.DARK_GRAY + "ID: ", ""))
                .findFirst()
                .orElse(null);
    }
}
