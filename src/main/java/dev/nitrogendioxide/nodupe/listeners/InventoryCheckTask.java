package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import dev.nitrogendioxide.nodupe.NoDupePlugin;

import java.io.*;
import java.time.Instant;
import java.util.*;

public class InventoryCheckTask extends BukkitRunnable {

    private final NoDupePlugin plugin;
    private final NamespacedKey idKey;
    private final Map<Location, String> containerIDMap = new HashMap<>(); // Store container IDs

    public InventoryCheckTask(NoDupePlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "item_id");
    }

    @Override
    public void run() {
        int playerCount = Bukkit.getOnlinePlayers().size();

        int minInterval = plugin.getConfig().getInt("min-inventory-check-interval", 30);
        int maxInterval = plugin.getConfig().getInt("max-inventory-check-interval", 120);
        boolean scanContainers = plugin.getConfig().getBoolean("scan-containers", true);
        boolean enableAlerts = plugin.getConfig().getBoolean("enable-duplicate-alerts", true);
        boolean logAlerts = plugin.getConfig().getBoolean("log-duplicate-alerts", true);

        int interval = Math.min(maxInterval, Math.max(minInterval, maxInterval - (playerCount * 4)));
        new InventoryCheckTask(plugin).runTaskLater(plugin, interval * 20L);

        Map<String, List<String>> duplicateMap = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndAssignID(player.getInventory().getContents());
            checkAndAssignID(player.getEnderChest().getContents());

            checkForDuplicates(player.getName(), player.getInventory().getContents(), "Inventory", duplicateMap);
            checkForDuplicates(player.getName(), player.getEnderChest().getContents(), "EnderChest", duplicateMap);
        }

        if (scanContainers) {
            scanLoadedChunksForContainers(duplicateMap);
        }

        if (!duplicateMap.isEmpty() && enableAlerts) {
            alertStaff(duplicateMap, logAlerts);
        }
    }

    private void checkForDuplicates(String owner, ItemStack[] contents, String location, Map<String, List<String>> duplicateMap) {
        Map<String, ItemStack> idMap = new HashMap<>();

        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;

            String id = getItemID(item);
            if (id == null) continue;

            if (idMap.containsKey(id)) {
                duplicateMap.computeIfAbsent(id, k -> new ArrayList<>())
                            .add(location + " - " + owner + " (" + item.getType() + ")");
            } else {
                idMap.put(id, item);
            }

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

        List<String> highVulnerabilityItems = plugin.getConfig().getStringList("high-vulnerability-items");

        for (ItemStack item : items) {
            if (item == null || !item.getType().isItem()) continue;

            String itemType = item.getType().name();
            if (!highVulnerabilityItems.contains(itemType)) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String existingID = pdc.get(idKey, PersistentDataType.STRING);

            if (existingID == null) {
                existingID = UUID.randomUUID().toString();
                pdc.set(idKey, PersistentDataType.STRING, existingID);
            }

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String idLinePrefix = ChatColor.BLACK + "" + ChatColor.ITALIC + "ID: ";
            lore.removeIf(line -> line.startsWith(idLinePrefix));
            lore.add(idLinePrefix + existingID);

            meta.setLore(lore);
            item.setItemMeta(meta);

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
                        Location loc = container.getBlock().getLocation();
                        String id = containerIDMap.getOrDefault(loc, UUID.randomUUID().toString());
                        containerIDMap.put(loc, id); // Ensure ID persists

                        checkAndAssignID(container.getInventory().getContents());
                        checkForDuplicates("Container at " + loc.toVector().toString(), container.getInventory().getContents(), "Container", duplicateMap);
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

    private void logToFile(String logEntry) {
        File logFile = new File(plugin.getDataFolder(), "dupe-alerts.log");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
            bw.write(logEntry);
            bw.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to dupe-alerts.log: " + e.getMessage());
        }
    }

    private String getItemID(ItemStack item) {
        if (item == null || !item.getType().isItem()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(idKey, PersistentDataType.STRING);
    }
}
