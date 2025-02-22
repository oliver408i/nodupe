package dev.nitrogendioxide.nodupe;

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

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /nodupe reload");
            return true;
        }

        // Reload the plugin configuration
        NoDupePlugin.getInstance().reloadConfig();
        ItemUtils.reloadConfig();

        sender.sendMessage(ChatColor.GREEN + "NoDupePlugin configuration reloaded.");

        return true;
    }
}