package dev.nitrogendioxide.nodupe.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NoDupeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nodupe.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "-----[ NoDupe Help Menu ]-----");
        sender.sendMessage(ChatColor.YELLOW + "/nodupe - " + ChatColor.WHITE + "Shows this help menu.");
        sender.sendMessage(ChatColor.YELLOW + "/checkitem - " + ChatColor.WHITE + "Compare two items' unique IDs.");

        sender.sendMessage(ChatColor.AQUA + "Permissions:");
        sender.sendMessage(ChatColor.GRAY + " - nodupe.admin (Access to all NoDupe commands)");
        sender.sendMessage(ChatColor.GRAY + " - nodupe.checkitem (Use /checkitem)");
        sender.sendMessage(ChatColor.GRAY + " - nodupe.alerts (Recieve duplicate items alert)");

        return true;
    }
}
