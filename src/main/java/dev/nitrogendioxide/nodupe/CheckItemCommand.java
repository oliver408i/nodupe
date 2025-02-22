package dev.nitrogendioxide.nodupe;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CheckItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("nodupe.checkitem")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            sender.sendMessage(ChatColor.YELLOW + "You are not holding any item.");
            return true;
        }

        boolean isVulnerable = ItemUtils.isHighVulnerabilityItem(item);
        String id = ItemUtils.getUniqueId(item);

        sender.sendMessage(ChatColor.GOLD + "Item: " + ChatColor.RESET + item.getType().toString());
        sender.sendMessage(ChatColor.GOLD + "High Vulnerability: " + ChatColor.RESET + isVulnerable);
        if (id != null) {
            sender.sendMessage(ChatColor.GOLD + "Unique ID: " + ChatColor.RESET + id);
        } else {
            if (isVulnerable) {
                // Assign an ID if the item is vulnerable and doesn't have one
                ItemUtils.assignUniqueId(item);
                id = ItemUtils.getUniqueId(item);
                sender.sendMessage(ChatColor.GREEN + "Assigned new unique ID: " + ChatColor.RESET + id);
            } else {
                sender.sendMessage(ChatColor.YELLOW + "This item does not have a unique ID.");
            }
        }

        return true;
    }
}