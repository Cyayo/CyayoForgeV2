package id.cyayo.forgev2.manager;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.model.ShinyTracker;
import id.cyayo.forgev2.util.ItemUtil;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ShinyTrackerManager {

    private final CyayoForgeV2 plugin;
    private final Map<String, ShinyTracker> trackers = new LinkedHashMap<>();

    public ShinyTrackerManager(CyayoForgeV2 plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        trackers.clear();
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection sec = config.getConfigurationSection("shiny.trackers");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection tSec = sec.getConfigurationSection(key);
                if (tSec != null) {
                    ShinyTracker tracker = ShinyTracker.fromSection(key, tSec);
                    if (tracker != null) {
                        trackers.put(key.toLowerCase(), tracker);
                    }
                }
            }
        }
    }

    public boolean isShinyEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("shiny.enabled", true);
    }

    public String getNamePrefix() {
        return plugin.getConfigManager().getConfig().getString("shiny.name-format.prefix", "&6★ &eShiny ");
    }

    public String getNameSuffix() {
        return plugin.getConfigManager().getConfig().getString("shiny.name-format.suffix", "");
    }

    public boolean isGlowEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("shiny.glow", true);
    }

    public String getPreviewTrackerName() {
        return plugin.getConfigManager().getConfig().getString("shiny.preview-tracker-name", "Unknown Track");
    }

    public String getTrackerFormat() {
        return plugin.getConfigManager().getConfig().getString("shiny.tracker-format", "&6★ &e%tracker_name%: &f%count%");
    }

    public String formatTrackerString(String displayName, long count) {
        String template = getTrackerFormat();
        String formattedCount = String.format(Locale.US, "%,d", count);
        String res = template.replace("%tracker_name%", displayName != null ? displayName : "Tracker")
                             .replace("%count%", formattedCount);
        return ItemUtil.colorize(res);
    }

    public Map<String, ShinyTracker> getTrackers() {
        return Collections.unmodifiableMap(trackers);
    }

    public ShinyTracker getTrackerById(String id) {
        if (id == null) return null;
        return trackers.get(id.trim().toLowerCase());
    }

    public List<ShinyTracker> getMatchingTrackers(boolean isWeapon, String targetType) {
        List<ShinyTracker> matching = new ArrayList<>();
        for (ShinyTracker t : trackers.values()) {
            if (t.matchesTarget(isWeapon, targetType)) {
                matching.add(t);
            }
        }
        return matching;
    }

    public ShinyTracker rollRandomTracker(boolean isWeapon, String targetType) {
        List<ShinyTracker> pool = getMatchingTrackers(isWeapon, targetType);
        if (pool.isEmpty()) {
            if (!trackers.isEmpty()) {
                pool = new ArrayList<>(trackers.values());
            } else {
                return new ShinyTracker("mobs_killed", "Mobs Killed", "MOBS_KILLED", "ALL", 100);
            }
        }

        int totalWeight = 0;
        for (ShinyTracker t : pool) {
            totalWeight += t.getWeight();
        }

        int rand = ThreadLocalRandom.current().nextInt(Math.max(1, totalWeight));
        int cur = 0;
        for (ShinyTracker t : pool) {
            cur += t.getWeight();
            if (rand < cur) {
                return t;
            }
        }
        return pool.get(0);
    }

    /**
     * Memperbarui nilai tracker pada sebuah ItemStack jika item adalah Shiny dan trackernya cocok.
     * Mengembalikan item baru yang sudah diupdate, atau null jika item tidak cocok/tidak diubah.
     */
    public ItemStack updateItemTracker(ItemStack item, String triggerOrId, long amountToAdd) {
        if (item == null || item.getType().isAir() || amountToAdd == 0) return null;

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return null;

        boolean isShiny = nbt.hasTag("CYAYO_FORGE_IS_SHINY") && nbt.getBoolean("CYAYO_FORGE_IS_SHINY");
        if (!isShiny) {
            if (nbt.hasTag("MMOITEMS_FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(nbt.getString("MMOITEMS_FORGE_IS_SHINY"))) {
                isShiny = true;
            }
        }
        if (!isShiny) return null;

        String itemTrackerId = nbt.getString("CYAYO_SHINY_TRACKER_ID");
        if (itemTrackerId == null || itemTrackerId.isEmpty()) return null;

        ShinyTracker trackerDef = getTrackerById(itemTrackerId);
        String trackerTrigger = trackerDef != null ? trackerDef.getTriggerType() : "CUSTOM";
        String trackerDisplayName = nbt.getString("CYAYO_SHINY_TRACKER_NAME");
        if (trackerDisplayName.isEmpty() && trackerDef != null) {
            trackerDisplayName = trackerDef.getDisplayName();
        }
        if (trackerDisplayName.isEmpty()) trackerDisplayName = itemTrackerId;

        boolean match = false;
        if (triggerOrId.equalsIgnoreCase("*") || triggerOrId.equalsIgnoreCase("ALL")) {
            match = true;
        } else if (triggerOrId.equalsIgnoreCase(itemTrackerId)) {
            match = true;
        } else if (triggerOrId.equalsIgnoreCase(trackerTrigger)) {
            match = true;
        }

        if (!match) return null;

        long currentVal = 0;
        if (nbt.hasTag("CYAYO_SHINY_TRACKER_VALUE")) {
            try {
                currentVal = nbt.getInteger("CYAYO_SHINY_TRACKER_VALUE");
            } catch (Exception e) {
                try {
                    currentVal = (long) Math.round(nbt.getDouble("CYAYO_SHINY_TRACKER_VALUE"));
                } catch (Exception ignored) {}
            }
        }

        long newVal = Math.max(0, currentVal + amountToAdd);
        String newLine = formatTrackerString(trackerDisplayName, newVal);

        nbt.addTag(new ItemTag("CYAYO_SHINY_TRACKER_VALUE", (int) newVal));
        nbt.addTag(new ItemTag("MMOITEMS_FORGE_TRACKER_VALUE", (double) newVal));
        nbt.addTag(new ItemTag("MMOITEMS_FORGE_TRACKER", newLine));

        ItemStack updated = nbt.toItem();
        ItemMeta meta = updated.getItemMeta();
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            List<String> lore = meta.getLore();
            String cleanName = ChatColor.stripColor(trackerDisplayName).toLowerCase();
            boolean replaced = false;

            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line == null) continue;
                String cleanLine = ChatColor.stripColor(line).toLowerCase();
                if (cleanLine.contains(cleanName) || cleanLine.contains("★") || cleanLine.contains("stattrak")) {
                    lore.set(i, newLine);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                lore.add(newLine);
            }
            meta.setLore(lore);
            if (isGlowEnabled()) {
                ItemUtil.applyGlow(meta);
            }
            updated.setItemMeta(meta);
        }

        return updated;
    }

    /**
     * Menambahkan nilai tracker ke gear pemain (mainhand, armor, atau all).
     * Mengembalikan jumlah item yang berhasil diperbarui.
     */
    public int addTrackerToPlayer(Player player, String trackerTriggerOrId, long amountToAdd, String slotFilter) {
        if (player == null || !player.isOnline() || amountToAdd == 0) return 0;

        int updatedCount = 0;
        String filter = slotFilter != null ? slotFilter.trim().toLowerCase() : "all";

        // 1. Cek Main Hand
        if (filter.equals("all") || filter.equals("mainhand") || filter.equals("hand") || filter.equals("weapon")) {
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack res = updateItemTracker(main, trackerTriggerOrId, amountToAdd);
            if (res != null) {
                player.getInventory().setItemInMainHand(res);
                updatedCount++;
            }
        }

        // 2. Cek Armor
        if (filter.equals("all") || filter.equals("armor")) {
            ItemStack[] armors = player.getInventory().getArmorContents();
            boolean changed = false;
            for (int i = 0; i < armors.length; i++) {
                ItemStack arm = armors[i];
                ItemStack res = updateItemTracker(arm, trackerTriggerOrId, amountToAdd);
                if (res != null) {
                    armors[i] = res;
                    changed = true;
                    updatedCount++;
                }
            }
            if (changed) {
                player.getInventory().setArmorContents(armors);
            }
        }

        return updatedCount;
    }
}
