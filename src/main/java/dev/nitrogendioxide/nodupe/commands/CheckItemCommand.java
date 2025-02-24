package dev.nitrogendioxide.nodupe.commands;

import dev.nitrogendioxide.nodupe.ItemTracker;
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

        Player player = (Player) sender;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand == null || offHand == null || mainHand.getType().isAir() || offHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You need two items to compare.");
            return true;
        }

        if (!ItemTracker.hasUniqueID(mainHand) || !ItemTracker.hasUniqueID(offHand)) {
            player.sendMessage(ChatColor.RED + "One or both items lack a unique ID.");
            return true;
        }

        String mainID = ItemTracker.getUniqueID(mainHand);
        String offID = ItemTracker.getUniqueID(offHand);

        if (mainID.equals(offID)) {
            player.sendMessage(ChatColor.GREEN + "These items have the same unique ID.");
        } else {
            player.sendMessage(ChatColor.RED + "These items have different unique IDs.");
        }

        return true;
    }
}
