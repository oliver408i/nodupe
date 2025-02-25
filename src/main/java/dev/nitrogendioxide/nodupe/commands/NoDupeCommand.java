package dev.nitrogendioxide.nodupe.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NoDupeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "===== NoDupe Commands =====");
        sender.sendMessage(ChatColor.YELLOW + "/nodupe" + ChatColor.GRAY + " - Shows this help menu.");
        sender.sendMessage(ChatColor.YELLOW + "/checkitem" + ChatColor.GRAY + " - Compares the two held items.");
        sender.sendMessage(ChatColor.YELLOW + "/simdupe" + ChatColor.GRAY + " - (If enabled) Simulates a dupe.");
        sender.sendMessage(ChatColor.GREEN + "===========================");
        return true;
    }
}
