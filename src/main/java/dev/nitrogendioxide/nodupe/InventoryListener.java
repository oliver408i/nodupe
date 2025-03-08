package dev.nitrogendioxide.nodupe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta; // For older versions

public class InventoryListener implements Listener {

    private static final NoDupeLogger logger = new NoDupeLogger(
        NoDupePlugin.getInstance().getLogger(),
        NoDupePlugin.getInstance().getConfig()
    );
    private final Set<String> blacklistedItems = new HashSet<>(NoDupePlugin.getInstance().getConfig().getStringList("blacklisted-items"));
    private final Map<String, Object> blacklistedMetadata = NoDupePlugin.getInstance().getConfig().getConfigurationSection("blacklisted-metadata").getValues(false);
    private static final File dataFolder = NoDupePlugin.getInstance().getDataFolder();
    private static final File violationLogFile = new File(dataFolder, "violations.log");

    /*@EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player)) return;
        Player player = (Player) human;

        logger.debug("Player " + player.getName() + " opened inventory of type " + event.getInventory().getType());

        // Remove blacklisted items
        removeBlacklistedItems(player);

        // Assign unique IDs and check for duplicates in player's inventory
        processInventory(player.getInventory(), player, "Player Inventory");

        // Assign unique IDs and check for duplicates in the opened inventory
        processInventory(event.getInventory(), player, "Opened Inventory");
    }*/

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
 
        logger.debug("Player " + player.getName() + " clicked inventory slot " + event.getSlot());
 
        // If the cursor item (item being placed) is blacklisted, remove it
        if (cursorItem != null && isBlacklisted(cursorItem)) {
            event.setCursor(null);
            logger.info("Removed blacklisted item " + cursorItem.getType() + " from player " + player.getName() +
                        " while attempting to move it.");
            logViolation(player.getName(), null, cursorItem.getType().toString(),
                         "Cursor (moving item)", "N/A", "Blacklisted Item Removal");
            player.sendMessage(ChatColor.RED + "Blacklisted items have been removed from your inventory.");
            event.setCancelled(true);
            return;
        }
 
        // Process the clicked item
        processItem(clickedItem, player, "Clicked Item");

        // Schedule duplicate check
        Bukkit.getScheduler().runTaskLater(NoDupePlugin.getInstance(), () -> {
            checkForDuplicatesInInventory(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        // Check if the entity is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Item itemEntity = event.getItem();
        ItemStack itemStack = itemEntity.getItemStack();

        logger.debug("Player " + player.getName() + " is picking up item " + itemStack.getType());

        if (isBlacklisted(itemStack)) {
            event.setCancelled(true);
            itemEntity.remove();
            logger.info("Removed blacklisted item " + itemStack.getType() + " from ground before pickup by " + player.getName());
            logViolation(player.getName(), null, itemStack.getType().toString(),
                         "Ground Item", "N/A", "Blacklisted Item Pickup Prevention");
            if (NoDupePlugin.getInstance().getConfig().getBoolean("notify-player", false))
                player.sendMessage(ChatColor.RED + "Blacklisted items cannot be picked up.");
            return;
        }
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

        logger.debug("Player " + player.getName() + " dropped item " + itemStack.getType());

        // Remove blacklisted items
        removeBlacklistedItems(player);

        // Process the item being dropped
        processItem(itemStack, player, "Dropped Item");

        // Schedule duplicate check
        Bukkit.getScheduler().runTaskLater(NoDupePlugin.getInstance(), () -> {
            checkForDuplicatesInInventory(player);
        }, 1L);
    }

    private void removeBlacklistedItems(Player player) {
        if (blacklistedItems.isEmpty() && blacklistedMetadata.isEmpty()) {
            return;
        }

        ItemStack[] items = player.getInventory().getContents();
        boolean removed = false;

        for (int slot = 0; slot < items.length; slot++) {
            ItemStack invItem = items[slot];
            if (isBlacklisted(invItem)) {
                player.getInventory().setItem(slot, null);
                removed = true;
                logger.info("Removed blacklisted item " + invItem.getType() + " from player " + player.getName() +
                            " at slot " + slot);
                logViolation(player.getName(), null, invItem.getType().toString(),
                             "Player inventory slot " + slot, "N/A", "Blacklisted Item Removal");
            }
        }

        if (removed && NoDupePlugin.getInstance().getConfig().getBoolean("notify-player", false)) {
            player.sendMessage(ChatColor.RED + "Blacklisted items have been removed from your inventory.");
        }
    }

    private void logViolation(String playerName, String id, String itemType, String location1, String location2, String eventType) {
        // Ensure the data folder existses
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Prepare the log entry
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + eventType + " - Player: " + playerName + ", Item: " + itemType + 
                          (id != null ? ", ID: " + id : "") + ", Locations: " + location1 + ", " + location2 + "\n";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(violationLogFile, true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            logger.severe("Failed to write to violation log file: " + e.getMessage());
        }
    }

    private void processInventory(Inventory inventory, Player player, String inventoryName) {
        if (inventory == null) {
            logger.debug("Inventory " + inventoryName + " is null, skipping.");
            return;
        }

        Map<String, String> uniqueIdLocations = new HashMap<>();

        logger.debug("Processing " + inventoryName + " for player " + player.getName());

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (ItemUtils.isHighVulnerabilityItem(item)) {
                String itemName = item.getType().toString();
                logger.debug("Found high-vulnerability item " + itemName + " at slot " + slot + " in " + inventoryName);

                // Assign unique ID if the item doesn't have one
                String id = ItemUtils.getUniqueId(item);
                if (id == null) {
                    logger.debug("Item at slot " + slot + " does not have a unique ID, assigning one.");
                    ItemUtils.assignUniqueId(item);
                    id = ItemUtils.getUniqueId(item);
                    logger.debug("Assigned unique ID " + id + " to item at slot " + slot);
                } else {
                    logger.info("Item at slot " + slot + " already has unique ID " + id);
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
            logger.debug(itemContext + " is null or air, skipping.");
            return;
        }

        if (ItemUtils.isHighVulnerabilityItem(item)) {
            logger.debug("Processing " + itemContext + " (" + item.getType() + ") for player " + player.getName());

            // Assign unique ID if the item doesn't have one
            String id = ItemUtils.getUniqueId(item);
            if (id == null) {
                logger.debug(itemContext + " does not have a unique ID, assigning one.");
                ItemUtils.assignUniqueId(item);
                id = ItemUtils.getUniqueId(item);
                logger.debug("Assigned unique ID " + id + " to " + itemContext);
            } else {
                logger.debug(itemContext + " already has unique ID " + id);
            }
        } else {
            logger.debug(itemContext + " (" + item.getType() + ") is not a high-vulnerability item, skipping.");
        }
    }

    private void checkForDuplicatesInInventory(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(NoDupePlugin.getInstance(), () -> {
            Map<String, String> uniqueIdLocations = new HashMap<>();
            logger.debug("Checking for duplicates asynchronously in inventory of player " + player.getName());

            ItemStack[] items = player.getInventory().getContents();
            for (int slot = 0; slot < items.length; slot++) {
                ItemStack invItem = items[slot];
                if (ItemUtils.isHighVulnerabilityItem(invItem)) {
                    String id = ItemUtils.getUniqueId(invItem);
                    String itemName = invItem.getType().toString();
                    String location = "Player inventory slot " + slot;

                    if (id != null) {
                        if (uniqueIdLocations.containsKey(id)) {
                            String firstLocation = uniqueIdLocations.get(id);
                            Bukkit.getScheduler().runTask(NoDupePlugin.getInstance(), () -> {
                                logger.warning("Duplicate detected in player's inventory: ID " + id + " at " + firstLocation + " and " + location);
                                handleDuplicate(player, id, firstLocation, location, invItem.getType());
                            });
                        } else {
                            uniqueIdLocations.put(id, location);
                        }
                    } else {
                        final int slotIndex = slot; // Create a final copy of slot
                        Bukkit.getScheduler().runTask(NoDupePlugin.getInstance(), () -> {
                            logger.debug("Item at slot " + slotIndex + " does not have a unique ID, assigning one.");
                            ItemUtils.assignUniqueId(invItem);
                        });
                    }
                }
            }
        });
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
        logViolation(player.getName(), id, itemType.toString(), location1, location2, "Duplicate Item Detection");

        // Remove duplicates if enabled in config
        if (NoDupePlugin.getInstance().getConfig().getBoolean("remove-duplicates", false)) {
            removeDuplicateItems(player, id);
        }

        // Remove blacklisted items
        removeBlacklistedItems(player);
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

    private boolean isBlacklisted(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        String itemType = item.getType().toString();
        if (blacklistedItems.contains(itemType)) return true;

        if (blacklistedMetadata.containsKey(itemType)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
                List<String> lore = meta.hasLore() ? meta.getLore() : null;

                Object metadataObj = blacklistedMetadata.get(itemType);
                if (metadataObj instanceof List<?>) {
                    for (String bannedName : (List<String>) metadataObj) {
                        if (displayName.equalsIgnoreCase(bannedName) || (lore != null && lore.contains(bannedName))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
    