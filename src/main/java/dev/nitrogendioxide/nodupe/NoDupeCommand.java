package dev.nitrogendioxide.nodupe;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NoDupeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /nodupe <reload|notify>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nodupe.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
                return true;
            }
            // Reload the plugin configuration
            NoDupePlugin.getInstance().reloadConfig();
            ItemUtils.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "NoDupePlugin configuration reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("notify")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can toggle notifications.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("nodupe.notify")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to toggle notifications.");
                return true;
            }

            boolean currentSetting = InventoryListener.adminNotificationPreferences.getOrDefault(player, true);
            InventoryListener.adminNotificationPreferences.put(player, !currentSetting);

            player.sendMessage(ChatColor.YELLOW + "[NoDupe] Notifications " +
                    (!currentSetting ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.YELLOW + ".");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown command. Use /nodupe <reload|notify>.");
        return true;
    }
}