package dev.nitrogendioxide.nodupe.listeners;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import dev.nitrogendioxide.nodupe.NoDupePlugin;
import dev.nitrogendioxide.nodupe.utils.DuplicateChecker;

import java.io.*;
import java.time.Instant;
import java.util.*;

public class InventoryCheckTask extends BukkitRunnable {

    private final NoDupePlugin plugin;
    private final DuplicateChecker duplicateChecker;
    private final NamespacedKey idKey;

    public InventoryCheckTask(NoDupePlugin plugin, DuplicateChecker duplicateChecker) {
        this.plugin = plugin;
        this.duplicateChecker = duplicateChecker;
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
        new InventoryCheckTask(plugin, duplicateChecker).runTaskLater(plugin, interval * 20L);

        boolean removeDuplicates = plugin.getConfig().getBoolean("remove-duplicates", false);
        Map<String, List<String>> duplicateMap = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndAssignID(player.getInventory().getContents());
            checkAndAssignID(player.getEnderChest().getContents());

            checkForDuplicates(player.getName(), player.getInventory(), "Inventory", duplicateMap, removeDuplicates);
            checkForDuplicates(player.getName(), player.getEnderChest(), "EnderChest", duplicateMap, removeDuplicates);
        }

        if (scanContainers) {
            scanLoadedChunksForContainers(duplicateMap, removeDuplicates);
        }

        if (!duplicateMap.isEmpty() && enableAlerts) {
            alertStaff(duplicateMap, logAlerts);
        }
    }

    private void checkForDuplicates(String owner, Inventory inventory, String location, Map<String, List<String>> duplicateMap, boolean removeDuplicates) {
        ItemStack[] contents = inventory.getContents();
        Map<String, Integer> idCountMap = new HashMap<>();
        Map<String, List<Integer>> idSlotMap = new HashMap<>();
        Map<String, Material> idTypeMap = new HashMap<>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String id = getItemID(item);
            if (id == null) continue;

            idCountMap.put(id, idCountMap.getOrDefault(id, 0) + 1);
            idSlotMap.computeIfAbsent(id, k -> new ArrayList<>()).add(i);
            idTypeMap.putIfAbsent(id, item.getType());
        }

        for (Map.Entry<String, Integer> entry : idCountMap.entrySet()) {
            String id = entry.getKey();
            int count = entry.getValue();
            if (count > 1) {
                List<Integer> slots = idSlotMap.get(id);
                Material itemType = idTypeMap.get(id);

                if (removeDuplicates) {
                    for (int i = 1; i < slots.size(); i++) {
                        contents[slots.get(i)] = null;
                    }
                    inventory.setContents(contents); // Apply the change to the inventory
                }

                String alertMessage = ChatColor.YELLOW + "Item: " + ChatColor.WHITE + itemType.name() +
                                      ChatColor.YELLOW + " | ID: " + ChatColor.WHITE + id;
                duplicateMap.computeIfAbsent(alertMessage, k -> new ArrayList<>()).add(location + " - " + owner);
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

    private void scanLoadedChunksForContainers(Map<String, List<String>> duplicateMap, boolean removeDuplicates) {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (tile instanceof Container) {
                        Container container = (Container) tile;
                        String location = container.getBlock().getLocation().toVector().toString();
                        checkAndAssignID(container.getInventory().getContents());
                        checkForDuplicates("Container at " + location, container.getInventory(), "Container", duplicateMap, removeDuplicates);
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

            alertMessage.append(ChatColor.YELLOW).append("ID: ").append(ChatColor.WHITE).append(id).append("\n");

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
