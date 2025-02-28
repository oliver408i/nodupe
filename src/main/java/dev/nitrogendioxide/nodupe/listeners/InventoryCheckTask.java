package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import dev.nitrogendioxide.nodupe.NoDupePlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class InventoryCheckTask extends BukkitRunnable {

    private final NoDupePlugin plugin;

    public InventoryCheckTask(NoDupePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        int playerCount = Bukkit.getOnlinePlayers().size();

        int minInterval = plugin.getConfig().getInt("min-inventory-check-interval", 30);
        int maxInterval = plugin.getConfig().getInt("max-inventory-check-interval", 120);
        boolean scanContainers = plugin.getConfig().getBoolean("scan-containers", true);
        boolean enableAlerts = plugin.getConfig().getBoolean("enable-duplicate-alerts", true);
        boolean logAlerts = plugin.getConfig().getBoolean("log-duplicate-alerts", true);

        // Adjust scan interval dynamically
        int interval = Math.min(maxInterval, Math.max(minInterval, maxInterval - (playerCount * 4)));
        new InventoryCheckTask(plugin).runTaskLater(plugin, interval * 20L);

        // Store duplicate item data
        Map<String, List<String>> duplicateMap = new HashMap<>();

        // Scan players' inventories and ender chests
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndAssignID(player.getInventory().getContents());
            checkAndAssignID(player.getEnderChest().getContents());

            checkForDuplicates(player.getName(), player.getInventory().getContents(), "Inventory", duplicateMap);
            checkForDuplicates(player.getName(), player.getEnderChest().getContents(), "EnderChest", duplicateMap);
        }

        // Scan chests, barrels, shulkers, and bundles
        if (scanContainers) {
            scanLoadedChunksForContainers(duplicateMap);
        }

        // Alert staff and log duplicates
        if (!duplicateMap.isEmpty() && enableAlerts) {
            alertStaff(duplicateMap, logAlerts);
        }
    }

    private void checkForDuplicates(String owner, ItemStack[] contents, String location, Map<String, List<String>> duplicateMap) {
        Map<String, ItemStack> idMap = new HashMap<>();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            String id = getItemID(item);
            if (id == null) continue; // Item has no ID (shouldn't happen due to checkAndAssignID)

            if (idMap.containsKey(id)) {
                duplicateMap.computeIfAbsent(id, k -> new ArrayList<>())
                            .add(location + " - " + owner + " (" + item.getType() + ")");
            } else {
                idMap.put(id, item);
            }

            // Check bundles for duplicate items
            if (item.getType() == Material.BUNDLE) {
                BundleMeta bundleMeta = (BundleMeta) item.getItemMeta();
                if (bundleMeta != null && bundleMeta.hasItems()) {
                    checkForDuplicates(owner, bundleMeta.getItems().toArray(new ItemStack[0]), location + " (Bundle)", duplicateMap);
                }
            }
        }
    }

    private void checkAndAssignID(ItemStack[] items) {
        if (items == null) return;

        for (ItemStack item : items) {
            if (item == null || !item.getType().isItem()) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String idLinePrefix = ChatColor.BLACK + "" + ChatColor.ITALIC + "ID: ";

            // Check if an ID already exists
            String existingID = null;
            for (String line : lore) {
                if (line.startsWith(idLinePrefix)) {
                    existingID = line;
                    break;
                }
            }

            // If no ID exists, generate a new one
            if (existingID == null) {
                existingID = idLinePrefix + UUID.randomUUID();
            } else {
                lore.remove(existingID); // Remove existing ID to reposition it
            }

            // Ensure ID is always the last line
            lore.add(existingID);

            meta.setLore(lore);
            item.setItemMeta(meta);

            // Check for items inside bundles
            if (item.getType() == Material.BUNDLE) {
                BundleMeta bundleMeta = (BundleMeta) item.getItemMeta();
                if (bundleMeta != null && bundleMeta.hasItems()) {
                    checkAndAssignID(bundleMeta.getItems().toArray(new ItemStack[0]));
                }
            }
        }
    }

    private void scanLoadedChunksForContainers(Map<String, List<String>> duplicateMap) {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (tile instanceof Container) {
                        Container container = (Container) tile;
                        String location = container.getBlock().getLocation().toVector().toString();
                        checkAndAssignID(container.getInventory().getContents());
                        checkForDuplicates("Container at " + location, container.getInventory().getContents(), "Container", duplicateMap);
                    }
                }
            }
        }
    }

    private void alertStaff(Map<String, List<String>> duplicateMap, boolean logToFile) {
        StringBuilder alertMessage = new StringBuilder(ChatColor.RED + "⚠ Duplicate Items Found! ⚠\n");
        StringBuilder logEntry = new StringBuilder();
        String timestamp = Instant.now().toString();

        logEntry.append("[").append(timestamp).append("] Duplicate Items Detected:\n");

        for (Map.Entry<String, List<String>> entry : duplicateMap.entrySet()) {
            String id = entry.getKey();
            List<String> locations = entry.getValue();
            String itemType = locations.get(0).split("\\(")[1].replace(")", "").trim();

            alertMessage.append(ChatColor.YELLOW).append("Item: ").append(ChatColor.WHITE).append(itemType)
                        .append(ChatColor.YELLOW).append(" | ID: ").append(ChatColor.WHITE).append(id).append("\n");

            for (String location : locations) {
                alertMessage.append(ChatColor.GRAY).append(" - ").append(location).append("\n");
                logEntry.append(" - ").append(location).append("\n");
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("nodupe.alerts")) {
                player.sendMessage(alertMessage.toString());
            }
        }

        if (logToFile) {
            logToFile(logEntry.toString());
        }
    }

    private String getItemID(ItemStack item) {
        if (item == null || !item.getType().isItem()) return null;
    
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
    
        String idLinePrefix = ChatColor.BLACK + "" + ChatColor.ITALIC + "ID: ";
    
        for (String line : meta.getLore()) {
            if (line.startsWith(idLinePrefix)) {
                return line.substring(idLinePrefix.length());
            }
        }
    
        return null;
    }    

    private void logToFile(String logEntry) {
        File logFile = new File(plugin.getDataFolder(), "dupe-alerts.log");

        try (FileWriter writer = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(logEntry);
            bw.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to dupe-alerts.log: " + e.getMessage());
        }
    }
}
