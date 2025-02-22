package dev.nitrogendioxide.nodupe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent; // For older versions
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryListener implements Listener {

    private static final Logger logger = NoDupePlugin.getInstance().getLogger();
    private static final File dataFolder = NoDupePlugin.getInstance().getDataFolder();
    private static final File violationLogFile = new File(dataFolder, "violations.log");

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player)) return;
        Player player = (Player) human;

        logger.fine("Player " + player.getName() + " opened inventory of type " + event.getInventory().getType());

        // Assign unique IDs and check for duplicates in player's inventory
        processInventory(player.getInventory(), player, "Player Inventory");

        // Assign unique IDs and check for duplicates in the opened inventory
        processInventory(event.getInventory(), player, "Opened Inventory");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        Player player = (Player) event.getWhoClicked();

        logger.fine("Player " + player.getName() + " clicked inventory slot " + event.getSlot());

        // Process the item being clicked
        processItem(clickedItem, player, "Clicked Item");

        // Process the item on the cursor
        processItem(cursorItem, player, "Cursor Item");
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        // Check if the entity is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Item itemEntity = event.getItem();
        ItemStack itemStack = itemEntity.getItemStack();

        logger.fine("Player " + player.getName() + " is picking up item " + itemStack.getType());

        // Process the item being picked up
        processItem(itemStack, player, "Picked Up Item");

        // Schedule duplicate check
        Bukkit.getScheduler().runTaskLater(NoDupePlugin.getInstance(), () -> {
            checkForDuplicatesInInventory(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item itemEntity = event.getItemDrop();
        ItemStack itemStack = itemEntity.getItemStack();

        logger.fine("Player " + player.getName() + " dropped item " + itemStack.getType());

        // Process the item being dropped
        processItem(itemStack, player, "Dropped Item");

        // Schedule duplicate check
        Bukkit.getScheduler().runTaskLater(NoDupePlugin.getInstance(), () -> {
            checkForDuplicatesInInventory(player);
        }, 1L);
    }

    private void processInventory(Inventory inventory, Player player, String inventoryName) {
        if (inventory == null) {
            logger.fine("Inventory " + inventoryName + " is null, skipping.");
            return;
        }

        Map<String, String> uniqueIdLocations = new HashMap<>();

        logger.fine("Processing " + inventoryName + " for player " + player.getName());

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (ItemUtils.isHighVulnerabilityItem(item)) {
                String itemName = item.getType().toString();
                logger.fine("Found high-vulnerability item " + itemName + " at slot " + slot + " in " + inventoryName);

                // Assign unique ID if the item doesn't have one
                String id = ItemUtils.getUniqueId(item);
                if (id == null) {
                    logger.fine("Item at slot " + slot + " does not have a unique ID, assigning one.");
                    ItemUtils.assignUniqueId(item);
                    id = ItemUtils.getUniqueId(item);
                    logger.fine("Assigned unique ID " + id + " to item at slot " + slot);
                } else {
                    logger.fine("Item at slot " + slot + " already has unique ID " + id);
                }

                String location = inventoryName + " slot " + slot;

                if (uniqueIdLocations.containsKey(id)) {
                    // Duplicate detected
                    String firstLocation = uniqueIdLocations.get(id);
                    logger.warning("Duplicate detected: ID " + id + " at " + firstLocation + " and " + location);
                    handleDuplicate(player, id, firstLocation, location, item.getType());
                } else {
                    uniqueIdLocations.put(id, location);
                }
            }
        }
    }

    private void processItem(ItemStack item, Player player, String itemContext) {
        if (item == null || item.getType().isAir()) {
            logger.fine(itemContext + " is null or air, skipping.");
            return;
        }

        if (ItemUtils.isHighVulnerabilityItem(item)) {
            logger.fine("Processing " + itemContext + " (" + item.getType() + ") for player " + player.getName());

            // Assign unique ID if the item doesn't have one
            String id = ItemUtils.getUniqueId(item);
            if (id == null) {
                logger.fine(itemContext + " does not have a unique ID, assigning one.");
                ItemUtils.assignUniqueId(item);
                id = ItemUtils.getUniqueId(item);
                logger.fine("Assigned unique ID " + id + " to " + itemContext);
            } else {
                logger.fine(itemContext + " already has unique ID " + id);
            }
        } else {
            logger.fine(itemContext + " (" + item.getType() + ") is not a high-vulnerability item, skipping.");
        }
    }

    private void checkForDuplicatesInInventory(Player player) {
        Map<String, String> uniqueIdLocations = new HashMap<>();

        logger.fine("Checking for duplicates in inventory of player " + player.getName());

        // Check all items in the player's inventory
        ItemStack[] items = player.getInventory().getContents();

        for (int slot = 0; slot < items.length; slot++) {
            ItemStack invItem = items[slot];
            if (ItemUtils.isHighVulnerabilityItem(invItem)) {
                String id = ItemUtils.getUniqueId(invItem);
                String itemName = invItem.getType().toString();
                String location = "Player inventory slot " + slot;

                if (id != null) {
                    logger.fine("Found high-vulnerability item " + itemName + " with ID " + id + " at slot " + slot);

                    if (uniqueIdLocations.containsKey(id)) {
                        // Duplicate detected
                        String firstLocation = uniqueIdLocations.get(id);
                        logger.warning("Duplicate detected in player's inventory: ID " + id + " at " + firstLocation + " and " + location);
                        handleDuplicate(player, id, firstLocation, location, invItem.getType());
                    } else {
                        uniqueIdLocations.put(id, location);
                    }
                } else {
                    // Assign a unique ID if somehow the item doesn't have one
                    logger.fine("Item at slot " + slot + " does not have a unique ID, assigning one.");
                    ItemUtils.assignUniqueId(invItem);
                    id = ItemUtils.getUniqueId(invItem);
                    logger.fine("Assigned unique ID " + id + " to item at slot " + slot);
                }
            }
        }
    }

    private void handleDuplicate(Player player, String id, String location1, String location2, Material itemType) {
        String message = "Duplicate item detected for player " + player.getName() +
                " with ID: " + id + " (" + itemType.toString() + ") at " + location1 + " and " + location2;
        logger.warning(message);

        // Notify online administrators
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("nodupe.notify")) {
                onlinePlayer.sendMessage(ChatColor.YELLOW + "[NoDupe] " + ChatColor.RED + message);
            }
        }

        // Notify the player
        if (NoDupePlugin.getInstance().getConfig().getBoolean("notify-player", false))
        player.sendMessage(ChatColor.RED + "Warning: Duplicate items detected. This incident has been reported.");

        // Write to violation log
        logViolation(player.getName(), id, itemType.toString(), location1, location2);

        // Remove duplicates if enabled in config
        if (NoDupePlugin.getInstance().getConfig().getBoolean("remove-duplicates", false)) {
            removeDuplicateItems(player, id);
        }
    }

    private void logViolation(String playerName, String id, String itemType, String location1, String location2) {
        // Ensure the data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Prepare the log entry
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + "Player: " + playerName + ", Item: " + itemType + ", ID: " + id +
                ", Locations: " + location1 + ", " + location2 + "\n";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(violationLogFile, true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            logger.severe("Failed to write to violation log file: " + e.getMessage());
        }
    }

    private void removeDuplicateItems(Player player, String id) {
        ItemStack[] items = player.getInventory().getContents();

        boolean removed = false;

        for (int slot = 0; slot < items.length; slot++) {
            ItemStack invItem = items[slot];
            if (ItemUtils.isHighVulnerabilityItem(invItem)) {
                String itemId = ItemUtils.getUniqueId(invItem);
                if (id.equals(itemId)) {
                    // Remove the item
                    player.getInventory().setItem(slot, null);
                    removed = true;
                    logger.info("Removed duplicate item with ID " + id + " from player " + player.getName() + " at slot " + slot);
                }
            }
        }

        if (removed && NoDupePlugin.getInstance().getConfig().getBoolean("notify-player", false)) {
            player.sendMessage(ChatColor.RED + "Duplicate items have been removed from your inventory.");
        }
    }
}