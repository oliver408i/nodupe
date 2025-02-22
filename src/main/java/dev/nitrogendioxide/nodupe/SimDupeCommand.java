package dev.nitrogendioxide.nodupe;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SimDupeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("nodupe.simdupe")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage(ChatColor.YELLOW + "You are not holding any item to duplicate.");
            return true;
        }

        // Make sure the player's inventory has space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Your inventory is full.");
            return true;
        }

        // Create a copy of the item, including its NBT data
        ItemStack duplicatedItem = itemInHand.clone();

        // Simulate the duplication without modifying the unique ID
        // Note: We intentionally do not assign a new unique ID here to test the detection

        // Add the duplicated item to the player's inventory
        player.getInventory().addItem(duplicatedItem);

        player.sendMessage(ChatColor.GREEN + "Duplicated the item in your hand. Check your inventory.");

        // After adding the item, we can manually trigger the duplicate check
        // Alternatively, rely on existing event handlers (e.g., inventory interactions)

        return true;
    }
}