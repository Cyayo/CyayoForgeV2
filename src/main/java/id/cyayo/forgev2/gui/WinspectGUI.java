package id.cyayo.forgev2.gui;

import id.cyayo.forgev2.CyayoForgeV2;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class WinspectGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final ItemStack weapon;

    public WinspectGUI(Player player, ItemStack weapon) {
        this.player = player;
        this.weapon = weapon;
        org.bukkit.configuration.file.FileConfiguration guiConfig = CyayoForgeV2.getInstance().getConfigManager().getWinspectGui();
        int size = guiConfig.getInt("size", 54);
        String title = org.bukkit.ChatColor.translateAlternateColorCodes('&', guiConfig.getString("name", "&8Large Chest"));
        
        this.inventory = Bukkit.createInventory(this, size, title);
        setupDecorations();
        populateData();
    }

    private void setupDecorations() {
        org.bukkit.configuration.file.FileConfiguration guiConfig = CyayoForgeV2.getInstance().getConfigManager().getWinspectGui();
        org.bukkit.configuration.ConfigurationSection decorations = guiConfig.getConfigurationSection("decorations");
        if (decorations != null) {
            for (String key : decorations.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection section = decorations.getConfigurationSection(key);
                if (section == null) continue;
                
                ItemStack item = id.cyayo.forgev2.util.ItemUtil.createDecorationItem(section);
                if (item == null) continue;
                
                List<Integer> slots = section.getIntegerList("slots");
                for (int slot : slots) {
                    if (slot >= 0 && slot < inventory.getSize()) {
                        inventory.setItem(slot, item.clone());
                    }
                }
            }
        }
    }
    
    private void populateData() {
        org.bukkit.configuration.file.FileConfiguration guiConfig = CyayoForgeV2.getInstance().getConfigManager().getWinspectGui();
        int weaponSlot = guiConfig.getInt("slots.weapon-slot", 4);
        inventory.setItem(weaponSlot, weapon);
        
        io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(weapon);
        
        String crafterName = nbt.getString("CYAYO_FORGE_CRAFTER");
        String dateMillis = nbt.getString("CYAYO_FORGE_DATE");

        double quality = 0.0;
        if (nbt.hasTag("CYAYO_FORGE_QUALITY")) {
            try {
                quality = nbt.getDouble("CYAYO_FORGE_QUALITY");
            } catch (Exception e) {
                try {
                    quality = Double.parseDouble(nbt.getString("CYAYO_FORGE_QUALITY"));
                } catch (Exception ignored) {}
            }
        }
        if (quality <= 0 && nbt.hasTag("MMOITEMS_FORGE_QUALITY")) {
            try {
                quality = nbt.getDouble("MMOITEMS_FORGE_QUALITY");
            } catch (Exception ignored) {}
        }
        if (quality <= 0 && nbt.hasTag("FORGE_QUALITY")) {
            try {
                quality = nbt.getDouble("FORGE_QUALITY");
            } catch (Exception ignored) {}
        }
        
        // Format waktu
        String dateStr = "Unknown";
        try {
            long ms = Long.parseLong(dateMillis);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm");
            dateStr = sdf.format(new java.util.Date(ms));
        } catch (Exception ignored) {}
        
        // Kaca Merah (Info Penempa & Kualitas)
        id.cyayo.forgev2.manager.ConfigManager cfg = CyayoForgeV2.getInstance().getConfigManager();
        List<Integer> redSlots = guiConfig.getIntegerList("decorations.red-glass.slots");
        if (redSlots.size() >= 2) {
            ItemStack info1 = new ItemStack(Material.PAPER);
            ItemMeta m1 = info1.getItemMeta();
            m1.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    cfg.getMessages().getString("winspect.crafter-info.title", "&c&lINFO PENEMPA")));
            List<String> rawLore1 = cfg.getMessages().getStringList("winspect.crafter-info.lore");
            if (rawLore1.isEmpty()) {
                rawLore1 = java.util.Arrays.asList("&7Penempa: &e%crafter%", "&7Tanggal: &e%date%");
            }
            int rerollCount = 0;
            if (nbt.hasTag("CYAYO_FORGE_REROLL_COUNT")) {
                rerollCount = nbt.getInteger("CYAYO_FORGE_REROLL_COUNT");
            } else if (nbt.hasTag("FORGE_REROLL_COUNT")) {
                try {
                    rerollCount = (int) Math.round(nbt.getDouble("FORGE_REROLL_COUNT"));
                } catch (Exception ignored) {}
            }

            List<String> lore1 = new ArrayList<>();
            for (String l : rawLore1) {
                String f = l.replace("%crafter%", crafterName != null && !crafterName.isEmpty() ? crafterName : "Unknown")
                            .replace("%date%", dateStr)
                            .replace("%reroll_count%", String.valueOf(rerollCount));
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', f));
            }

            String ingredientsUsed = nbt.getString("CYAYO_FORGE_INGREDIENTS");
            if (ingredientsUsed != null && !ingredientsUsed.trim().isEmpty()) {
                lore1.add("");
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&d&lINGREDIENTS:"));
                for (String ing : ingredientsUsed.split(", ")) {
                    lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &7■ &f" + ing));
                }
            }

            String traitsInjected = nbt.getString("CYAYO_FORGE_TRAITS");
            if (traitsInjected != null && !traitsInjected.trim().isEmpty()) {
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lTRAITS BONUS:"));
                for (String tr : traitsInjected.split(";")) {
                    lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &7■ &e" + tr));
                }
            }

            String abilitiesInjected = nbt.getString("CYAYO_FORGE_ABILITIES");
            if (abilitiesInjected != null && !abilitiesInjected.trim().isEmpty()) {
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b&lSPECIAL ABILITIES:"));
                for (String ab : abilitiesInjected.split(";")) {
                    lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &7■ &b" + ab));
                }
            }

            boolean isShiny = nbt.hasTag("CYAYO_FORGE_IS_SHINY") && nbt.getBoolean("CYAYO_FORGE_IS_SHINY");
            if (!isShiny && nbt.hasTag("MMOITEMS_FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(nbt.getString("MMOITEMS_FORGE_IS_SHINY"))) {
                isShiny = true;
            }
            if (isShiny) {
                String trName = nbt.getString("CYAYO_SHINY_TRACKER_NAME");
                if (trName.isEmpty()) trName = nbt.getString("MMOITEMS_FORGE_TRACKER_NAME");
                if (trName.isEmpty()) trName = nbt.getString("CYAYO_SHINY_TRACKER_ID");
                long trVal = 0;
                if (nbt.hasTag("CYAYO_SHINY_TRACKER_VALUE")) {
                    try {
                        trVal = nbt.getInteger("CYAYO_SHINY_TRACKER_VALUE");
                    } catch (Exception e) {
                        try {
                            trVal = (long) Math.round(nbt.getDouble("CYAYO_SHINY_TRACKER_VALUE"));
                        } catch (Exception ignored) {}
                    }
                }
                lore1.add("");
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6★ &e&lSHINY ITEM (STATTRAK)"));
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &7■ &fTracker: &e" + (trName.isEmpty() ? "None" : trName)));
                lore1.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &7■ &fProgress: &a" + String.format(java.util.Locale.US, "%,d", trVal)));
            }

            m1.setLore(lore1);
            info1.setItemMeta(m1);
            id.cyayo.forgev2.util.ItemUtil.markAsDecoration(info1);
            inventory.setItem(redSlots.get(0), info1);
            
            ItemStack info2 = new ItemStack(Material.NETHER_STAR);
            ItemMeta m2 = info2.getItemMeta();
            m2.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    cfg.getMessages().getString("winspect.quality-info.title", "&a&lKUALITAS")));
            List<String> rawLore2 = cfg.getMessages().getStringList("winspect.quality-info.lore");
            if (rawLore2.isEmpty()) {
                rawLore2 = java.util.Collections.singletonList("&7Tingkat Kesempurnaan: &b%quality%%");
            }
            List<String> lore2 = new ArrayList<>();
            for (String l : rawLore2) {
                String f = l.replace("%quality%", String.format(java.util.Locale.US, "%.1f", quality))
                            .replace("%reroll_count%", String.valueOf(rerollCount));
                lore2.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', f));
            }
            m2.setLore(lore2);
            info2.setItemMeta(m2);
            id.cyayo.forgev2.util.ItemUtil.markAsDecoration(info2);
            inventory.setItem(redSlots.get(1), info2);
        }
        
        // Material Berdasarkan 3 Kategori (Material 1, Material 2, Material 3)
        String mat1 = nbt.getString("CYAYO_FORGE_MATS_1");
        if (mat1 == null || mat1.isEmpty()) mat1 = nbt.getString("CYAYO_FORGE_MATS_MOB");

        String mat2 = nbt.getString("CYAYO_FORGE_MATS_2");
        if (mat2 == null || mat2.isEmpty()) mat2 = nbt.getString("CYAYO_FORGE_MATS_FISH");

        String mat3 = nbt.getString("CYAYO_FORGE_MATS_3");
        if (mat3 == null || mat3.isEmpty()) mat3 = nbt.getString("CYAYO_FORGE_MATS_MINE");
        
        populateColumn("MATERIAL_1", mat1, new int[]{11, 20, 29});
        populateColumn("MATERIAL_2", mat2, new int[]{13, 22, 31});
        populateColumn("MATERIAL_3", mat3, new int[]{15, 24, 33});
    }
    
    private void populateColumn(String categoryKey, String matStr, int[] slots) {
        id.cyayo.forgev2.manager.ConfigManager cfg = CyayoForgeV2.getInstance().getConfigManager();
        String catName = CyayoForgeV2.getInstance().getForgeManager().getCategoryName(categoryKey);

        List<String> materialLoreTemplate = cfg.getMessages().getStringList("winspect.material-lore");
        if (materialLoreTemplate.isEmpty()) {
            materialLoreTemplate = java.util.Arrays.asList("", "&7Bagian: %category%", "&7Jumlah Digunakan: &f%amount%x");
        }

        List<String> emptyLoreTemplate = cfg.getMessages().getStringList("winspect.empty-slot.lore");
        if (emptyLoreTemplate.isEmpty()) {
            emptyLoreTemplate = java.util.Collections.singletonList("&7(Tidak ada material yang digunakan)");
        }
        String emptyNameTemplate = cfg.getMessages().getString("winspect.empty-slot.name", "%category%");

        if (matStr == null || matStr.trim().isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
            ItemMeta em = emptyItem.getItemMeta();
            if (em != null) {
                em.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', emptyNameTemplate.replace("%category%", catName)));
                List<String> el = new ArrayList<>();
                for (String l : emptyLoreTemplate) {
                    el.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', l.replace("%category%", catName)));
                }
                em.setLore(el);
                emptyItem.setItemMeta(em);
            }
            id.cyayo.forgev2.util.ItemUtil.markAsDecoration(emptyItem);
            inventory.setItem(slots[0], emptyItem);
            return;
        }

        String[] split = matStr.split(",");
        for (int i = 0; i < slots.length; i++) {
            if (i < split.length && !split[i].trim().isEmpty()) {
                String raw = split[i].trim();
                String[] matData = raw.split(":");
                if (matData.length == 2) {
                    String fullKey = matData[0];
                    int amount = 1;
                    try {
                        amount = Integer.parseInt(matData[1]);
                    } catch (Exception ignored) {}

                    String mmoType = null;
                    String mmoId = fullKey;
                    if (fullKey.contains("/")) {
                        String[] slash = fullKey.split("/", 2);
                        mmoType = slash[0];
                        mmoId = slash[1];
                    }

                    ItemStack matDisplay = null;
                    if (mmoType != null) {
                        matDisplay = net.Indyuce.mmoitems.MMOItems.plugin.getItem(mmoType, mmoId);
                    }
                    if (matDisplay == null) {
                        List<String> validTypes = cfg.getMaterials().getStringList("categories." + categoryKey + ".valid-types");
                        for (String vt : validTypes) {
                            matDisplay = net.Indyuce.mmoitems.MMOItems.plugin.getItem(vt, mmoId);
                            if (matDisplay != null) break;
                        }
                    }
                    if (matDisplay == null) {
                        Material m = Material.matchMaterial(mmoId);
                        if (m != null && !m.isAir()) {
                            matDisplay = new ItemStack(m);
                        } else {
                            matDisplay = new ItemStack(Material.CHEST);
                        }
                    } else {
                        matDisplay = matDisplay.clone();
                    }

                    ItemMeta md = matDisplay.getItemMeta();
                    if (md != null) {
                        String displayName = md.hasDisplayName() ? md.getDisplayName() : "§6" + mmoId.replace("_", " ");
                        md.setDisplayName(displayName);

                        List<String> dl = md.hasLore() ? new ArrayList<>(md.getLore()) : new ArrayList<>();
                        for (String l : materialLoreTemplate) {
                            String f = l.replace("%category%", catName)
                                        .replace("%amount%", String.valueOf(amount))
                                        .replace("%name%", displayName);
                            dl.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', f));
                        }
                        md.setLore(dl);
                        matDisplay.setItemMeta(md);
                    }
                    id.cyayo.forgev2.util.ItemUtil.markAsDecoration(matDisplay);
                    inventory.setItem(slots[i], matDisplay);
                }
            } else {
                ItemStack emptyItem = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
                ItemMeta em = emptyItem.getItemMeta();
                if (em != null) {
                    em.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', emptyNameTemplate.replace("%category%", catName)));
                    List<String> el = new ArrayList<>();
                    for (String l : emptyLoreTemplate) {
                        el.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', l.replace("%category%", catName)));
                    }
                    em.setLore(el);
                    emptyItem.setItemMeta(em);
                }
                id.cyayo.forgev2.util.ItemUtil.markAsDecoration(emptyItem);
                inventory.setItem(slots[i], emptyItem);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
