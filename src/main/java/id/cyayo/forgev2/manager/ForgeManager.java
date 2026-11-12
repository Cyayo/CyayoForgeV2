package id.cyayo.forgev2.manager;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.util.ItemUtil;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.cyayo.forgev2.model.IngredientTrait;
import id.cyayo.forgev2.util.IngredientSpatialUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ForgeManager {

    private final CyayoForgeV2 plugin;

    public ForgeManager(CyayoForgeV2 plugin) {
        this.plugin = plugin;
    }

    /**
     * Helper untuk mengambil String dari NBT dengan berbagai variasi casing/prefix.
     */
    public String getNbtString(NBTItem nbt, String key) {
        if (nbt == null || key == null) return "";
        String[] prefixes = new String[]{"MMOITEMS_", ""};
        for (String p : prefixes) {
            String fullKey = p + key;
            if (nbt.hasTag(fullKey)) return nbt.getString(fullKey);
            if (nbt.hasTag(fullKey.toLowerCase())) return nbt.getString(fullKey.toLowerCase());
            if (nbt.hasTag(fullKey.toUpperCase())) return nbt.getString(fullKey.toUpperCase());
        }
        return "";
    }

    /**
     * Helper untuk mengambil nilai numerik syarat dari NBT dengan berbagai variasi tag.
     */
    public int getRequirement(NBTItem nbt, String key) {
        String[] possibleKeys = new String[]{
                "MMOITEMS_" + key,
                key,
                key.toLowerCase(),
                "MMOITEMS_" + key.toLowerCase()
        };

        for (String k : possibleKeys) {
            if (!nbt.hasTag(k)) continue;
            try {
                double d = nbt.getDouble(k);
                if (d > 0) return (int) Math.round(d);
            } catch (Exception ignored) {}
            try {
                int i = nbt.getInteger(k);
                if (i > 0) return i;
            } catch (Exception ignored) {}
            try {
                String s = nbt.getString(k);
                if (s != null && !s.isEmpty()) {
                    double parsed = Double.parseDouble(s);
                    if (parsed > 0) return (int) Math.round(parsed);
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    /**
     * Membaca daftar syarat material dinamis dari Blueprint.
     * Mendukung format baru FORGE_REQUIREMENTS (StringList/JSON/String)
     * dan fallback legacy FORGE_REQ_1 / FORGE_REQ_MOB, dll.
     */
    public List<id.cyayo.forgev2.model.BlueprintRequirement> getRequirements(NBTItem bpNbt) {
        List<id.cyayo.forgev2.model.BlueprintRequirement> list = new ArrayList<>();
        if (bpNbt == null) return list;

        // 1. Coba baca dari FORGE_REQUIREMENTS (StringList / JSON / Delimited String)
        String[] possibleKeys = new String[]{"MMOITEMS_FORGE_REQUIREMENTS", "FORGE_REQUIREMENTS", "forge_requirements"};
        for (String key : possibleKeys) {
            if (bpNbt.hasTag(key)) {
                try {
                    String raw = bpNbt.getString(key);
                    if (raw != null && !raw.trim().isEmpty()) {
                        // Jika berupa format JSON array string ["A:10","B:5"]
                        if (raw.startsWith("[") && raw.endsWith("]")) {
                            raw = raw.substring(1, raw.length() - 1);
                        }
                        String[] entries = raw.split("[,;\n\r]+");
                        for (String entry : entries) {
                            String clean = entry.replace("\"", "").replace("'", "").trim();
                            if (clean.isEmpty()) continue;
                            if (clean.contains(":")) {
                                String[] split = clean.split(":", 2);
                                String cat = split[0].trim();
                                int amt = (int) Math.round(Double.parseDouble(split[1].trim()));
                                if (!cat.isEmpty() && amt > 0) {
                                    list.add(new id.cyayo.forgev2.model.BlueprintRequirement(cat, amt));
                                }
                            }
                        }
                        if (!list.isEmpty()) return list;
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Fallback Legacy jika format baru tidak ditemukan
        int req1 = getRequirement(bpNbt, "FORGE_REQ_1");
        if (req1 <= 0) req1 = getRequirement(bpNbt, "FORGE_REQ_MAT1");
        if (req1 <= 0) req1 = getRequirement(bpNbt, "FORGE_REQ_MOB");

        int req2 = getRequirement(bpNbt, "FORGE_REQ_2");
        if (req2 <= 0) req2 = getRequirement(bpNbt, "FORGE_REQ_MAT2");
        if (req2 <= 0) req2 = getRequirement(bpNbt, "FORGE_REQ_FISH");

        int req3 = getRequirement(bpNbt, "FORGE_REQ_3");
        if (req3 <= 0) req3 = getRequirement(bpNbt, "FORGE_REQ_MAT3");
        if (req3 <= 0) req3 = getRequirement(bpNbt, "FORGE_REQ_MINE");

        if (req1 > 0 || (req2 <= 0 && req3 <= 0)) {
            list.add(new id.cyayo.forgev2.model.BlueprintRequirement("MATERIAL_1", Math.max(1, req1)));
        }
        if (req2 > 0) {
            list.add(new id.cyayo.forgev2.model.BlueprintRequirement("MATERIAL_2", req2));
        }
        if (req3 > 0) {
            list.add(new id.cyayo.forgev2.model.BlueprintRequirement("MATERIAL_3", req3));
        }

        return list;
    }

    public id.cyayo.forgev2.model.BlueprintRequirement getRequirementForSlot(NBTItem bpNbt, int slotIndex) {
        List<id.cyayo.forgev2.model.BlueprintRequirement> reqs = getRequirements(bpNbt);
        if (slotIndex >= 0 && slotIndex < reqs.size()) {
            return reqs.get(slotIndex);
        }
        return null;
    }

    public int getRequiredMat1(NBTItem bpNbt) {
        id.cyayo.forgev2.model.BlueprintRequirement req = getRequirementForSlot(bpNbt, 0);
        return req != null ? req.getAmountRequired() : 0;
    }

    public int getRequiredMat2(NBTItem bpNbt) {
        id.cyayo.forgev2.model.BlueprintRequirement req = getRequirementForSlot(bpNbt, 1);
        return req != null ? req.getAmountRequired() : 0;
    }

    public int getRequiredMat3(NBTItem bpNbt) {
        id.cyayo.forgev2.model.BlueprintRequirement req = getRequirementForSlot(bpNbt, 2);
        return req != null ? req.getAmountRequired() : 0;
    }

    public int getRequiredMob(NBTItem bpNbt) { return getRequiredMat1(bpNbt); }
    public int getRequiredFish(NBTItem bpNbt) { return getRequiredMat2(bpNbt); }
    public int getRequiredMine(NBTItem bpNbt) { return getRequiredMat3(bpNbt); }

    public String getCategoryName(String categoryKey) {
        if (categoryKey == null || categoryKey.equalsIgnoreCase("NONE") || categoryKey.trim().isEmpty()) {
            return "§7-";
        }
        String cleanKey = categoryKey.trim();

        // 1. Cek langsung dari materials.yml (categories.<KEY>.name)
        String direct = plugin.getConfigManager().getMaterials().getString("categories." + cleanKey + ".name");
        if (direct != null && !direct.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', direct);
        }

        // 2. Cek secara case-insensitive dari seluruh key yang terdaftar di categories
        org.bukkit.configuration.ConfigurationSection catSec = plugin.getConfigManager().getMaterials().getConfigurationSection("categories");
        if (catSec != null) {
            for (String k : catSec.getKeys(false)) {
                if (k.equalsIgnoreCase(cleanKey)) {
                    String val = catSec.getString(k + ".name");
                    if (val != null && !val.isEmpty()) {
                        return ChatColor.translateAlternateColorCodes('&', val);
                    }
                }
            }
        }

        // 3. Jika tidak ada di config, format nama yang rapi
        return ItemUtil.colorize(cleanKey.replace("_", " "));
    }

    public String getQualityMiniMessageTemplate(double quality) {
        String qualityStr = (quality == (long) quality) ? String.valueOf((long) quality) : String.format(java.util.Locale.US, "%.1f", quality);
        
        org.bukkit.configuration.ConfigurationSection formatSec = plugin.getConfigManager().getConfig().getConfigurationSection("lore-injector-formats");
        java.util.TreeMap<Integer, String> formats = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        if (formatSec != null) {
            for (String k : formatSec.getKeys(false)) {
                try {
                    int thresh = Integer.parseInt(k);
                    formats.put(thresh, formatSec.getString(k));
                } catch (Exception ignored) {}
            }
        }
        if (formats.isEmpty()) {
            formats.put(100, "&6%percentage%%");
            formats.put(90, "&a%percentage%%");
            formats.put(70, "&e%percentage%%");
            formats.put(40, "&6%percentage%%");
            formats.put(0, "&c%percentage%%");
        }

        String template = "&f%percentage%%";
        for (Map.Entry<Integer, String> entry : formats.entrySet()) {
            if (quality >= entry.getKey()) {
                template = entry.getValue();
                break;
            }
        }

        // Ambil format persis dari lore-injector-formats, namun hapus tanda kurung siku [ ] dan kata Max
        String cleanTemplate = template
                .replace("[", "")
                .replace("]", "")
                .replace(" Max", "")
                .trim();

        // Bersihkan kode warna abu-abu &8 di awal/akhir jika ada sisa dari kurung siku
        cleanTemplate = cleanTemplate.replaceAll("^&8", "").replaceAll("&8$", "").trim();

        return cleanTemplate.replace("%percentage%", qualityStr);
    }

    public String formatQualityPercentage(double quality) {
        return ItemUtil.colorize(getQualityMiniMessageTemplate(quality));
    }

    private List<String> getCategoryStringList(String categoryKey, String subKey) {
        if (categoryKey == null || categoryKey.trim().isEmpty()) return new ArrayList<>();
        String cleanKey = categoryKey.trim();

        List<String> list = plugin.getConfigManager().getMaterials().getStringList("categories." + cleanKey + "." + subKey);
        if (!list.isEmpty()) return list;

        // Cek case-insensitive
        org.bukkit.configuration.ConfigurationSection catSec = plugin.getConfigManager().getMaterials().getConfigurationSection("categories");
        if (catSec != null) {
            for (String k : catSec.getKeys(false)) {
                if (k.equalsIgnoreCase(cleanKey)) {
                    return catSec.getStringList(k + "." + subKey);
                }
            }
        }
        return new ArrayList<>();
    }

    public String getTargetType(NBTItem bpNbt) {
        if (bpNbt == null) return "";
        // 1. Coba baca dari FORGE_TARGET ("TYPE:ID")
        String target = getNbtString(bpNbt, "FORGE_TARGET");
        if (target.contains(":")) {
            return target.split(":", 2)[0].trim();
        }

        // 2. Fallback jika masih terpisah
        String type = getNbtString(bpNbt, "FORGE_TARGET_TYPE");
        if (type.isEmpty()) type = getNbtString(bpNbt, "TARGET_TYPE");
        if (type.isEmpty()) type = getNbtString(bpNbt, "CRAFT_TARGET_TYPE");
        if (type.isEmpty()) {
            type = bpNbt.getType();
        }
        return type != null ? type.trim() : "";
    }

    public String getTargetId(NBTItem bpNbt) {
        if (bpNbt == null) return "";
        // 1. Coba baca dari FORGE_TARGET ("TYPE:ID")
        String target = getNbtString(bpNbt, "FORGE_TARGET");
        if (target.contains(":")) {
            return target.split(":", 2)[1].trim();
        }

        // 2. Fallback jika masih terpisah
        String id = getNbtString(bpNbt, "FORGE_TARGET_ID");
        if (id.isEmpty()) id = getNbtString(bpNbt, "TARGET_ID");
        if (id.isEmpty()) id = getNbtString(bpNbt, "CRAFT_TARGET_ID");
        if (id.isEmpty()) {
            id = bpNbt.getString("MMOITEMS_ITEM_ID");
        }
        if (id.isEmpty()) {
            id = getNbtString(bpNbt, "ITEM_ID");
        }
        return id != null ? id.trim() : "";
    }

    public String getTargetTier(NBTItem bpNbt) {
        if (bpNbt == null) return "";
        // 1. Ambil dari stat Tier bawaan MMOItems (MMOITEMS_TIER)
        String tier = bpNbt.getString("MMOITEMS_TIER");
        if (tier != null && !tier.trim().isEmpty()) {
            return tier.trim();
        }

        // 2. Ambil dari data stat MMOItems bawaan jika ada
        try {
            VolatileMMOItem vItem = new VolatileMMOItem(bpNbt);
            if (vItem.hasData(net.Indyuce.mmoitems.ItemStats.TIER)) {
                Object tData = vItem.getData(net.Indyuce.mmoitems.ItemStats.TIER);
                if (tData != null) {
                    String s = tData.toString().trim();
                    if (!s.isEmpty()) return s;
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback jika tersimpan di tag NBT lain
        if (tier == null || tier.isEmpty()) {
            tier = getNbtString(bpNbt, "TIER");
        }
        return tier != null ? tier.trim() : "";
    }

    public net.Indyuce.mmoitems.api.ItemTier resolveItemTier(String tierName) {
        if (tierName == null || tierName.trim().isEmpty()) return null;
        try {
            if (MMOItems.plugin.getTiers() != null) {
                net.Indyuce.mmoitems.api.ItemTier tier = MMOItems.plugin.getTiers().get(tierName.trim());
                if (tier != null) return tier;
                for (net.Indyuce.mmoitems.api.ItemTier it : MMOItems.plugin.getTiers().getAll()) {
                    if (it.getId().equalsIgnoreCase(tierName.trim()) 
                            || it.getName().equalsIgnoreCase(tierName.trim()) 
                            || ChatColor.stripColor(it.getName()).equalsIgnoreCase(tierName.trim())) {
                        return it;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Mengecek apakah item merupakan Blueprint yang sah.
     */
    public boolean isBlueprint(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;

        // 1. Cek Custom Stats MMOItems (FORGE_TARGET_ID atau FORGE_TARGET_TYPE)
        if (!getNbtString(nbt, "FORGE_TARGET_ID").isEmpty() || !getNbtString(nbt, "FORGE_TARGET_TYPE").isEmpty()) {
            return true;
        }

        // 2. Cek tipe MMOItems bawaan
        String type = nbt.getType();
        if (type.equalsIgnoreCase("BLUEPRINT") || type.equalsIgnoreCase("RECIPE")) {
            return true;
        }

        // 3. Cek konfigurasi materials.yml (jika didaftarkan)
        List<String> validTypes = plugin.getConfigManager().getMaterials().getStringList("blueprint.valid-types");
        List<String> validItems = plugin.getConfigManager().getMaterials().getStringList("blueprint.valid-items");
        String fullId = type + ":" + nbt.getString("MMOITEMS_ITEM_ID");

        return validTypes.contains(type) || validItems.contains(fullId);
    }

    /**
     * Mengecek apakah item merupakan Blueprint bertipe Shiny.
     */
    public boolean isShinyBlueprint(ItemStack item) {
        if (!isBlueprint(item)) return false;
        if (plugin.getShinyTrackerManager() == null || !plugin.getShinyTrackerManager().isShinyEnabled()) return false;

        NBTItem nbt = NBTItem.get(item);

        // 1. Cek NBT Tags langsung
        if (nbt.hasTag("CYAYO_SHINY") && nbt.getBoolean("CYAYO_SHINY")) return true;
        if (nbt.hasTag("CYAYO_FORGE_IS_SHINY") && nbt.getBoolean("CYAYO_FORGE_IS_SHINY")) return true;
        if (nbt.hasTag("MMOITEMS_SHINY")) {
            String sVal = nbt.getString("MMOITEMS_SHINY");
            if ("TRUE".equalsIgnoreCase(sVal) || nbt.getBoolean("MMOITEMS_SHINY")) return true;
        }
        if (nbt.hasTag("SHINY")) {
            String sVal = nbt.getString("SHINY");
            if ("TRUE".equalsIgnoreCase(sVal) || nbt.getBoolean("SHINY")) return true;
        }
        if (nbt.hasTag("FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(nbt.getString("FORGE_IS_SHINY"))) return true;
        if (nbt.hasTag("MMOITEMS_FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(nbt.getString("MMOITEMS_FORGE_IS_SHINY"))) return true;

        // 2. Cek Custom Stat MMOItems jika ada
        String shinyStat = getNbtString(nbt, "FORGE_IS_SHINY");
        if ("TRUE".equalsIgnoreCase(shinyStat) || "YES".equalsIgnoreCase(shinyStat)) return true;

        // 3. Cek ID item
        String itemId = nbt.getString("MMOITEMS_ITEM_ID");
        if (itemId != null && (itemId.toUpperCase().contains("SHINY") || itemId.toUpperCase().endsWith("_SHINY"))) {
            return true;
        }

        // 4. Cek Display Name
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String dName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toUpperCase();
            if (dName.contains("SHINY") || dName.contains("★")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Mengambil data IngredientTrait dari item jika terdaftar di trait.yml.
     */
    public IngredientTrait getIngredientTrait(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        NBTItem nbt = NBTItem.get(item);

        String type = null;
        String id = null;

        if (nbt.hasType()) {
            type = nbt.getString("MMOITEMS_ITEM_TYPE");
            if (type == null || type.isEmpty()) type = nbt.getType();
            id = nbt.getString("MMOITEMS_ITEM_ID");
            if (id == null || id.isEmpty()) id = MMOItems.getID(item);
        } else {
            type = "VANILLA";
            id = item.getType().name();
        }

        if (id == null || id.isEmpty()) return null;

        String fullId = (type + ":" + id).toUpperCase();
        String simpleId = id.toUpperCase();

        FileConfiguration traitCfg = plugin.getConfigManager().getTraits();
        if (traitCfg == null) return null;

        ConfigurationSection itemsSec = traitCfg.getConfigurationSection("items");
        if (itemsSec == null) return null;

        for (String key : itemsSec.getKeys(false)) {
            String cleanKey = key.trim().toUpperCase();
            if (cleanKey.equals(fullId) || cleanKey.equals(simpleId) || cleanKey.endsWith(":" + simpleId)) {
                return IngredientTrait.fromConfig(key, itemsSec.getConfigurationSection(key));
            }
        }
        return null;
    }

    /**
     * Mengecek apakah item merupakan Ingredient yang sah terdaftar di trait.yml.
     */
    public boolean isValidIngredient(ItemStack item) {
        return getIngredientTrait(item) != null;
    }

    /**
     * Mengecek apakah item merupakan gear yang sah untuk dilebur di Crusher.
     */
    public boolean isValidCrushItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (isValidFragment(item)) return false;

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;

        String itemType = nbt.getString("MMOITEMS_ITEM_TYPE");
        if (itemType == null || itemType.isEmpty()) itemType = nbt.getType();
        if (itemType == null) return false;

        // Blueprint diizinkan untuk di-crush menjadi fragment!
        boolean isBp = isBlueprint(item);
        if (!isBp) {
            // Mencegah Material biasa, Consumable, Miscellaneous, Gem, atau Block dimasukkan ke crusher
            if (itemType.equalsIgnoreCase("MATERIAL") 
                    || itemType.equalsIgnoreCase("CONSUMABLE") 
                    || itemType.equalsIgnoreCase("MISCELLANEOUS")
                    || itemType.equalsIgnoreCase("GEM_STONE")
                    || itemType.equalsIgnoreCase("BLOCK")) {
                return false;
            }
        }

        String tier = nbt.getString("MMOITEMS_TIER");
        if (tier == null || tier.isEmpty()) tier = nbt.getString("CYAYO_FORGE_TIER");
        if (tier == null || tier.isEmpty()) tier = getTargetTier(nbt);
        if (tier == null || tier.isEmpty()) return false;

        String fragKey = plugin.getConfigManager().getConfig().getString("fragments.mapping." + tier,
                plugin.getConfigManager().getConfig().getString("fragments.default-fragment", "NONE"));
        return !fragKey.equalsIgnoreCase("NONE") && !fragKey.isEmpty();
    }

    /**
     * Mengecek apakah item merupakan target senjata/armor yang sah untuk di-Reroll.
     */
    public boolean isValidRerollTarget(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;

        String itemType = nbt.getString("MMOITEMS_ITEM_TYPE");
        if (itemType == null || itemType.isEmpty()) itemType = nbt.getType();
        if (itemType == null) return false;

        // Mencegah Material, Consumable, Gem, Fragment, atau Blueprint dimasukkan ke reroll
        if (itemType.equalsIgnoreCase("MATERIAL") 
                || itemType.equalsIgnoreCase("CONSUMABLE") 
                || itemType.equalsIgnoreCase("MISCELLANEOUS") 
                || itemType.equalsIgnoreCase("GEM_STONE") 
                || itemType.equalsIgnoreCase("BLOCK")
                || isBlueprint(item) 
                || isValidFragment(item)) {
            return false;
        }

        String tier = nbt.getString("MMOITEMS_TIER");
        if (tier == null || tier.isEmpty()) tier = nbt.getString("CYAYO_FORGE_TIER");
        if (tier == null || tier.isEmpty()) return false;

        String fragKey = plugin.getConfigManager().getConfig().getString("fragments.mapping." + tier,
                plugin.getConfigManager().getConfig().getString("fragments.default-fragment", "NONE"));
        return !fragKey.equalsIgnoreCase("NONE") && !fragKey.isEmpty();
    }

    /**
     * Mengecek apakah item merupakan Fragment yang terdaftar di konfigurasi.
     */
    public boolean isValidFragment(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;

        String actualFragKey = nbt.getString("MMOITEMS_ITEM_TYPE") + ":" + nbt.getString("MMOITEMS_ITEM_ID");
        org.bukkit.configuration.ConfigurationSection mapping = plugin.getConfigManager().getConfig().getConfigurationSection("fragments.mapping");
        if (mapping != null) {
            for (String tier : mapping.getKeys(false)) {
                if (actualFragKey.equalsIgnoreCase(mapping.getString(tier))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper untuk membuat atau mengambil sampel ItemStack MMOItem secara aman berdasarkan format TYPE:ID atau ID.
     */
    public ItemStack getMMOItemSample(String fullKey) {
        if (fullKey == null || fullKey.trim().isEmpty() || fullKey.equalsIgnoreCase("NONE")) return null;
        String typeStr = "MATERIAL";
        String idStr = fullKey.trim();
        if (fullKey.contains(":")) {
            String[] split = fullKey.split(":", 2);
            typeStr = split[0].trim();
            idStr = split[1].trim();
        }
        try {
            Type type = MMOItems.plugin.getTypes().get(typeStr.toUpperCase());
            if (type == null) type = MMOItems.plugin.getTypes().get(typeStr.toLowerCase());
            if (type == null) type = MMOItems.plugin.getTypes().get(typeStr);

            if (type != null) {
                MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, idStr);
                if (template == null) template = MMOItems.plugin.getTemplates().getTemplate(type, idStr.toUpperCase());
                if (template == null) template = MMOItems.plugin.getTemplates().getTemplate(type, idStr.toLowerCase());
                if (template != null) {
                    MMOItem mmo = template.newBuilder(0, null).build();
                    if (mmo != null) {
                        ItemStack it = mmo.newBuilder().build();
                        if (it != null && it.getType() != Material.AIR) return it;
                    }
                }
                ItemStack directItem = MMOItems.plugin.getItem(type, idStr);
                if (directItem != null && directItem.getType() != Material.AIR) return directItem;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Helper untuk membuat ItemStack fragment siap pakai dengan amount tertentu.
     */
    public ItemStack createFragmentItem(String fragKey, int amount) {
        ItemStack sample = getMMOItemSample(fragKey);
        if (sample != null) {
            ItemStack copy = sample.clone();
            copy.setAmount(Math.max(1, amount));
            return copy;
        }
        return null;
    }

    /**
     * Mengambil Display Name yang rapi dari sebuah fragment key (misal "MATERIAL:RARE_WEAPON_ESSENCE").
     */
    public String getFragmentDisplayName(String fragKey) {
        if (fragKey == null || fragKey.isEmpty() || fragKey.equalsIgnoreCase("NONE")) return "§7-";
        ItemStack sample = getMMOItemSample(fragKey);
        if (sample != null && sample.hasItemMeta() && sample.getItemMeta().hasDisplayName()) {
            return sample.getItemMeta().getDisplayName();
        }
        String id = fragKey.contains(":") ? fragKey.split(":", 2)[1] : fragKey;
        return id.replace("_", " ");
    }

    /**
     * Mengecek apakah Fragment cocok dengan Tier senjata target.
     */
    public boolean isMatchingFragment(ItemStack targetItem, ItemStack fragmentItem) {
        if (!isValidRerollTarget(targetItem) || !isValidFragment(fragmentItem)) return false;
        NBTItem targetNbt = NBTItem.get(targetItem);
        NBTItem fragNbt = NBTItem.get(fragmentItem);

        String tier = targetNbt.getString("MMOITEMS_TIER");
        String expectedFragKey = plugin.getConfigManager().getConfig().getString("fragments.mapping." + tier);
        String actualFragKey = fragNbt.getString("MMOITEMS_ITEM_TYPE") + ":" + fragNbt.getString("MMOITEMS_ITEM_ID");
        return actualFragKey.equalsIgnoreCase(expectedFragKey);
    }

    public static class RerollCostInfo {
        public final double baseCost;
        public final double finalCost;
        public final int discountPercent;
        public final String badge;
        public final String displayCost;

        public RerollCostInfo(double baseCost, double finalCost, int discountPercent, String badge, String displayCost) {
            this.baseCost = baseCost;
            this.finalCost = finalCost;
            this.discountPercent = discountPercent;
            this.badge = badge != null ? ChatColor.translateAlternateColorCodes('&', badge) : "";
            this.displayCost = ChatColor.translateAlternateColorCodes('&', displayCost);
        }
    }

    /**
     * Menghitung biaya reroll berdasarkan tier item dan rank/permission diskon yang dimiliki pemain.
     */
    public RerollCostInfo calculateRerollCost(Player player, String tierId) {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig();
        double baseCost = config.getDouble("reroll-cost.costs." + tierId,
                config.getDouble("reroll.cost-per-tier." + tierId,
                        config.getDouble("reroll-cost.default-cost", 1000.0)));

        double finalCost = baseCost;
        int discountPercent = 0;
        String badge = "";

        boolean discountsEnabled = config.getBoolean("reroll-cost.discounts.enabled", true);
        if (discountsEnabled && player != null) {
            org.bukkit.configuration.ConfigurationSection ranksSec = config.getConfigurationSection("reroll-cost.discounts.ranks");
            if (ranksSec != null) {
                for (String key : ranksSec.getKeys(false)) {
                    String perm = ranksSec.getString(key + ".permission", "");
                    if (!perm.isEmpty() && player.hasPermission(perm)) {
                        discountPercent = ranksSec.getInt(key + ".discount-percentage", 0);
                        badge = ranksSec.getString(key + ".badge", "");
                        if (discountPercent > 0) {
                            finalCost = Math.max(0, baseCost * (1.0 - (discountPercent / 100.0)));
                            break;
                        }
                    }
                }
            }
        }

        String baseFormatted = (baseCost == (long) baseCost) ? String.valueOf((long) baseCost) : String.format(java.util.Locale.US, "%.2f", baseCost);
        String finalFormatted = (finalCost == (long) finalCost) ? String.valueOf((long) finalCost) : String.format(java.util.Locale.US, "%.2f", finalCost);

        String displayCost;
        if (discountPercent > 0) {
            String format = config.getString("reroll-cost.discounts.discount-format", "&m%base_cost%&r &a%final_cost% %badge%");
            displayCost = format.replace("%base_cost%", baseFormatted)
                                .replace("%final_cost%", finalFormatted)
                                .replace("%discount_percent%", String.valueOf(discountPercent))
                                .replace("%badge%", badge);
        } else {
            displayCost = baseFormatted;
        }

        return new RerollCostInfo(baseCost, finalCost, discountPercent, badge, displayCost);
    }

    /**
     * Mengecek apakah item merupakan material yang valid berdasarkan config.
     */
    public boolean isValidMaterial(ItemStack item, String categoryKey) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (isBlueprint(item) || isValidFragment(item)) return false;

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;
        
        String mmoType = nbt.getType();
        if (mmoType == null || mmoType.isEmpty()) mmoType = nbt.getString("MMOITEMS_ITEM_TYPE");
        String mmoId = nbt.getString("MMOITEMS_ITEM_ID");
        if (mmoId == null || mmoId.isEmpty()) mmoId = nbt.getString("ITEM_ID");
        
        String fullId = (mmoType != null ? mmoType : "") + ":" + (mmoId != null ? mmoId : "");

        List<String> validTypes = getCategoryStringList(categoryKey, "valid-types");
        List<String> validItems = getCategoryStringList(categoryKey, "valid-items");

        for (String vt : validTypes) {
            if (vt != null && (vt.equalsIgnoreCase(mmoType) || (mmoType != null && vt.equalsIgnoreCase(mmoType.toUpperCase())))) {
                return true;
            }
        }

        for (String vi : validItems) {
            if (vi != null) {
                if (vi.equalsIgnoreCase(fullId) || vi.equalsIgnoreCase(mmoId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Mengecek apakah grup target cocok dengan target item type MMOItems berdasarkan konfigurasi item-groups.
     * Mendukung multi-group dipisahkan koma atau garis miring, contoh: [MELEE,RANGED] atau [SWORD,AXE,BOW].
     * Jika groupTarget adalah "ALL", kosong, atau jika targetType terdaftar di salah satu grup, return true.
     */
    public boolean isGroupTargetMatching(String groupTarget, String targetType) {
        if (groupTarget == null || groupTarget.trim().isEmpty() || groupTarget.equalsIgnoreCase("ALL")) {
            return true;
        }
        if (targetType == null || targetType.trim().isEmpty()) {
            return true;
        }
        String cleanType = targetType.trim().toUpperCase();

        // Pisahkan jika ada multi-group (contoh: "MELEE,RANGED" atau "MELEE/RANGED" atau "SWORD,BOW")
        String[] groups = groupTarget.split("[,/|]+");
        for (String g : groups) {
            String cleanGroup = g.trim().toUpperCase();
            if (cleanGroup.isEmpty()) continue;

            if (cleanGroup.equalsIgnoreCase("ALL") || cleanGroup.equalsIgnoreCase(cleanType)) {
                return true;
            }

            // Cek dari section item-groups di config.yml
            List<String> typesInGroup = plugin.getConfigManager().getConfig().getStringList("item-groups." + cleanGroup);
            if (typesInGroup != null && !typesInGroup.isEmpty()) {
                for (String t : typesInGroup) {
                    if (t.equalsIgnoreCase(cleanType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Membaca daftar bonus stat dari sebuah material MMOItems, disaring berdasarkan targetType item yang sedang ditempa.
     * Mendukung format flat (STAT:10) maupun range (STAT:1-3 atau STAT:1..5), serta filter grup ([MELEE]STAT:1-3).
     */
    public Map<String, id.cyayo.forgev2.model.MaterialStatRange> getMaterialBonusStats(ItemStack item, String targetType) {
        Map<String, id.cyayo.forgev2.model.MaterialStatRange> map = new HashMap<>();
        if (item == null || item.getType() == Material.AIR) return map;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return map;

        String[] possibleKeys = new String[]{"MMOITEMS_FORGE_BONUS_STATS", "FORGE_BONUS_STATS", "forge_bonus_stats"};
        for (String key : possibleKeys) {
            if (nbt.hasTag(key)) {
                try {
                    String raw = nbt.getString(key);
                    if (raw != null && !raw.trim().isEmpty()) {
                        if (raw.startsWith("[") && raw.endsWith("]") && !raw.contains(":")) {
                            raw = raw.substring(1, raw.length() - 1);
                        }
                        String[] entries = raw.split("[,;\n\r]+");
                        for (String entry : entries) {
                            String clean = entry.replace("\"", "").replace("'", "").trim();
                            if (clean.isEmpty()) continue;
                            id.cyayo.forgev2.model.MaterialStatRange range = id.cyayo.forgev2.model.MaterialStatRange.parse(clean);
                            if (range != null && !range.getStatId().isEmpty()) {
                                if (isGroupTargetMatching(range.getGroupTarget(), targetType)) {
                                    map.put(range.getStatId(), range);
                                }
                            }
                        }
                        if (!map.isEmpty()) return map;
                    }
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    public Map<String, id.cyayo.forgev2.model.MaterialStatRange> getMaterialBonusStats(ItemStack item) {
        return getMaterialBonusStats(item, "ALL");
    }

    /**
     * Mengumpulkan dan menggabungkan seluruh rentang bonus stat dari daftar material unik (Akumulasi Min & Max),
     * disaring berdasarkan targetType item yang sedang ditempa.
     * Contoh: Material 1 (HP 1-3) + Material 2 (HP 1-5) => Akumulasi (HP 2-8).
     */
    public Map<String, id.cyayo.forgev2.model.MaterialStatRange> getCombinedBonusStatRanges(List<ItemStack> materials, String targetType) {
        Map<String, double[]> accumulator = new HashMap<>();
        if (materials == null || materials.isEmpty()) return Collections.emptyMap();

        Set<String> processedItemIds = new HashSet<>();
        for (ItemStack mat : materials) {
            if (mat == null || mat.getType() == Material.AIR) continue;
            String matId = MMOItems.getID(mat);
            if (matId == null || processedItemIds.contains(matId.toUpperCase())) {
                continue; // Flat per jenis material unik
            }
            processedItemIds.add(matId.toUpperCase());

            Map<String, id.cyayo.forgev2.model.MaterialStatRange> stats = getMaterialBonusStats(mat, targetType);
            for (Map.Entry<String, id.cyayo.forgev2.model.MaterialStatRange> entry : stats.entrySet()) {
                String statId = entry.getKey();
                id.cyayo.forgev2.model.MaterialStatRange r = entry.getValue();
                double[] current = accumulator.computeIfAbsent(statId, k -> new double[]{0.0, 0.0});
                current[0] += r.getMin();
                current[1] += r.getMax();
            }
        }

        Map<String, id.cyayo.forgev2.model.MaterialStatRange> result = new HashMap<>();
        for (Map.Entry<String, double[]> e : accumulator.entrySet()) {
            result.put(e.getKey(), new id.cyayo.forgev2.model.MaterialStatRange(e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
        return result;
    }

    public Map<String, id.cyayo.forgev2.model.MaterialStatRange> getCombinedBonusStatRanges(List<ItemStack> materials) {
        return getCombinedBonusStatRanges(materials, "ALL");
    }

    /**
     * Melakukan roll acak terhadap hasil akumulasi rentang stat material dan menghitung persentase roll-nya.
     */
    public static class RolledMaterialStats {
        public final Map<String, Double> rolledValues = new HashMap<>();
        public final Map<String, Double> purePercentages = new HashMap<>();
    }

    public RolledMaterialStats rollMaterialBonusStats(Map<String, id.cyayo.forgev2.model.MaterialStatRange> ranges) {
        RolledMaterialStats res = new RolledMaterialStats();
        if (ranges == null || ranges.isEmpty()) return res;

        for (Map.Entry<String, id.cyayo.forgev2.model.MaterialStatRange> entry : ranges.entrySet()) {
            String statId = entry.getKey();
            id.cyayo.forgev2.model.MaterialStatRange r = entry.getValue();
            double rolled = r.roll();
            res.rolledValues.put(statId, rolled);

            if (r.hasRange()) {
                double pct = ((rolled - r.getMin()) / (r.getMax() - r.getMin())) * 100.0;
                pct = Math.max(0.0, Math.min(100.0, pct));
                res.purePercentages.put(statId, pct);
            }
        }
        return res;
    }

    /**
     * Membaca multiplier stat dari material MMOItems.
     */
    public double getMaterialMultiplier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0.0;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return 0.0;
        return nbt.getDouble("MMOITEMS_FORGE_MATERIAL_MULTIPLIER");
    }

    /**
     * Generate preview item berupa item asli yang akan didapat dengan nilai stat ber-roll diubah menjadi min - max.
     */
    /**
     * Generate dummy preview item yang meniru tampilan item asli (Material, DisplayName, CustomModelData, Enchantments, Flags)
     * dengan nilai statistik acak (roll) diganti menjadi rentang min - max secara instan tanpa lag.
     */
    public ItemStack generatePreview(ItemStack blueprint) {
        return generatePreview(null, blueprint, java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    /**
     * Generate dummy preview item yang meniru tampilan item asli (Material, DisplayName, CustomModelData, Enchantments, Flags)
     * serta menyuntikkan perkiraan Crafter, Dominant Material, Final Multiplier, dan Quality Range ke preview lore secara instan.
     */
    public ItemStack generatePreview(Player player, ItemStack blueprint, List<ItemStack> mat1List, List<ItemStack> mat2List, List<ItemStack> mat3List) {
        return generatePreview(player, blueprint, mat1List, mat2List, mat3List, Collections.emptyMap());
    }

    public ItemStack generatePreview(Player player, ItemStack blueprint, List<ItemStack> mat1List, List<ItemStack> mat2List, List<ItemStack> mat3List, Map<Integer, ItemStack> ingredients) {
        if (blueprint == null || blueprint.getType() == Material.AIR) return null;
        try {
            NBTItem nbt = NBTItem.get(blueprint);
            if (!nbt.hasType()) return null;

            String targetType = getTargetType(nbt);
            String targetId = getTargetId(nbt);
            if (targetType.isEmpty() || targetId.isEmpty()) {
                plugin.getLogger().warning("[CyayoForgeV2] Blueprint tidak memiliki target_type atau target_id yang valid!");
                return null;
            }

            Type type = MMOItems.plugin.getTypes().get(targetType);
            if (type == null) type = MMOItems.plugin.getTypes().get(targetType.toUpperCase());
            if (type == null) type = MMOItems.plugin.getTypes().get(targetType.toLowerCase());
            if (type == null) {
                plugin.getLogger().warning("[CyayoForgeV2] MMOItems Type tidak ditemukan: " + targetType);
                return null;
            }

            MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, targetId);
            if (template == null) template = MMOItems.plugin.getTemplates().getTemplate(type, targetId.toUpperCase());
            if (template == null) template = MMOItems.plugin.getTemplates().getTemplate(type, targetId.toLowerCase());
            if (template == null) {
                plugin.getLogger().warning("[CyayoForgeV2] MMOItems Template tidak ditemukan untuk " + type.getId() + ":" + targetId);
                return null;
            }

            // 1. Hitung perkiraan Multiplier, Material Dominan, dan Bonus Stats Material
            java.util.List<ItemStack> materials = new java.util.ArrayList<>();
            if (mat1List != null) materials.addAll(mat1List);
            if (mat2List != null) materials.addAll(mat2List);
            if (mat3List != null) materials.addAll(mat3List);

            double totalMultiplier = 0.0;
            int count = 0;
            String dominantMatId = "NONE";
            String dominantMatDisplayName = "§7(Belum ada)";

            if (!materials.isEmpty()) {
                java.util.Map<String, Integer> materialCounts = new java.util.HashMap<>();
                java.util.Map<String, String> materialNames = new java.util.HashMap<>();
                java.util.Map<String, Double> materialMults = new java.util.HashMap<>();
                for (ItemStack mat : materials) {
                    if (mat == null || mat.getType() == org.bukkit.Material.AIR) continue;
                    String matId = MMOItems.getID(mat);
                    if (matId != null) {
                        materialCounts.put(matId, materialCounts.getOrDefault(matId, 0) + mat.getAmount());
                        if (!materialNames.containsKey(matId)) {
                            String dName = (mat.hasItemMeta() && mat.getItemMeta().hasDisplayName()) 
                                    ? mat.getItemMeta().getDisplayName() 
                                    : matId;
                            materialNames.put(matId, dName);
                            materialMults.put(matId, getMaterialMultiplier(mat));
                        }
                    }
                    totalMultiplier += getMaterialMultiplier(mat) * mat.getAmount();
                    count += mat.getAmount();
                }
                dominantMatId = materialCounts.keySet().stream()
                        .max((id1, id2) -> {
                            int countCompare = Integer.compare(materialCounts.get(id1), materialCounts.get(id2));
                            if (countCompare != 0) return countCompare;
                            return Double.compare(materialMults.getOrDefault(id1, 0.0), materialMults.getOrDefault(id2, 0.0));
                        })
                        .orElse("NONE");
                if (!"NONE".equalsIgnoreCase(dominantMatId)) {
                    dominantMatDisplayName = materialNames.getOrDefault(dominantMatId, dominantMatId);
                }
            }
            double avgMultiplier = count > 0 ? (totalMultiplier / count) : 1.0;
            double formattedMultiplier = Math.round(avgMultiplier * 1000.0) / 1000.0;
            String crafterName = (player != null) ? player.getName() : "§7-";

            String targetTier = getTargetTier(nbt);
            net.Indyuce.mmoitems.api.ItemTier mmoTier = resolveItemTier(targetTier);

            // Kumpulkan Traits dan Abilities dari Ingredients ke Preview
            Map<Integer, IngredientTrait> activeTraits = new HashMap<>();
            if (ingredients != null && !ingredients.isEmpty()) {
                for (Map.Entry<Integer, ItemStack> entry : ingredients.entrySet()) {
                    ItemStack item = entry.getValue();
                    if (item == null || item.getType() == Material.AIR || ItemUtil.isDecoration(item)) continue;
                    IngredientTrait it = getIngredientTrait(item);
                    if (it != null) activeTraits.put(entry.getKey(), it);
                }
            }

            // 2. Generate item dasar dari template MMOItems
            MMOItem mmoItem = template.newBuilder(0, mmoTier).build();

            // Cek apakah Blueprint adalah Shiny Item
            boolean isShiny = isShinyBlueprint(blueprint);
            String previewTrackerLine = null;
            String previewTrackerName = "Unknown Track";
            if (isShiny && plugin.getShinyTrackerManager() != null) {
                previewTrackerName = plugin.getShinyTrackerManager().getPreviewTrackerName();
                previewTrackerLine = plugin.getShinyTrackerManager().formatTrackerString(previewTrackerName, 0);
            }

            int bpPreviewReqLevel = (plugin.getCyayoProfilesHook() != null && plugin.getCyayoProfilesHook().shouldInheritBlueprintLevel())
                    ? plugin.getCyayoProfilesHook().getItemRequiredLevel(blueprint)
                    : 0;

            if (mmoItem != null) {
                if (mmoTier != null) {
                    setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.TIER, new StringData(mmoTier.getId()));
                } else if (!targetTier.isEmpty()) {
                    setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.TIER, new StringData(targetTier));
                }
                if (bpPreviewReqLevel > 0) {
                    plugin.getCyayoProfilesHook().applyRequiredLevelStat(mmoItem, bpPreviewReqLevel, "preview:" + targetType + ":" + targetId);
                }
                setSafeStatData(mmoItem, "FORGE_FINAL_MULTIPLIER", new DoubleData(formattedMultiplier));
                setSafeStatData(mmoItem, "FORGE_CRAFTER", new StringData(crafterName));
                setSafeStatData(mmoItem, "FORGE_DOMINANT_MATERIAL", new StringData(dominantMatDisplayName));
                setSafeStatData(mmoItem, "FORGE_QUALITY", new DoubleData(100.0));

                if (isShiny && previewTrackerLine != null) {
                    setSafeStatData(mmoItem, "FORGE_IS_SHINY", new StringData("true"));
                    setSafeStatData(mmoItem, "FORGE_TRACKER", new StringData(previewTrackerLine));
                    setSafeStatData(mmoItem, "FORGE_TRACKER_NAME", new StringData(previewTrackerName));
                    setSafeStatData(mmoItem, "FORGE_TRACKER_VALUE", new DoubleData(0.0));
                }

                // Suntikkan Bonus Stats Material ke MMOItem Preview (Nilai Rata-rata / Max untuk display awal)
                Map<String, id.cyayo.forgev2.model.MaterialStatRange> bonusRanges = getCombinedBonusStatRanges(materials, targetType);
                for (Map.Entry<String, id.cyayo.forgev2.model.MaterialStatRange> entry : bonusRanges.entrySet()) {
                    try {
                        ItemStat stat = MMOItems.plugin.getStats().get(entry.getKey());
                        if (stat != null) {
                            double currentVal = 0.0;
                            if (mmoItem.hasData(stat) && mmoItem.getData(stat) instanceof DoubleData dd) {
                                currentVal = dd.getValue();
                            }
                            mmoItem.setData(stat, new DoubleData(currentVal + entry.getValue().getMax()));
                        }
                    } catch (Exception ignored) {}
                }

                if (!activeTraits.isEmpty()) {
                    List<Integer> allIngSlots = plugin.getConfigManager().getForgeGui().getIntegerList("slots.ingredients");
                    Map<Integer, Double> effMap = IngredientSpatialUtil.calculateSlotEffectiveness(allIngSlots, activeTraits);
                    boolean isWp = isWeapon(targetType);

                    for (Map.Entry<Integer, IngredientTrait> entry : activeTraits.entrySet()) {
                        int s = entry.getKey();
                        IngredientTrait it = entry.getValue();
                        double eff = effMap.getOrDefault(s, 100.0);
                        double mult = eff / 100.0;

                        for (id.cyayo.forgev2.model.MaterialStatRange r : it.getTraits(isWp)) {
                            if (!isGroupTargetMatching(r.getGroupTarget(), targetType)) continue;
                            String normStatId = r.getStatId().toUpperCase().replace("-", "_");
                            double sMin = r.getMin() * mult;
                            double sMax = r.getMax() * mult;
                            double realMax = Math.max(sMin, sMax);
                            try {
                                ItemStat stat = MMOItems.plugin.getStats().get(normStatId);
                                if (stat != null) {
                                    double curVal = 0.0;
                                    if (mmoItem.hasData(stat) && mmoItem.getData(stat) instanceof DoubleData dd) {
                                        curVal = dd.getValue();
                                    }
                                    mmoItem.setData(stat, new DoubleData(curVal + realMax));
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    List<Integer> sortedSlots = new ArrayList<>(activeTraits.keySet());
                    Collections.sort(sortedSlots);
                    Set<String> claimedModes = new HashSet<>();
                    for (int s : sortedSlots) {
                        IngredientTrait it = activeTraits.get(s);
                        IngredientTrait.TraitAbility ab = it.getAbility(isWp);
                        if (ab == null || ab.getAbilityId().isEmpty()) continue;
                        String mode = ab.getCastMode().toUpperCase();
                        if (mode.equals("ON_HIT") || mode.equals("ON_DAMAGED") || !claimedModes.contains(mode)) {
                            claimedModes.add(mode);
                            injectTraitAbility(mmoItem, ab);
                        }
                    }
                }
            }

            ItemStack sampleItem = null;
            if (mmoItem != null) {
                try {
                    sampleItem = mmoItem.newBuilder().build();
                } catch (Exception ignored) {}
            }

            if (sampleItem == null || sampleItem.getType() == Material.AIR) {
                try {
                    sampleItem = MMOItems.plugin.getItem(type, targetId);
                } catch (Exception ignored) {}
            }

            if (sampleItem == null || sampleItem.getType() == Material.AIR) {
                plugin.getLogger().warning("[CyayoForgeV2] Gagal membuat sampel item preview untuk " + targetType + ":" + targetId);
                return null;
            }

            // 3. Clone item asli sebagai dummy preview
            ItemStack dummyPreview = sampleItem.clone();
            ItemMeta dummyMeta = dummyPreview.getItemMeta();

            if (dummyMeta != null && dummyMeta.hasLore() && dummyMeta.getLore() != null) {
                // Ekstraksi Stat Ranges dari Template & YAML MMOItems
                Set<StatRange> uniqueRanges = new HashSet<>();
                Map<String, StatRange> ranges = extractStatRanges(type, targetId, template, uniqueRanges);

                // Gabungkan dengan Bonus Ranges dari Material yang dimasukkan (disaring berdasarkan targetType)!
                Map<String, id.cyayo.forgev2.model.MaterialStatRange> bonusRanges = getCombinedBonusStatRanges(materials, targetType);
                for (Map.Entry<String, id.cyayo.forgev2.model.MaterialStatRange> entry : bonusRanges.entrySet()) {
                    String statId = entry.getKey().toUpperCase().replace("-", "_");
                    id.cyayo.forgev2.model.MaterialStatRange mr = entry.getValue();

                    StatRange existing = ranges.get(statId.toLowerCase());
                    if (existing != null) {
                        // Jika senjata dasar sudah punya range, tambahkan rentang material ke rentang dasar
                        StatRange combinedRange = new StatRange(statId, existing.statName, existing.min + mr.getMin(), existing.max + mr.getMax(), existing.isPercent);
                        registerStatRange(ranges, uniqueRanges, statId, existing.statName, combinedRange);
                    } else {
                        // Jika stat baru dari material, daftarkan sebagai range baru
                        String statName = statId.replace("_", " ");
                        try {
                            ItemStat mStat = MMOItems.plugin.getStats().get(statId);
                            if (mStat != null && mStat.getName() != null) statName = mStat.getName();
                        } catch (Exception ignored) {}
                        boolean isPct = statId.contains("PERCENT") || statId.contains("CHANCE") || statName.contains("%");
                        StatRange newRange = new StatRange(statId, statName, mr.getMin(), mr.getMax(), isPct);
                        registerStatRange(ranges, uniqueRanges, statId, statName, newRange);
                    }
                }

                // Gabungkan juga dengan Bonus Ranges dari Ingredients yang terpasang!
                if (!activeTraits.isEmpty()) {
                    List<Integer> allIngSlots = plugin.getConfigManager().getForgeGui().getIntegerList("slots.ingredients");
                    Map<Integer, Double> effMap = IngredientSpatialUtil.calculateSlotEffectiveness(allIngSlots, activeTraits);
                    boolean isWp = isWeapon(targetType);

                    for (Map.Entry<Integer, IngredientTrait> entry : activeTraits.entrySet()) {
                        int s = entry.getKey();
                        IngredientTrait it = entry.getValue();
                        double eff = effMap.getOrDefault(s, 100.0);
                        double mult = eff / 100.0;

                        for (id.cyayo.forgev2.model.MaterialStatRange r : it.getTraits(isWp)) {
                            if (!isGroupTargetMatching(r.getGroupTarget(), targetType)) continue;
                            String statId = r.getStatId().toUpperCase().replace("-", "_");
                            double sMin = r.getMin() * mult;
                            double sMax = r.getMax() * mult;
                            double realMin = Math.min(sMin, sMax);
                            double realMax = Math.max(sMin, sMax);

                            StatRange existing = ranges.get(statId.toLowerCase());
                            if (existing != null) {
                                StatRange combinedRange = new StatRange(statId, existing.statName, existing.min + realMin, existing.max + realMax, existing.isPercent);
                                registerStatRange(ranges, uniqueRanges, statId, existing.statName, combinedRange);
                            } else {
                                String statName = statId.replace("_", " ");
                                try {
                                    ItemStat mStat = MMOItems.plugin.getStats().get(statId);
                                    if (mStat != null && mStat.getName() != null) statName = mStat.getName();
                                } catch (Exception ignored) {}
                                boolean isPct = statId.contains("PERCENT") || statId.contains("CHANCE") || statName.contains("%");
                                StatRange newRange = new StatRange(statId, statName, realMin, realMax, isPct);
                                registerStatRange(ranges, uniqueRanges, statId, statName, newRange);
                            }
                        }
                    }
                }

                // Modifikasi hanya nilai angka pada baris lore yang ada
                List<String> newLore = new ArrayList<>();
                for (String line : dummyMeta.getLore()) {
                    if (line == null) continue;
                    String cleanLine = ChatColor.stripColor(line).toLowerCase().trim();
                    StatRange matchedRange = null;

                    // Cek apakah ini baris Quality
                    if (cleanLine.contains("quality") || cleanLine.contains("kesempurnaan") || cleanLine.contains("kualitas")) {
                        if (line.contains(":")) {
                            int idx = line.indexOf(':');
                            String label = line.substring(0, idx + 1);
                            line = label + " §b1.0 - 100.0%";
                        } else {
                            line = line.replaceFirst("([+\\-\\s]*)(\\d+(?:\\.\\d+)?)(%?)", "$11.0 - 100.0%");
                        }
                        newLore.add(line);
                        continue;
                    }

                    // Cari kecocokan stat berdasarkan baris lore asli
                    for (Map.Entry<String, StatRange> entry : ranges.entrySet()) {
                        String key = entry.getKey();
                        if (key.isEmpty()) continue;
                        if (cleanLine.contains(key)) {
                            matchedRange = entry.getValue();
                            break;
                        }
                    }

                    if (matchedRange != null) {
                        String minStr = formatStatValue(matchedRange.min);
                        String maxStr = formatStatValue(matchedRange.max);
                        boolean percent = matchedRange.isPercent || line.contains("%");
                        String rangeText = minStr + " - " + maxStr + (percent ? "%" : "");

                        if (line.contains(":")) {
                            int idx = line.indexOf(':');
                            String label = line.substring(0, idx + 1);
                            String right = line.substring(idx + 1);

                            // Temukan kode warna pertama setelah titik dua
                            String col = "§e";
                            int firstColIdx = right.indexOf('§');
                            if (firstColIdx >= 0 && firstColIdx + 1 < right.length()) {
                                col = "§" + right.charAt(firstColIdx + 1);
                            } else {
                                String lastCol = ChatColor.getLastColors(label);
                                if (lastCol != null && !lastCol.isEmpty()) col = lastCol;
                            }

                            String cleanRight = ChatColor.stripColor(right).trim();
                            String prefix = "";
                            if (cleanRight.startsWith("+")) prefix = "+";
                            else if (cleanRight.startsWith("-")) prefix = "-";

                            line = label + " " + col + prefix + rangeText;
                        } else {
                            line = line.replaceFirst("([+\\-\\s]*)(\\d+(?:\\.\\d+)?)(%?)", "$1" + java.util.regex.Matcher.quoteReplacement(rangeText));
                        }
                    }
                    newLore.add(line);
                }

                dummyMeta.setLore(newLore);
                dummyPreview.setItemMeta(dummyMeta);
            }

            // Injeksi format nama dan lore tracker Shiny ke Dummy Preview jika blueprint Shiny
            if (isShiny && plugin.getShinyTrackerManager() != null) {
                ItemMeta meta = dummyPreview.getItemMeta();
                if (meta != null) {
                    String prefix = plugin.getShinyTrackerManager().getNamePrefix();
                    String suffix = plugin.getShinyTrackerManager().getNameSuffix();
                    String origName = meta.hasDisplayName() ? meta.getDisplayName() : dummyPreview.getType().name();
                    // Timpa warna asli item dengan warna dari prefix shiny
                    String cleanName = ChatColor.stripColor(origName).replaceAll("<[^>]*>", "").trim();
                    meta.setDisplayName(ItemUtil.colorize(prefix) + cleanName + ItemUtil.colorize(suffix));

                    if (previewTrackerLine != null) {
                        List<String> dLore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                        boolean hasTr = false;
                        String cleanTr = ChatColor.stripColor(previewTrackerName).toLowerCase();
                        for (int i = 0; i < dLore.size(); i++) {
                            String dl = dLore.get(i);
                            if (dl != null && (ChatColor.stripColor(dl).toLowerCase().contains(cleanTr) || (dl.contains("★") && dl.contains(":")))) {
                                dLore.set(i, previewTrackerLine);
                                hasTr = true;
                                break;
                            }
                        }
                        if (!hasTr) {
                            dLore.add(previewTrackerLine);
                        }
                        meta.setLore(dLore);
                    }
                    if (plugin.getShinyTrackerManager().isGlowEnabled()) {
                        ItemUtil.applyGlow(meta);
                    }
                    dummyPreview.setItemMeta(meta);
                }
            }

            // 4. Kunci dummy preview sebagai dekorasi agar tidak bisa diambil pemain
            NBTItem previewNbt = NBTItem.get(dummyPreview);
            if (bpPreviewReqLevel > 0) {
                double appliedPrevLvl = previewNbt.hasTag("MMOITEMS_REQUIRED_LEVEL") ? previewNbt.getDouble("MMOITEMS_REQUIRED_LEVEL") : 0.0;
                if (Math.round(appliedPrevLvl) != bpPreviewReqLevel) {
                    previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_REQUIRED_LEVEL", (double) bpPreviewReqLevel));
                }
            }
            if (!targetTier.isEmpty()) {
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_TIER", (mmoTier != null) ? mmoTier.getId() : targetTier));
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_TIER", (mmoTier != null) ? mmoTier.getId() : targetTier));
            }
            if (isShiny) {
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_IS_SHINY", true));
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_IS_SHINY", "true"));
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_ID", "unknown"));
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_NAME", previewTrackerName));
                previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_VALUE", 0));
                if (previewTrackerLine != null) {
                    previewNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER", previewTrackerLine));
                }
            }
            dummyPreview = previewNbt.toItem();
            id.cyayo.forgev2.util.ItemUtil.markAsDecoration(dummyPreview);
            return dummyPreview;
        } catch (Exception e) {
            plugin.getLogger().severe("[CyayoForgeV2] Error saat generatePreview: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Mengeksekusi penempaan item asli.
     */
    public ItemStack craftItem(Player crafter, ItemStack blueprint, java.util.List<ItemStack> mat1List, java.util.List<ItemStack> mat2List, java.util.List<ItemStack> mat3List) {
        return craftItem(crafter, blueprint, mat1List, mat2List, mat3List, Collections.emptyMap());
    }

    public ItemStack craftItem(Player crafter, ItemStack blueprint, java.util.List<ItemStack> mat1List, java.util.List<ItemStack> mat2List, java.util.List<ItemStack> mat3List, Map<Integer, ItemStack> ingredients) {
        // Validasi Blueprint menggunakan API MMOItems
        NBTItem bpNbt = NBTItem.get(blueprint);
        if (!bpNbt.hasType()) return null;

        String targetType = getTargetType(bpNbt);
        String targetId = getTargetId(bpNbt);
        if (targetType.isEmpty() || targetId.isEmpty()) return null;

        net.Indyuce.mmoitems.api.Type type = MMOItems.plugin.getTypes().get(targetType);
        if (type == null) return null;

        net.Indyuce.mmoitems.api.item.template.MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, targetId);
        if (template == null) return null;
        
        // Gabungkan semua material untuk hitungan kualitas & dominan
        java.util.List<ItemStack> materials = new java.util.ArrayList<>();
        materials.addAll(mat1List);
        materials.addAll(mat2List);
        materials.addAll(mat3List);

        // 1. Hitung rata-rata multiplier dari materials
        double totalMultiplier = 0.0;
        int count = 0;
        // 1. Tentukan Material Dominan (Berdasarkan jumlah terbanyak & tie-breaker multiplier tertinggi)
        String dominantMatId = "NONE";
        String dominantMatDisplayName = "NONE";
        if (!materials.isEmpty()) {
            java.util.Map<String, Integer> materialCounts = new java.util.HashMap<>();
            java.util.Map<String, String> materialNames = new java.util.HashMap<>();
            java.util.Map<String, Double> materialMults = new java.util.HashMap<>();
            for (ItemStack mat : materials) {
                if (mat == null || mat.getType() == org.bukkit.Material.AIR) continue;
                String matId = MMOItems.getID(mat);
                if (matId != null) {
                    materialCounts.put(matId, materialCounts.getOrDefault(matId, 0) + mat.getAmount());
                    if (!materialNames.containsKey(matId)) {
                        String dName = (mat.hasItemMeta() && mat.getItemMeta().hasDisplayName()) 
                                ? mat.getItemMeta().getDisplayName() 
                                : matId;
                        materialNames.put(matId, dName);
                        materialMults.put(matId, getMaterialMultiplier(mat));
                    }
                }
                totalMultiplier += getMaterialMultiplier(mat) * mat.getAmount();
                count += mat.getAmount();
            }
            // Cari material dengan jumlah item terbanyak (murni berdasarkan amount)
            dominantMatId = materialCounts.keySet().stream()
                    .max((id1, id2) -> {
                        int countCompare = Integer.compare(materialCounts.get(id1), materialCounts.get(id2));
                        if (countCompare != 0) return countCompare;
                        return Double.compare(materialMults.getOrDefault(id1, 0.0), materialMults.getOrDefault(id2, 0.0));
                    })
                    .orElse("NONE");
            dominantMatDisplayName = materialNames.getOrDefault(dominantMatId, dominantMatId);
        }
        double avgMultiplier = count > 0 ? (totalMultiplier / count) : 1.0;
        if (avgMultiplier <= 0) avgMultiplier = 1.0;
        
        String targetTier = getTargetTier(bpNbt);
        net.Indyuce.mmoitems.api.ItemTier mmoTier = resolveItemTier(targetTier);

        // 2. Generate item dasar dari template dengan Tier
        MMOItem mmoItem = template.newBuilder(0, mmoTier).build();
        
        // 3. Ekstraksi Stat Ranges dan Rekam Persentase Roll Murni SEBELUM dikalikan multiplier
        Set<StatRange> uniqueRanges = new HashSet<>();
        Map<String, StatRange> ranges = extractStatRanges(type, targetId, template, uniqueRanges);
        Map<String, Double> purePercentages = new HashMap<>();

        for (StatRange sr : uniqueRanges) {
            try {
                ItemStat stat = MMOItems.plugin.getStats().get(sr.statId);
                if (stat != null && mmoItem.hasData(stat)) {
                    Object sData = mmoItem.getData(stat);
                    if (sData instanceof DoubleData dd) {
                        double rawVal = dd.getValue();
                        if (Math.abs(sr.max - sr.min) > 0.001) {
                            double pct = ((rawVal - sr.min) / (sr.max - sr.min)) * 100.0;
                            pct = Math.max(0.0, Math.min(100.0, pct));
                            purePercentages.put(sr.statId, pct);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 4. Terapkan Multiplier dari Material ke Stat yang Didaftarkan di config.yml (multiplied-stats)
        if (avgMultiplier > 0 && Math.abs(avgMultiplier - 1.0) > 0.001) {
            List<String> multStats = plugin.getConfigManager().getConfig().getStringList("multiplied-stats");
            if (multStats.isEmpty()) {
                multStats = java.util.Arrays.asList("ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_STRIKE_CHANCE", "CRITICAL_STRIKE_POWER", "ARMOR", "ARMOR_TOUGHNESS", "MAX_HEALTH");
            }
            for (String statId : multStats) {
                try {
                    ItemStat stat = MMOItems.plugin.getStats().get(statId);
                    if (stat != null && mmoItem.hasData(stat)) {
                        Object sData = mmoItem.getData(stat);
                        if (sData instanceof DoubleData dd) {
                            mmoItem.setData(stat, new DoubleData(dd.getValue() * avgMultiplier));
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // 4b. Terapkan Bonus Stats Khusus dari Material (Roll Rentang Kumulatif disaring berdasarkan targetType & Rekam Persentase)
        Map<String, id.cyayo.forgev2.model.MaterialStatRange> bonusRanges = getCombinedBonusStatRanges(materials, targetType);
        RolledMaterialStats rolledMatStats = rollMaterialBonusStats(bonusRanges);

        for (Map.Entry<String, Double> entry : rolledMatStats.rolledValues.entrySet()) {
            try {
                ItemStat stat = MMOItems.plugin.getStats().get(entry.getKey());
                if (stat != null) {
                    double currentVal = 0.0;
                    if (mmoItem.hasData(stat) && mmoItem.getData(stat) instanceof DoubleData dd) {
                        currentVal = dd.getValue();
                    }
                    mmoItem.setData(stat, new DoubleData(currentVal + entry.getValue()));
                }
            } catch (Exception ignored) {}
        }

        // Gabungkan persentase roll murni dari material ke purePercentages senjata
        for (Map.Entry<String, Double> pEntry : rolledMatStats.purePercentages.entrySet()) {
            purePercentages.put(pEntry.getKey(), pEntry.getValue());
        }

        // 4c. Terapkan Bonus Stats Traits dari Ingredients (Dikalikan efektivitas spasial)
        Map<Integer, IngredientTrait> activeTraits = new HashMap<>();
        Map<Integer, String> ingredientDisplayNames = new HashMap<>();
        if (ingredients != null && !ingredients.isEmpty()) {
            for (Map.Entry<Integer, ItemStack> entry : ingredients.entrySet()) {
                ItemStack item = entry.getValue();
                if (item == null || item.getType() == Material.AIR || ItemUtil.isDecoration(item)) continue;
                IngredientTrait it = getIngredientTrait(item);
                if (it != null) {
                    activeTraits.put(entry.getKey(), it);
                    String dName = (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) 
                            ? item.getItemMeta().getDisplayName() 
                            : it.getItemId();
                    ingredientDisplayNames.put(entry.getKey(), dName);
                }
            }
        }

        List<Integer> allIngSlots = plugin.getConfigManager().getForgeGui().getIntegerList("slots.ingredients");
        Map<Integer, Double> effMap = IngredientSpatialUtil.calculateSlotEffectiveness(allIngSlots, activeTraits);
        boolean isWp = isWeapon(targetType);

        Map<String, double[]> traitStatsAccumulator = new HashMap<>();
        for (Map.Entry<Integer, IngredientTrait> entry : activeTraits.entrySet()) {
            int s = entry.getKey();
            IngredientTrait it = entry.getValue();
            double eff = effMap.getOrDefault(s, 100.0);
            double mult = eff / 100.0;

            for (id.cyayo.forgev2.model.MaterialStatRange r : it.getTraits(isWp)) {
                if (!isGroupTargetMatching(r.getGroupTarget(), targetType)) continue;
                double sMin = r.getMin() * mult;
                double sMax = r.getMax() * mult;
                double realMin = Math.min(sMin, sMax);
                double realMax = Math.max(sMin, sMax);

                String normStatId = r.getStatId().toUpperCase().replace("-", "_");
                double[] acc = traitStatsAccumulator.computeIfAbsent(normStatId, k -> new double[]{0.0, 0.0});
                acc[0] += realMin;
                acc[1] += realMax;
            }
        }

        Map<String, Double> rolledTraits = new HashMap<>();
        for (Map.Entry<String, double[]> entry : traitStatsAccumulator.entrySet()) {
            String statId = entry.getKey().toUpperCase().replace("-", "_");
            double min = entry.getValue()[0];
            double max = entry.getValue()[1];
            double rolledVal;
            if (Math.abs(max - min) < 0.001) {
                rolledVal = min;
            } else {
                rolledVal = min + (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * (max - min));
            }
            rolledVal = Math.round(rolledVal * 10.0) / 10.0;
            rolledTraits.put(statId, rolledVal);

            try {
                ItemStat stat = MMOItems.plugin.getStats().get(statId);
                if (stat != null) {
                    double currentVal = 0.0;
                    if (mmoItem.hasData(stat) && mmoItem.getData(stat) instanceof DoubleData dd) {
                        currentVal = dd.getValue();
                    }
                    mmoItem.setData(stat, new DoubleData(currentVal + rolledVal));
                }
            } catch (Exception ignored) {}
        }

        // Resolusi Ability sesuai aturan tabrakan cast-mode
        List<Integer> sortedSlots = new ArrayList<>(activeTraits.keySet());
        Collections.sort(sortedSlots);
        Set<String> claimedModes = new HashSet<>();
        List<IngredientTrait.TraitAbility> selectedAbilities = new ArrayList<>();

        for (int s : sortedSlots) {
            IngredientTrait it = activeTraits.get(s);
            IngredientTrait.TraitAbility ab = it.getAbility(isWp);
            if (ab == null || ab.getAbilityId().isEmpty()) continue;
            String mode = ab.getCastMode().toUpperCase();
            if (mode.equals("ON_HIT") || mode.equals("ON_DAMAGED")) {
                selectedAbilities.add(ab);
            } else if (!claimedModes.contains(mode)) {
                claimedModes.add(mode);
                selectedAbilities.add(ab);
            }
        }

        for (IngredientTrait.TraitAbility ab : selectedAbilities) {
            injectTraitAbility(mmoItem, ab);
        }
        
        // 5. Kalkulasi Quality Asli Senjata (Rata-rata dari seluruh persentase roll murni stat)
        double realQuality = 100.0;
        if (!purePercentages.isEmpty()) {
            double sum = 0.0;
            for (double p : purePercentages.values()) {
                sum += p;
            }
            realQuality = Math.round((sum / purePercentages.size()) * 10.0) / 10.0;
        }

        // Bulatkan multiplier ke 3 desimal
        double formattedMultiplier = Math.round(avgMultiplier * 1000.0) / 1000.0;
        
        // 6. Suntik Custom Stats Kosmetik, Crafter, Dominant Material, Multiplier, dan Tier
        if (mmoTier != null) {
            setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.TIER, new StringData(mmoTier.getId()));
        } else if (!targetTier.isEmpty()) {
            setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.TIER, new StringData(targetTier));
        }
        int bpReqLevel = (plugin.getCyayoProfilesHook() != null && plugin.getCyayoProfilesHook().shouldInheritBlueprintLevel())
                ? plugin.getCyayoProfilesHook().getItemRequiredLevel(blueprint)
                : 0;
        if (bpReqLevel > 0) {
            plugin.getCyayoProfilesHook().applyRequiredLevelStat(mmoItem, bpReqLevel, "craft:" + targetType + ":" + targetId);
        }
        setSafeStatData(mmoItem, "FORGE_FINAL_MULTIPLIER", new DoubleData(formattedMultiplier));
        setSafeStatData(mmoItem, "FORGE_QUALITY", new DoubleData(realQuality));
        setSafeStatData(mmoItem, "FORGE_CRAFTER", new StringData(crafter.getName()));
        setSafeStatData(mmoItem, "FORGE_REROLL_COUNT", new DoubleData(0));
        if (!"NONE".equalsIgnoreCase(dominantMatDisplayName) && !dominantMatDisplayName.isEmpty()) {
            setSafeStatData(mmoItem, "FORGE_DOMINANT_MATERIAL", new StringData(dominantMatDisplayName));
        }

        // Cek apakah Blueprint adalah Shiny Item
        boolean isShiny = isShinyBlueprint(blueprint);
        id.cyayo.forgev2.model.ShinyTracker rolledTracker = null;
        String rolledTrackerLine = null;
        if (isShiny && plugin.getShinyTrackerManager() != null) {
            rolledTracker = plugin.getShinyTrackerManager().rollRandomTracker(isWp, targetType);
            if (rolledTracker != null) {
                rolledTrackerLine = plugin.getShinyTrackerManager().formatTrackerString(rolledTracker.getDisplayName(), 0);
                setSafeStatData(mmoItem, "FORGE_IS_SHINY", new StringData("true"));
                setSafeStatData(mmoItem, "FORGE_TRACKER", new StringData(rolledTrackerLine));
                setSafeStatData(mmoItem, "FORGE_TRACKER_NAME", new StringData(rolledTracker.getDisplayName()));
                setSafeStatData(mmoItem, "FORGE_TRACKER_VALUE", new DoubleData(0.0));
            }
        }
        
        // 7. Injeksi Passive Ability dari Dominant Material ID (jika ada)
        injectPassiveAbility(mmoItem, dominantMatId, type.getId());

        ItemStack result = mmoItem.newBuilder().build();
        
        // Simpan Data Winspect per kategori
        String mat1Str = serializeMats(mat1List);
        String mat2Str = serializeMats(mat2List);
        String mat3Str = serializeMats(mat3List);

        // Serialize traits, ingredients, abilities
        StringBuilder traitsSb = new StringBuilder();
        for (Map.Entry<String, Double> entry : rolledTraits.entrySet()) {
            if (traitsSb.length() > 0) traitsSb.append(";");
            traitsSb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        String traitsStr = traitsSb.toString();

        StringBuilder ingSb = new StringBuilder();
        for (int s : sortedSlots) {
            if (ingSb.length() > 0) ingSb.append(", ");
            String dName = ingredientDisplayNames.getOrDefault(s, "Ingredient");
            double eff = effMap.getOrDefault(s, 100.0);
            ingSb.append(dName).append(" (").append((long) eff).append("%)");
        }
        String ingStr = ingSb.toString();

        StringBuilder abSb = new StringBuilder();
        for (IngredientTrait.TraitAbility ab : selectedAbilities) {
            if (abSb.length() > 0) abSb.append(";");
            abSb.append(ab.getAbilityId()).append(":").append(ab.getCastMode());
            if (ab.getModifiers() != null && !ab.getModifiers().isEmpty()) {
                abSb.append(":");
                boolean first = true;
                for (Map.Entry<String, Double> me : ab.getModifiers().entrySet()) {
                    if (!first) abSb.append(",");
                    abSb.append(me.getKey()).append("=").append(me.getValue());
                    first = false;
                }
            }
        }
        String abStr = abSb.toString();
        
        // Gunakan NBTItem untuk menyuntik data String rahasia yang tidak perlu tampil di lore
        io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(result);
        if (bpReqLevel > 0) {
            double appliedLvl = nbt.hasTag("MMOITEMS_REQUIRED_LEVEL") ? nbt.getDouble("MMOITEMS_REQUIRED_LEVEL") : 0.0;
            if (Math.round(appliedLvl) != bpReqLevel) {
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_REQUIRED_LEVEL", (double) bpReqLevel));
            }
        }
        if (!targetTier.isEmpty()) {
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_TIER", (mmoTier != null) ? mmoTier.getId() : targetTier));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_TIER", (mmoTier != null) ? mmoTier.getId() : targetTier));
        }
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_CRAFTER", crafter.getName()));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_DOMINANT", dominantMatDisplayName));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_QUALITY", realQuality));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_FINAL_MULTIPLIER", formattedMultiplier));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_REROLL_COUNT", 0));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_DATE", String.valueOf(System.currentTimeMillis())));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_MATS_1", mat1Str));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_MATS_2", mat2Str));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_MATS_3", mat3Str));
        if (traitsStr != null && !traitsStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_TRAITS", traitsStr));
        if (ingStr != null && !ingStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_INGREDIENTS", ingStr));
        if (abStr != null && !abStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_ABILITIES", abStr));

        if (isShiny) {
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_IS_SHINY", true));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_IS_SHINY", "true"));
            if (rolledTracker != null) {
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_ID", rolledTracker.getId()));
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_NAME", rolledTracker.getDisplayName()));
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_VALUE", 0));
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER", rolledTrackerLine));
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER_NAME", rolledTracker.getDisplayName()));
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER_VALUE", 0.0));
            }
        }
        result = nbt.toItem();

        ItemStack crafted = injectLore(result, type, targetId, template, purePercentages, formattedMultiplier, realQuality);
        if (isShiny && plugin.getShinyTrackerManager() != null) {
            ItemMeta resMeta = crafted.getItemMeta();
            if (resMeta != null) {
                String prefix = plugin.getShinyTrackerManager().getNamePrefix();
                String suffix = plugin.getShinyTrackerManager().getNameSuffix();
                String origName = resMeta.hasDisplayName() ? resMeta.getDisplayName() : crafted.getType().name();
                // Timpa warna asli item dengan warna dari prefix shiny
                String cleanName = ChatColor.stripColor(origName).replaceAll("<[^>]*>", "").trim();
                resMeta.setDisplayName(ItemUtil.colorize(prefix) + cleanName + ItemUtil.colorize(suffix));

                if (rolledTrackerLine != null) {
                    List<String> resLore = resMeta.hasLore() && resMeta.getLore() != null ? resMeta.getLore() : new ArrayList<>();
                    boolean hasTr = false;
                    String cleanTr = ChatColor.stripColor(rolledTracker.getDisplayName()).toLowerCase();
                    for (int i = 0; i < resLore.size(); i++) {
                        String rl = resLore.get(i);
                        if (rl != null && ChatColor.stripColor(rl).toLowerCase().contains(cleanTr)) {
                            resLore.set(i, rolledTrackerLine);
                            hasTr = true;
                            break;
                        }
                    }
                    if (!hasTr) {
                        resLore.add(rolledTrackerLine);
                    }
                    resMeta.setLore(resLore);
                }
                if (plugin.getShinyTrackerManager().isGlowEnabled()) {
                    ItemUtil.applyGlow(resMeta);
                }
                crafted.setItemMeta(resMeta);
            }
        }
        return crafted;
    }

    /**
     * Me-reroll item tempaan: HANYA mengacak ulang statistik yang memiliki rentang Min-Max,
     * mempertahankan Crafter, Dominant Material, Multiplier, Tier, Passive Abilities, dan Lore Injector.
     */
    public ItemStack rerollItem(ItemStack targetItem) {
        return rerollItem(null, targetItem);
    }

    public ItemStack rerollItem(Player player, ItemStack targetItem) {
        if (targetItem == null || targetItem.getType() == Material.AIR) return null;
        NBTItem targetNbt = NBTItem.get(targetItem);
        if (!targetNbt.hasType()) return null;

        String typeStr = targetNbt.getString("MMOITEMS_ITEM_TYPE");
        String targetId = targetNbt.getString("MMOITEMS_ITEM_ID");
        if (typeStr.isEmpty() || targetId.isEmpty()) return null;

        Type type = MMOItems.plugin.getTypes().get(typeStr);
        if (type == null) return null;

        MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, targetId);
        if (template == null) return null;

        // 1. Ambil data jejak penempaan CyayoForge dari NBT dan Custom Stats
        String crafter = targetNbt.getString("CYAYO_FORGE_CRAFTER");
        if (crafter.isEmpty()) crafter = targetNbt.getString("FORGE_CRAFTER");

        String dominant = targetNbt.getString("CYAYO_FORGE_DOMINANT");
        if (dominant.isEmpty()) dominant = targetNbt.getString("FORGE_DOMINANT_MATERIAL");

        double finalMultiplier = 0.0;
        if (targetNbt.hasTag("CYAYO_FORGE_FINAL_MULTIPLIER")) {
            try {
                finalMultiplier = targetNbt.getDouble("CYAYO_FORGE_FINAL_MULTIPLIER");
            } catch (Exception e) {
                try {
                    finalMultiplier = Double.parseDouble(targetNbt.getString("CYAYO_FORGE_FINAL_MULTIPLIER"));
                } catch (Exception ignored) {}
            }
        }
        if (finalMultiplier <= 0 && targetNbt.hasTag("FORGE_FINAL_MULTIPLIER")) {
            try {
                finalMultiplier = targetNbt.getDouble("FORGE_FINAL_MULTIPLIER");
            } catch (Exception e) {
                try {
                    finalMultiplier = Double.parseDouble(targetNbt.getString("FORGE_FINAL_MULTIPLIER"));
                } catch (Exception ignored) {}
            }
        }
        if (finalMultiplier <= 0 && targetNbt.hasTag("MMOITEMS_FORGE_FINAL_MULTIPLIER")) {
            try {
                finalMultiplier = targetNbt.getDouble("MMOITEMS_FORGE_FINAL_MULTIPLIER");
            } catch (Exception e) {
                try {
                    finalMultiplier = Double.parseDouble(targetNbt.getString("MMOITEMS_FORGE_FINAL_MULTIPLIER"));
                } catch (Exception ignored) {}
            }
        }
        if (finalMultiplier <= 0) finalMultiplier = 1.0;

        String tier = targetNbt.getString("CYAYO_FORGE_TIER");
        if (tier == null || tier.isEmpty()) tier = targetNbt.getString("MMOITEMS_TIER");
        if (tier == null || tier.isEmpty()) tier = getTargetTier(targetNbt);
        net.Indyuce.mmoitems.api.ItemTier mmoTier = resolveItemTier(tier);
        int inheritedReqLevel = (plugin.getCyayoProfilesHook() != null && plugin.getCyayoProfilesHook().shouldInheritBlueprintLevel())
                ? plugin.getCyayoProfilesHook().getItemRequiredLevel(targetItem)
                : 0;

        // Ambil jumlah total reroll & pity counter sebelumnya
        int totalRerollCount = 0;
        if (targetNbt.hasTag("CYAYO_FORGE_REROLL_COUNT")) {
            totalRerollCount = targetNbt.getInteger("CYAYO_FORGE_REROLL_COUNT");
        } else if (targetNbt.hasTag("FORGE_REROLL_COUNT")) {
            try {
                totalRerollCount = (int) Math.round(targetNbt.getDouble("FORGE_REROLL_COUNT"));
            } catch (Exception ignored) {}
        }
        int newTotalRerollCount = totalRerollCount + 1;

        // Ambil data status Shiny dan capaian Stat Tracker sebelumnya
        boolean isShiny = targetNbt.hasTag("CYAYO_FORGE_IS_SHINY") && targetNbt.getBoolean("CYAYO_FORGE_IS_SHINY");
        if (!isShiny && targetNbt.hasTag("MMOITEMS_FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(targetNbt.getString("MMOITEMS_FORGE_IS_SHINY"))) {
            isShiny = true;
        }
        if (!isShiny && targetNbt.hasTag("FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(targetNbt.getString("FORGE_IS_SHINY"))) {
            isShiny = true;
        }

        String shinyTrackerId = "";
        String shinyTrackerName = "";
        long shinyTrackerValue = 0;

        if (isShiny) {
            shinyTrackerId = targetNbt.getString("CYAYO_SHINY_TRACKER_ID");
            shinyTrackerName = targetNbt.getString("CYAYO_SHINY_TRACKER_NAME");
            if (shinyTrackerName.isEmpty()) shinyTrackerName = targetNbt.getString("MMOITEMS_FORGE_TRACKER_NAME");
            if (shinyTrackerName.isEmpty() && plugin.getShinyTrackerManager() != null) {
                id.cyayo.forgev2.model.ShinyTracker tDef = plugin.getShinyTrackerManager().getTrackerById(shinyTrackerId);
                if (tDef != null) shinyTrackerName = tDef.getDisplayName();
            }
            if (shinyTrackerName.isEmpty()) shinyTrackerName = shinyTrackerId;

            if (targetNbt.hasTag("CYAYO_SHINY_TRACKER_VALUE")) {
                try {
                    shinyTrackerValue = targetNbt.getInteger("CYAYO_SHINY_TRACKER_VALUE");
                } catch (Exception e) {
                    try {
                        shinyTrackerValue = (long) Math.round(targetNbt.getDouble("CYAYO_SHINY_TRACKER_VALUE"));
                    } catch (Exception ignored) {}
                }
            } else if (targetNbt.hasTag("MMOITEMS_FORGE_TRACKER_VALUE")) {
                try {
                    shinyTrackerValue = (long) Math.round(targetNbt.getDouble("MMOITEMS_FORGE_TRACKER_VALUE"));
                } catch (Exception ignored) {}
            }
        }

        int prevPityCount = 0;
        if (targetNbt.hasTag("CYAYO_FORGE_PITY_COUNT")) {
            prevPityCount = targetNbt.getInteger("CYAYO_FORGE_PITY_COUNT");
        } else if (targetNbt.hasTag("FORGE_PITY_COUNT")) {
            try {
                prevPityCount = (int) Math.round(targetNbt.getDouble("FORGE_PITY_COUNT"));
            } catch (Exception ignored) {}
        }
        int currentPityCount = prevPityCount + 1;

        String dateStr = targetNbt.getString("CYAYO_FORGE_DATE");
        String mat1Str = targetNbt.getString("CYAYO_FORGE_MATS_1");
        String mat2Str = targetNbt.getString("CYAYO_FORGE_MATS_2");
        String mat3Str = targetNbt.getString("CYAYO_FORGE_MATS_3");
        String traitsStr = targetNbt.getString("CYAYO_FORGE_TRAITS");
        String ingStr = targetNbt.getString("CYAYO_FORGE_INGREDIENTS");
        String abStr = targetNbt.getString("CYAYO_FORGE_ABILITIES");

        // 2. Generate MMOItem baru dari template dengan Tier asli
        MMOItem mmoItem = template.newBuilder(0, mmoTier).build();

        // 3. Ekstraksi Stat Ranges yang memiliki rentang Min-Max (RNG stats)
        Set<StatRange> uniqueRanges = new HashSet<>();
        Map<String, StatRange> ranges = extractStatRanges(type, targetId, template, uniqueRanges);
        Map<String, Double> purePercentages = new HashMap<>();

        List<String> multStats = plugin.getConfigManager().getConfig().getStringList("multiplied-stats");
        if (multStats.isEmpty()) {
            multStats = java.util.Arrays.asList("ATTACK_DAMAGE", "MAGIC_DAMAGE", "CRITICAL_STRIKE_CHANCE", "CRITICAL_STRIKE_POWER", "ARMOR", "ARMOR_TOUGHNESS", "MAX_HEALTH");
        }

        // Pity System Calculation
        boolean pityEnabled = plugin.getConfigManager().getConfig().getBoolean("reroll-pity.enabled", true);
        double pityBoostPerReroll = plugin.getConfigManager().getConfig().getDouble("reroll-pity.pity-boost-per-reroll", 2.5);
        double maxPityFloor = plugin.getConfigManager().getConfig().getDouble("reroll-pity.max-pity-floor", 90.0);
        int guaranteedMaxAt = plugin.getConfigManager().getConfig().getInt("reroll-pity.guaranteed-max-at", 30);

        // Desperate Hard Pity (One-Time Milestone Jackpot)
        boolean desperateEnabled = plugin.getConfigManager().getConfig().getBoolean("reroll-pity.desperate-hard-pity.enabled", false);
        int desperateAt = plugin.getConfigManager().getConfig().getInt("reroll-pity.desperate-hard-pity.at-reroll", 30);
        Double desperateFixedPercent = null;
        String desperateBroadcastMsg = null;

        if (pityEnabled && desperateEnabled && newTotalRerollCount == desperateAt) {
            org.bukkit.configuration.ConfigurationSection tiersSec = plugin.getConfigManager().getConfig().getConfigurationSection("reroll-pity.desperate-hard-pity.tiers");
            if (tiersSec != null) {
                for (String tKey : tiersSec.getKeys(false)) {
                    org.bukkit.configuration.ConfigurationSection tSec = tiersSec.getConfigurationSection(tKey);
                    if (tSec == null) continue;
                    double chance = tSec.getDouble("chance", 0.0);
                    if (chance > 0 && (Math.random() * 100.0) <= chance) {
                        desperateFixedPercent = tSec.getDouble("all-stats-percent", 100.0);
                        desperateBroadcastMsg = tSec.getString("broadcast", "");
                        break; // Ambil tier jackpot pertama yang berhasil kena chance
                    }
                }
            }
        }

        double rollFloorRatio = 0.0;
        boolean triggerGuaranteedStat = false;
        if (pityEnabled) {
            if (guaranteedMaxAt > 0 && currentPityCount >= guaranteedMaxAt) {
                triggerGuaranteedStat = true;
            } else {
                double floorPct = Math.min(maxPityFloor, currentPityCount * pityBoostPerReroll);
                rollFloorRatio = Math.max(0.0, Math.min(0.99, floorPct / 100.0));
            }
        }

        // Kumpulkan daftar stat yang memiliki rentang RNG (min != max)
        List<StatRange> rngStats = new ArrayList<>();
        for (StatRange sr : uniqueRanges) {
            if (Math.abs(sr.max - sr.min) > 0.001) {
                rngStats.add(sr);
            }
        }

        // Tentukan index stat yang dijamin 100% jika mencapai standard hard pity
        int guaranteedIndex = -1;
        if (triggerGuaranteedStat && !rngStats.isEmpty() && desperateFixedPercent == null) {
            guaranteedIndex = (int) (Math.random() * rngStats.size());
        }

        boolean gotAnyHundredPercent = false;

        // 4. HANYA reroll stats yang memiliki rentang Min-Max (min != max)
        for (int i = 0; i < rngStats.size(); i++) {
            StatRange sr = rngStats.get(i);

            double newRawVal;
            double pct;

            if (desperateFixedPercent != null) {
                // Desperate Jackpot Hit: Berikan nilai persentase serentak ke seluruh stats
                pct = Math.max(0.0, Math.min(100.0, desperateFixedPercent));
                newRawVal = sr.min + ((pct / 100.0) * (sr.max - sr.min));
                if (!sr.isPercent) {
                    newRawVal = Math.round(newRawVal * 10.0) / 10.0;
                }
                if (pct >= 99.99 || Math.abs(newRawVal - sr.max) <= 0.0001) {
                    pct = 100.0;
                    gotAnyHundredPercent = true;
                }
            } else if (i == guaranteedIndex) {
                // Standard Hard Pity Hit: Langsung tetapkan ke nilai maksimal template stat ini
                newRawVal = sr.max;
                if (!sr.isPercent) {
                    newRawVal = Math.round(newRawVal * 10.0) / 10.0;
                }
                pct = 100.0;
                gotAnyHundredPercent = true;
            } else {
                // Roll nilai acak baru di antara [sr.min, sr.max] dengan memperhitungkan Pity Floor
                double rollRatio = rollFloorRatio + (Math.random() * (1.0 - rollFloorRatio));
                newRawVal = sr.min + (rollRatio * (sr.max - sr.min));
                if (!sr.isPercent) {
                    newRawVal = Math.round(newRawVal * 10.0) / 10.0;
                }

                // Pastikan tidak melebihi batas max akibat pembulatan
                if (newRawVal > sr.max) newRawVal = sr.max;
                if (newRawVal < sr.min) newRawVal = sr.min;

                pct = ((newRawVal - sr.min) / (sr.max - sr.min)) * 100.0;
                pct = Math.max(0.0, Math.min(100.0, pct));

                if (pct >= 99.99 || Math.abs(newRawVal - sr.max) <= 0.0001) {
                    pct = 100.0;
                    gotAnyHundredPercent = true;
                }
            }

            purePercentages.put(sr.statId, pct);

            // Terapkan Multiplier jika stat terdaftar di multiplied-stats
            boolean isMultiplied = multStats.contains(sr.statId);
            double finalVal = (isMultiplied && finalMultiplier > 0 && Math.abs(finalMultiplier - 1.0) > 0.001)
                    ? (newRawVal * finalMultiplier)
                    : newRawVal;

            try {
                ItemStat stat = MMOItems.plugin.getStats().get(sr.statId);
                if (stat != null) {
                    mmoItem.setData(stat, new DoubleData(finalVal));
                }
            } catch (Exception ignored) {}
        }

        // Broadcast khusus jika memicu Desperate Jackpot
        if (desperateFixedPercent != null && desperateBroadcastMsg != null && !desperateBroadcastMsg.isEmpty()) {
            String pName = (player != null) ? player.getName() : crafter;
            String itemName = targetItem.hasItemMeta() && targetItem.getItemMeta().hasDisplayName() ? targetItem.getItemMeta().getDisplayName() : targetId;
            String broadcastFormatted = desperateBroadcastMsg
                    .replace("%player%", pName)
                    .replace("%item%", itemName);
            org.bukkit.Bukkit.broadcastMessage(ItemUtil.colorize(broadcastFormatted));
        }

        // Jika player mendapatkan 100% pada salah satu stat, reset pity count ke 0 (tapi total reroll tetap tersimpan)
        int newPityCount = (pityEnabled && gotAnyHundredPercent) ? 0 : currentPityCount;

        // 5. Hitung Quality baru berdasarkan hasil roll murni baru
        double newQuality = 100.0;
        if (!purePercentages.isEmpty()) {
            double sum = 0.0;
            for (double p : purePercentages.values()) {
                sum += p;
            }
            newQuality = Math.round((sum / purePercentages.size()) * 10.0) / 10.0;
        }

        // 6. Pertahankan seluruh metadata penempaan, Crafter, Dominant Material, Multiplier, Tier, dan Reroll Count
        double formattedMultiplier = Math.round(finalMultiplier * 1000.0) / 1000.0;

        if (mmoTier != null) {
            setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.TIER, new StringData(mmoTier.getId()));
        } else if (tier != null && !tier.isEmpty()) {
            setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.TIER, new StringData(tier));
        }
        if (inheritedReqLevel > 0) {
            plugin.getCyayoProfilesHook().applyRequiredLevelStat(mmoItem, inheritedReqLevel, "reroll:" + typeStr + ":" + targetId);
        }

        setSafeStatData(mmoItem, "FORGE_FINAL_MULTIPLIER", new DoubleData(formattedMultiplier));
        setSafeStatData(mmoItem, "FORGE_QUALITY", new DoubleData(newQuality));
        setSafeStatData(mmoItem, "FORGE_REROLL_COUNT", new DoubleData(newTotalRerollCount));
        setSafeStatData(mmoItem, "FORGE_PITY_COUNT", new DoubleData(newPityCount));
        if (!crafter.isEmpty()) {
            setSafeStatData(mmoItem, "FORGE_CRAFTER", new StringData(crafter));
        }
        if (!dominant.isEmpty() && !"NONE".equalsIgnoreCase(dominant)) {
            setSafeStatData(mmoItem, "FORGE_DOMINANT_MATERIAL", new StringData(dominant));
        }

        if (isShiny && plugin.getShinyTrackerManager() != null) {
            String trackerLine = plugin.getShinyTrackerManager().formatTrackerString(shinyTrackerName, shinyTrackerValue);
            setSafeStatData(mmoItem, "FORGE_IS_SHINY", new StringData("true"));
            setSafeStatData(mmoItem, "FORGE_TRACKER", new StringData(trackerLine));
            setSafeStatData(mmoItem, "FORGE_TRACKER_NAME", new StringData(shinyTrackerName));
            setSafeStatData(mmoItem, "FORGE_TRACKER_VALUE", new DoubleData((double) shinyTrackerValue));
        }

        // 7. Jika ada dominant material, pastikan ability pasif terinjeksi jika belum ada
        if (!dominant.isEmpty() && !"NONE".equalsIgnoreCase(dominant)) {
            injectPassiveAbility(mmoItem, dominant, type.getId());
        }

        // 7b. Terapkan kembali traits dari ingredients yang terkunci
        if (traitsStr != null && !traitsStr.trim().isEmpty()) {
            applyStoredTraits(mmoItem, traitsStr);
        }
        // 7c. Terapkan kembali abilities dari ingredients
        if (abStr != null && !abStr.trim().isEmpty()) {
            reinjectAbilities(mmoItem, abStr);
        }

        ItemStack result = mmoItem.newBuilder().build();

        // 8. Suntikkan kembali seluruh NBT tag rahasia CyayoForge
        io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(result);
        if (inheritedReqLevel > 0) {
            double appliedLvl = nbt.hasTag("MMOITEMS_REQUIRED_LEVEL") ? nbt.getDouble("MMOITEMS_REQUIRED_LEVEL") : 0.0;
            if (Math.round(appliedLvl) != inheritedReqLevel) {
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_REQUIRED_LEVEL", (double) inheritedReqLevel));
            }
        }
        if (tier != null && !tier.isEmpty()) {
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_TIER", (mmoTier != null) ? mmoTier.getId() : tier));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_TIER", (mmoTier != null) ? mmoTier.getId() : tier));
        }
        if (!crafter.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_CRAFTER", crafter));
        if (!dominant.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_DOMINANT", dominant));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_QUALITY", newQuality));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_FINAL_MULTIPLIER", formattedMultiplier));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_REROLL_COUNT", newTotalRerollCount));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_PITY_COUNT", newPityCount));
        if (!dateStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_DATE", dateStr));
        if (!mat1Str.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_MATS_1", mat1Str));
        if (!mat2Str.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_MATS_2", mat2Str));
        if (!mat3Str.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_MATS_3", mat3Str));
        if (traitsStr != null && !traitsStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_TRAITS", traitsStr));
        if (ingStr != null && !ingStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_INGREDIENTS", ingStr));
        if (abStr != null && !abStr.isEmpty()) nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_ABILITIES", abStr));

        if (isShiny) {
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_FORGE_IS_SHINY", true));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_IS_SHINY", "true"));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_ID", shinyTrackerId));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_NAME", shinyTrackerName));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYO_SHINY_TRACKER_VALUE", (int) shinyTrackerValue));
            if (plugin.getShinyTrackerManager() != null) {
                String trackerLine = plugin.getShinyTrackerManager().formatTrackerString(shinyTrackerName, shinyTrackerValue);
                nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER", trackerLine));
            }
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER_NAME", shinyTrackerName));
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("MMOITEMS_FORGE_TRACKER_VALUE", (double) shinyTrackerValue));
        }
        result = nbt.toItem();

        // 9. Suntikkan lore dengan persentase roll baru dan format lore injector
        ItemStack rerolled = injectLore(result, type, targetId, template, purePercentages, formattedMultiplier, newQuality);
        if (isShiny && plugin.getShinyTrackerManager() != null) {
            ItemMeta resMeta = rerolled.getItemMeta();
            if (resMeta != null) {
                String prefix = plugin.getShinyTrackerManager().getNamePrefix();
                String suffix = plugin.getShinyTrackerManager().getNameSuffix();
                String origName = resMeta.hasDisplayName() ? resMeta.getDisplayName() : rerolled.getType().name();
                // Bersihkan warna asli item dan buang prefix lama jika sudah ada agar warna prefix baru menimpa
                String cleanName = ChatColor.stripColor(origName).replaceAll("<[^>]*>", "").trim();
                String cleanPrefix = ChatColor.stripColor(ItemUtil.colorize(prefix)).trim();
                if (cleanName.startsWith(cleanPrefix)) {
                    cleanName = cleanName.substring(cleanPrefix.length()).trim();
                }
                resMeta.setDisplayName(ItemUtil.colorize(prefix) + cleanName + ItemUtil.colorize(suffix));

                String trackerLine = plugin.getShinyTrackerManager().formatTrackerString(shinyTrackerName, shinyTrackerValue);
                List<String> resLore = resMeta.hasLore() && resMeta.getLore() != null ? resMeta.getLore() : new ArrayList<>();
                boolean hasTr = false;
                String cleanTr = ChatColor.stripColor(shinyTrackerName).toLowerCase();
                for (int i = 0; i < resLore.size(); i++) {
                    String rl = resLore.get(i);
                    if (rl != null && ChatColor.stripColor(rl).toLowerCase().contains(cleanTr)) {
                        resLore.set(i, trackerLine);
                        hasTr = true;
                        break;
                    }
                }
                if (!hasTr) {
                    resLore.add(trackerLine);
                }
                resMeta.setLore(resLore);
                if (plugin.getShinyTrackerManager().isGlowEnabled()) {
                    ItemUtil.applyGlow(resMeta);
                }
                rerolled.setItemMeta(resMeta);
            }
        }
        return rerolled;
    }

    private void applyStoredTraits(MMOItem mmoItem, String traitsStr) {
        if (mmoItem == null || traitsStr == null || traitsStr.trim().isEmpty()) return;
        String[] entries = traitsStr.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                String statId = parts[0].trim().toUpperCase().replace("-", "_");
                try {
                    double val = Double.parseDouble(parts[1].trim());
                    ItemStat stat = MMOItems.plugin.getStats().get(statId);
                    if (stat != null) {
                        double curVal = 0.0;
                        if (mmoItem.hasData(stat) && mmoItem.getData(stat) instanceof DoubleData dd) {
                            curVal = dd.getValue();
                        }
                        mmoItem.setData(stat, new DoubleData(curVal + val));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void reinjectAbilities(MMOItem mmoItem, String abilitiesStr) {
        if (mmoItem == null || abilitiesStr == null || abilitiesStr.trim().isEmpty()) return;
        String[] entries = abilitiesStr.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":", 3);
            if (parts.length >= 2) {
                String abId = parts[0].trim();
                String cMode = parts[1].trim();
                Map<String, Double> mods = new HashMap<>();
                if (parts.length == 3 && !parts[2].trim().isEmpty()) {
                    for (String modEntry : parts[2].split(",")) {
                        String[] mParts = modEntry.split("=", 2);
                        if (mParts.length == 2) {
                            try {
                                mods.put(mParts[0].trim().toLowerCase(), Double.parseDouble(mParts[1].trim()));
                            } catch (Exception ignored) {}
                        }
                    }
                }
                injectTraitAbility(mmoItem, new IngredientTrait.TraitAbility(abId, cMode, mods));
            }
        }
    }

    
    private String serializeMats(java.util.List<ItemStack> mats) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (ItemStack mat : mats) {
            if (mat == null || mat.getType() == org.bukkit.Material.AIR) continue;
            String id = MMOItems.getID(mat);
            if (id != null) {
                net.Indyuce.mmoitems.api.Type mmoType = MMOItems.getType(mat);
                String typeStr = (mmoType != null) ? mmoType.getId() : "";
                String key = typeStr.isEmpty() ? id : (typeStr + "/" + id);
                counts.put(key, counts.getOrDefault(key, 0) + mat.getAmount());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Membandingkan hasil stat item yang digenerate dengan Template Min-Max asli untuk mendapat % Perfection.
     */
    public double calculateRealPerfection(String typeId, String itemId, MMOItem mmoItem) {
        if (mmoItem == null) return 100.0;
        net.Indyuce.mmoitems.api.Type type = MMOItems.plugin.getTypes().get(typeId);
        if (type == null) return 100.0;
        
        net.Indyuce.mmoitems.api.item.template.MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, itemId);
        if (template == null) return 100.0;

        Set<StatRange> uniqueRanges = new HashSet<>();
        Map<String, StatRange> ranges = extractStatRanges(type, itemId, template, uniqueRanges);
        if (ranges.isEmpty()) return 100.0;

        double sum = 0.0;
        int count = 0;
        for (StatRange sr : uniqueRanges) {
            try {
                ItemStat stat = MMOItems.plugin.getStats().get(sr.statId);
                if (stat != null && mmoItem.hasData(stat)) {
                    Object sData = mmoItem.getData(stat);
                    if (sData instanceof DoubleData dd) {
                        double rawVal = dd.getValue();
                        if (Math.abs(sr.max - sr.min) > 0.001) {
                            double pct = ((rawVal - sr.min) / (sr.max - sr.min)) * 100.0;
                            sum += Math.max(0.0, Math.min(100.0, pct));
                            count++;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return count > 0 ? (Math.round((sum / count) * 10.0) / 10.0) : 100.0;
    }

    /**
     * Menyuntikkan skill/pasif ke dalam mmoItem jika material dominannya punya konfigurasi pasif di abilities.yml.
     */
    private void injectPassiveAbility(MMOItem mmoItem, String dominantMatId, String targetType) {
        if (mmoItem == null || dominantMatId == null || dominantMatId.isEmpty() || dominantMatId.equalsIgnoreCase("NONE")) return;

        org.bukkit.configuration.file.FileConfiguration abilitiesConfig = plugin.getConfigManager().getAbilities();
        if (abilitiesConfig == null) return;

        org.bukkit.configuration.ConfigurationSection sec = abilitiesConfig.getConfigurationSection("abilities." + dominantMatId);
        if (sec == null) sec = abilitiesConfig.getConfigurationSection("abilities.MATERIAL:" + dominantMatId);
        if (sec == null) {
            org.bukkit.configuration.ConfigurationSection allAbilities = abilitiesConfig.getConfigurationSection("abilities");
            if (allAbilities != null) {
                for (String key : allAbilities.getKeys(false)) {
                    if (key.equalsIgnoreCase(dominantMatId) || key.endsWith(":" + dominantMatId) || dominantMatId.endsWith(":" + key)) {
                        sec = allAbilities.getConfigurationSection(key);
                        break;
                    }
                }
            }
        }
        if (sec == null) return;
        
        String category = isWeapon(targetType) ? "WEAPON" : "ARMOR";
        org.bukkit.configuration.ConfigurationSection abilitySection = sec.getConfigurationSection(category);
        
        if (abilitySection != null) {
            String abilityId = abilitySection.getString("ability-id");
            if (abilityId != null && !abilityId.isEmpty()) {
                try {
                    io.lumine.mythic.lib.skill.handler.SkillHandler<?> handler = io.lumine.mythic.lib.MythicLib.plugin.getSkills().getHandler(abilityId);
                    if (handler != null) {
                        String castMode = abilitySection.getString("cast-mode", "ON_HIT");
                        io.lumine.mythic.lib.skill.trigger.TriggerType trigger = null;
                        try {
                            trigger = io.lumine.mythic.lib.skill.trigger.TriggerType.valueOf(castMode.toUpperCase().replace(" ", "_"));
                        } catch (Exception ignored) {}
                        if (trigger == null) {
                            java.util.Collection<io.lumine.mythic.lib.skill.trigger.TriggerType> types = io.lumine.mythic.lib.skill.trigger.TriggerType.values();
                            if (types != null && !types.isEmpty()) {
                                trigger = types.iterator().next();
                            }
                        }
                        if (trigger == null) return;

                        net.Indyuce.mmoitems.stat.data.AbilityData abilityData = new net.Indyuce.mmoitems.stat.data.AbilityData(handler, trigger);

                        org.bukkit.configuration.ConfigurationSection modSec = abilitySection.getConfigurationSection("modifiers");
                        if (modSec != null) {
                            for (String mKey : modSec.getKeys(false)) {
                                abilityData.setModifier(mKey, modSec.getDouble(mKey));
                            }
                        }

                        net.Indyuce.mmoitems.stat.data.AbilityListData list = new net.Indyuce.mmoitems.stat.data.AbilityListData();
                        if (mmoItem.hasData(net.Indyuce.mmoitems.ItemStats.ABILITIES)) {
                            list = (net.Indyuce.mmoitems.stat.data.AbilityListData) mmoItem.getData(net.Indyuce.mmoitems.ItemStats.ABILITIES);
                        }
                        list.add(abilityData);
                        setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.ABILITIES, list);
                        plugin.getLogger().info("[CyayoForgeV2] Berhasil menyuntikkan ability " + abilityId + " dari material dominan " + dominantMatId + " ke kategori " + category);
                    } else {
                        plugin.getLogger().warning("[CyayoForgeV2] Ability handler MythicLib tidak ditemukan: " + abilityId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[CyayoForgeV2] Gagal menerapkan ability " + abilityId + " dari material dominan: " + e.getMessage());
                }
            }
        }
    }

    public boolean isWeapon(String type) {
        if (type == null || type.trim().isEmpty()) return true;
        String clean = type.trim().toUpperCase();
        if (isGroupTargetMatching("ARMOR", clean)) return false;
        if (isGroupTargetMatching("MELEE", clean) || isGroupTargetMatching("RANGED", clean) || isGroupTargetMatching("MAGIC", clean)) {
            return true;
        }
        List<String> armors = java.util.Arrays.asList("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SHIELD", "ARMOR");
        return !armors.contains(clean);
    }

    public void injectTraitAbility(MMOItem mmoItem, IngredientTrait.TraitAbility ability) {
        if (mmoItem == null || ability == null) return;
        String abilityId = ability.getAbilityId();
        if (abilityId == null || abilityId.isEmpty()) return;

        try {
            io.lumine.mythic.lib.skill.handler.SkillHandler<?> handler = io.lumine.mythic.lib.MythicLib.plugin.getSkills().getHandler(abilityId);
            if (handler != null) {
                io.lumine.mythic.lib.skill.trigger.TriggerType trigger = null;
                try {
                    trigger = io.lumine.mythic.lib.skill.trigger.TriggerType.valueOf(ability.getCastMode().toUpperCase().replace(" ", "_"));
                } catch (Exception ignored) {}
                if (trigger == null) {
                    java.util.Collection<io.lumine.mythic.lib.skill.trigger.TriggerType> types = io.lumine.mythic.lib.skill.trigger.TriggerType.values();
                    if (types != null && !types.isEmpty()) {
                        trigger = types.iterator().next();
                    }
                }
                if (trigger == null) return;

                net.Indyuce.mmoitems.stat.data.AbilityData abilityData = new net.Indyuce.mmoitems.stat.data.AbilityData(handler, trigger);
                for (Map.Entry<String, Double> mEntry : ability.getModifiers().entrySet()) {
                    abilityData.setModifier(mEntry.getKey(), mEntry.getValue());
                }

                net.Indyuce.mmoitems.stat.data.AbilityListData list = new net.Indyuce.mmoitems.stat.data.AbilityListData();
                if (mmoItem.hasData(net.Indyuce.mmoitems.ItemStats.ABILITIES)) {
                    list = (net.Indyuce.mmoitems.stat.data.AbilityListData) mmoItem.getData(net.Indyuce.mmoitems.ItemStats.ABILITIES);
                }
                list.add(abilityData);
                setSafeStatData(mmoItem, net.Indyuce.mmoitems.ItemStats.ABILITIES, list);
                plugin.getLogger().info("[CyayoForgeV2] Berhasil menyuntikkan trait ability: " + abilityId + " (" + ability.getCastMode() + ")");
            } else {
                plugin.getLogger().warning("[CyayoForgeV2] Trait ability handler MythicLib tidak ditemukan: " + abilityId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CyayoForgeV2] Gagal menerapkan trait ability " + abilityId + ": " + e.getMessage());
        }
    }

    public ItemStack injectLore(ItemStack item, Type type, String targetId, MMOItemTemplate template, double quality) {
        return injectLore(item, type, targetId, template, null, 1.0, quality);
    }

    /**
     * Melakukan Injeksi persentase roll murni ke setiap baris lore statistik pada item hasil akhir tempaan
     */
    public ItemStack injectLore(ItemStack item, Type type, String targetId, MMOItemTemplate template, Map<String, Double> purePercentages, double multiplier, double quality) {
        if (item == null || item.getType() == Material.AIR) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return item;

        Set<StatRange> uniqueRanges = new HashSet<>();
        Map<String, StatRange> ranges = extractStatRanges(type, targetId, template, uniqueRanges);

        // Ambil konfigurasi format dari config.yml
        org.bukkit.configuration.ConfigurationSection formatSec = plugin.getConfigManager().getConfig().getConfigurationSection("lore-injector-formats");
        java.util.TreeMap<Integer, String> formats = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        if (formatSec != null) {
            for (String k : formatSec.getKeys(false)) {
                try {
                    int thresh = Integer.parseInt(k);
                    formats.put(thresh, formatSec.getString(k));
                } catch (Exception ignored) {}
            }
        }
        if (formats.isEmpty()) {
            formats.put(100, " &6[%percentage%% Max]");
            formats.put(90, " &8[&a%percentage%%&8]");
            formats.put(70, " &8[&e%percentage%%&8]");
            formats.put(40, " &8[&6%percentage%%&8]");
            formats.put(0, " &8[&c%percentage%%&8]");
        }

        List<String> newLore = new ArrayList<>();
        java.util.regex.Pattern valPattern = java.util.regex.Pattern.compile("([+\\-]?)(\\d+(?:\\.\\d+)?)");

        for (String line : meta.getLore()) {
            if (line == null) continue;
            String cleanLine = ChatColor.stripColor(line).toLowerCase().trim();
            StatRange matchedRange = null;

            for (Map.Entry<String, StatRange> entry : ranges.entrySet()) {
                String key = entry.getKey();
                if (key.isEmpty()) continue;
                if (cleanLine.contains(key)) {
                    matchedRange = entry.getValue();
                    break;
                }
            }

            if (matchedRange != null && Math.abs(matchedRange.max - matchedRange.min) > 0.001) {
                // Bersihkan HANYA tag persentase bracketed di ujung baris jika ada (misal akibat reroll)
                // JANGAN menghapus angka nilai stat asli seperti +4%
                line = line.replaceAll("\\s*§8\\[.*?\\%[^\\]]*\\]$", "")
                           .replaceAll("\\s*\\[.*?\\%[^\\]]*\\]$", "")
                           .replaceAll("\\s*§6\\[.*?Max\\]$", "")
                           .replaceAll("\\s*\\[.*?Max\\]$", "");

                Double precomputedPct = (purePercentages != null) ? purePercentages.get(matchedRange.statId) : null;
                double pct = 0.0;
                boolean validPct = false;

                if (precomputedPct != null) {
                    pct = precomputedPct;
                    validPct = true;
                } else {
                    // Ekstrak nilai angka aktual yang ada di baris lore ini
                    String valPart = line;
                    if (line.contains(":")) {
                        valPart = line.substring(line.indexOf(':') + 1);
                    }
                    String cleanVal = ChatColor.stripColor(valPart).trim();
                    java.util.regex.Matcher m = valPattern.matcher(cleanVal);
                    if (m.find()) {
                        try {
                            String sign = m.group(1);
                            double actualVal = Double.parseDouble(m.group(2));
                            if ("-".equals(sign)) actualVal = -actualVal;

                            double effMultiplier = multiplier > 0 ? multiplier : 1.0;
                            try {
                                NBTItem itemNbt = NBTItem.get(item);
                                if (itemNbt.hasType()) {
                                    double mVal = itemNbt.getDouble("FORGE_FINAL_MULTIPLIER");
                                    if (mVal > 0) effMultiplier = mVal;
                                }
                            } catch (Exception ignored) {}

                            // Kembalikan ke nilai roll dasar sebelum terkena multiplier
                            double baseVal = (effMultiplier > 0) ? (actualVal / effMultiplier) : actualVal;

                            pct = ((baseVal - matchedRange.min) / (matchedRange.max - matchedRange.min)) * 100.0;
                            pct = Math.max(0.0, Math.min(100.0, pct));
                            validPct = true;
                        } catch (Exception ignored) {}
                    }
                }

                if (validPct) {
                    String pctStr = (pct == (long) pct) ? String.valueOf((long) pct) : String.format(java.util.Locale.US, "%.1f", pct);

                    // Cari format tag yang sesuai dari config.yml (tetap mempertahankan format asli termasuk kurung siku &8[&e...&8])
                    String tagTemplate = " &8[&e%percentage%%&8]";
                    for (Map.Entry<Integer, String> fmtEntry : formats.entrySet()) {
                        if (pct >= fmtEntry.getKey()) {
                            tagTemplate = fmtEntry.getValue();
                            break;
                        }
                    }

                    // Parse format warna MiniMessage/ColorCodes
                    String tag = ItemUtil.colorize(tagTemplate.replace("%percentage%", pctStr));

                    line = line + tag;
                }
            } else if (cleanLine.contains("quality") || cleanLine.contains("kesempurnaan")) {
                // Baris kustom stat Quality milik plugin (misal: "★ Quality: 98.4%")
                // Format angka persentasenya dengan warna dinamis dari formatQualityPercentage tanpa kurung siku
                String coloredQuality = formatQualityPercentage(quality);
                int colonIdx = line.indexOf(':');
                if (colonIdx != -1) {
                    String prefix = line.substring(0, colonIdx + 1);
                    line = prefix + " " + coloredQuality;
                }
            }
            newLore.add(line);
        }
        
        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }

    public static class StatRange {
        public final String statId;
        public final String statName;
        public final double min;
        public final double max;
        public final boolean isPercent;

        public StatRange(String statId, String statName, double min, double max, boolean isPercent) {
            this.statId = statId;
            this.statName = statName;
            this.min = min;
            this.max = max;
            this.isPercent = isPercent;
        }
    }

    private Map<String, StatRange> extractStatRanges(Type type, String targetId, MMOItemTemplate template, Set<StatRange> uniqueRanges) {
        Map<String, StatRange> ranges = new HashMap<>();

        // 1. Coba baca dari Type ConfigFile / File YAML MMOItems
        try {
            org.bukkit.configuration.file.FileConfiguration yaml = null;
            try {
                java.lang.reflect.Method getCfgMethod = type.getClass().getMethod("getConfigFile");
                Object cfgFileObj = getCfgMethod.invoke(type);
                if (cfgFileObj != null) {
                    java.lang.reflect.Method getConfig = cfgFileObj.getClass().getMethod("getConfig");
                    Object res = getConfig.invoke(cfgFileObj);
                    if (res instanceof org.bukkit.configuration.file.FileConfiguration fc) {
                        yaml = fc;
                    }
                }
            } catch (Exception ignored) {}

            if (yaml == null) {
                java.io.File file = new java.io.File(MMOItems.plugin.getDataFolder(), "item/" + type.getId() + ".yml");
                if (!file.exists()) {
                    file = new java.io.File(MMOItems.plugin.getDataFolder(), "item/" + type.getId().toLowerCase() + ".yml");
                }
                if (!file.exists()) {
                    file = new java.io.File(MMOItems.plugin.getDataFolder(), "item/" + type.getId().toUpperCase() + ".yml");
                }
                if (file.exists()) {
                    yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                }
            }

            if (yaml != null) {
                org.bukkit.configuration.ConfigurationSection itemSec = yaml.getConfigurationSection(targetId);
                if (itemSec == null) itemSec = yaml.getConfigurationSection(targetId.toUpperCase());
                if (itemSec == null) itemSec = yaml.getConfigurationSection(targetId.toLowerCase());

                if (itemSec != null) {
                    parseSectionForRanges(itemSec, ranges, uniqueRanges);
                    org.bukkit.configuration.ConfigurationSection baseSec = itemSec.getConfigurationSection("base");
                    if (baseSec != null) {
                        parseSectionForRanges(baseSec, ranges, uniqueRanges);
                    }
                    org.bukkit.configuration.ConfigurationSection modSec = itemSec.getConfigurationSection("modifiers");
                    if (modSec != null) {
                        parseSectionForRanges(modSec, ranges, uniqueRanges);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CyayoForgeV2] Gagal membaca YAML config MMOItems: " + e.getMessage());
        }

        // 2. Coba deep inspection pada template.getBaseItemData()
        if (template != null) {
            parseFromTemplateData(template, ranges, uniqueRanges);
        }

        return ranges;
    }

    private void parseSectionForRanges(org.bukkit.configuration.ConfigurationSection sec, Map<String, StatRange> ranges, Set<StatRange> uniqueRanges) {
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            if (key.equalsIgnoreCase("material") || key.equalsIgnoreCase("name") || key.equalsIgnoreCase("lore")
                    || key.equalsIgnoreCase("tier") || key.equalsIgnoreCase("custom-model-data") || key.equalsIgnoreCase("base")
                    || key.equalsIgnoreCase("modifiers") || key.equalsIgnoreCase("option") || key.equalsIgnoreCase("type")) {
                continue;
            }

            String statId = key.toUpperCase().replace("-", "_");
            String statDisplayName = key.replace("-", " ");
            try {
                ItemStat stat = MMOItems.plugin.getStats().get(statId);
                if (stat != null && stat.getName() != null && !stat.getName().isEmpty()) {
                    statDisplayName = stat.getName();
                }
            } catch (Exception ignored) {}

            Object valObj = sec.get(key);
            StatRange sr = parseStatRangeFromObject(statId, statDisplayName, valObj);
            if (sr != null) {
                registerStatRange(ranges, uniqueRanges, statId, statDisplayName, sr);
            }
        }
    }

    private StatRange parseStatRangeFromObject(String statId, String statName, Object rawValue) {
        if (rawValue == null) return null;

        double min = 0;
        double max = 0;
        boolean hasRange = false;
        boolean isPercent = statId.toUpperCase().contains("PERCENT") 
                || statId.toUpperCase().contains("CHANCE") 
                || statId.toUpperCase().contains("POWER") 
                || statId.toUpperCase().contains("RATE")
                || (statName != null && statName.contains("%"));

        if (rawValue instanceof org.bukkit.configuration.ConfigurationSection sec) {
            Double base = sec.contains("base") ? sec.getDouble("base") : null;
            Double spread = sec.contains("spread") ? sec.getDouble("spread") : null;
            Double add = sec.contains("add") ? sec.getDouble("add") : 0.0;
            Double cfgMin = sec.contains("min") ? sec.getDouble("min") : null;
            Double cfgMax = sec.contains("max") ? sec.getDouble("max") : null;

            if (base != null && spread != null && spread > 0) {
                min = (cfgMin != null) ? cfgMin : Math.max(0, base * (1.0 - spread) + add);
                max = (cfgMax != null) ? cfgMax : (base * (1.0 + spread) + add);
                hasRange = (Math.abs(min - max) > 0.001);
            } else if (cfgMin != null && cfgMax != null) {
                min = cfgMin;
                max = cfgMax;
                hasRange = (Math.abs(min - max) > 0.001);
            }
        } else if (rawValue instanceof java.util.List<?> list && list.size() >= 2) {
            try {
                min = Double.parseDouble(String.valueOf(list.get(0)).trim());
                max = Double.parseDouble(String.valueOf(list.get(1)).trim());
                hasRange = (Math.abs(min - max) > 0.001);
            } catch (Exception ignored) {}
        } else if (rawValue instanceof String str) {
            str = str.trim();
            if (str.contains("..")) {
                String[] split = str.split("\\.\\.");
                if (split.length == 2) {
                    try {
                        min = Double.parseDouble(split[0].replace("+", "").trim());
                        max = Double.parseDouble(split[1].replace("+", "").trim());
                        hasRange = (Math.abs(min - max) > 0.001);
                    } catch (Exception ignored) {}
                }
            } else if (str.contains("+-")) {
                String[] split = str.split("\\+-");
                if (split.length == 2) {
                    try {
                        double b = Double.parseDouble(split[0].replace("+", "").trim());
                        double s = Double.parseDouble(split[1].replace("+", "").trim());
                        min = b - s;
                        max = b + s;
                        hasRange = (Math.abs(min - max) > 0.001);
                    } catch (Exception ignored) {}
                }
            } else if (str.contains("-") && !str.startsWith("-")) {
                String[] split = str.split("-");
                if (split.length == 2) {
                    try {
                        min = Double.parseDouble(split[0].replace("+", "").trim());
                        max = Double.parseDouble(split[1].replace("+", "").trim());
                        hasRange = (Math.abs(min - max) > 0.001);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (hasRange) {
            return new StatRange(statId, statName, min, max, isPercent);
        }
        return null;
    }

    private void parseFromTemplateData(MMOItemTemplate template, Map<String, StatRange> ranges, Set<StatRange> uniqueRanges) {
        try {
            java.lang.reflect.Method getBaseDataMethod = null;
            for (java.lang.reflect.Method m : template.getClass().getMethods()) {
                if (m.getName().equals("getBaseItemData") && m.getParameterCount() == 0) {
                    getBaseDataMethod = m;
                    break;
                }
            }
            if (getBaseDataMethod != null) {
                Object baseDataObj = getBaseDataMethod.invoke(template);
                if (baseDataObj instanceof java.util.Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        Object statObj = entry.getKey();
                        Object dataObj = entry.getValue();
                        if (statObj instanceof ItemStat stat && dataObj != null) {
                            String statId = stat.getId();
                            if (statId == null || statId.startsWith("FORGE_")) continue;

                            Double base = getFieldValue(dataObj, "base", "getBase");
                            Double spread = getFieldValue(dataObj, "spread", "getSpread");
                            Double min = getFieldValue(dataObj, "min", "getMin");
                            Double max = getFieldValue(dataObj, "max", "getMax");
                            Double add = getFieldValue(dataObj, "add", "getAdd");
                            if (add == null) add = 0.0;

                            double calcMin = 0.0;
                            double calcMax = 0.0;
                            boolean hasRange = false;

                            if (base != null && spread != null && spread > 0) {
                                calcMin = (min != null) ? min : Math.max(0, base * (1.0 - spread) + add);
                                calcMax = (max != null) ? max : (base * (1.0 + spread) + add);
                                hasRange = (Math.abs(calcMin - calcMax) > 0.001);
                            } else if (min != null && max != null) {
                                calcMin = min;
                                calcMax = max;
                                hasRange = (Math.abs(calcMin - calcMax) > 0.001);
                            }

                            if (hasRange) {
                                String rawName = null;
                                try {
                                    rawName = stat.getName();
                                } catch (Exception ignored) {}
                                String statDisplayName = (rawName != null && !rawName.trim().isEmpty()) ? rawName : statId;
                                boolean isPercent = statId.contains("PERCENT") 
                                        || statId.contains("CHANCE") 
                                        || statId.contains("POWER") 
                                        || statId.contains("RATE") 
                                        || (rawName != null && rawName.contains("%"));
                                StatRange sr = new StatRange(statId, statDisplayName, calcMin, calcMax, isPercent);
                                registerStatRange(ranges, uniqueRanges, statId, rawName, sr);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void registerStatRange(Map<String, StatRange> ranges, Set<StatRange> uniqueRanges, String statId, String rawName, StatRange sr) {
        uniqueRanges.add(sr);
        ranges.put(statId.toLowerCase().trim(), sr);
        ranges.put(statId.toLowerCase().replace("_", "-").trim(), sr);
        ranges.put(statId.toLowerCase().replace("_", " ").trim(), sr);

        if (rawName != null && !rawName.trim().isEmpty()) {
            String stripped = ChatColor.stripColor(rawName).toLowerCase().trim();
            if (!stripped.isEmpty()) {
                ranges.put(stripped, sr);
                ranges.put(stripped.replace(" ", "-"), sr);
                ranges.put(stripped.replace("-", " "), sr);
                ranges.put(stripped.replace("%", "").trim(), sr);
            }
        }

        // Cek juga dari registry stat MMOItems jika ada nama terjemahan lain
        try {
            ItemStat mmoStat = MMOItems.plugin.getStats().get(statId);
            if (mmoStat != null && mmoStat.getName() != null) {
                String sName = ChatColor.stripColor(mmoStat.getName()).toLowerCase().trim();
                if (!sName.isEmpty()) {
                    ranges.put(sName, sr);
                    ranges.put(sName.replace(" ", "-"), sr);
                    ranges.put(sName.replace("-", " "), sr);
                    ranges.put(sName.replace("%", "").trim(), sr);
                }
            }
        } catch (Exception ignored) {}

        // Alias Kamus Bahasa Indonesia & Inggris Terpadu
        String upper = statId.toUpperCase();
        if (upper.contains("ATTACK_DAMAGE") || upper.equals("DAMAGE")) {
            ranges.put("damage", sr);
            ranges.put("attack damage", sr);
            ranges.put("attack-damage", sr);
            ranges.put("physical damage", sr);
            ranges.put("serangan", sr);
            ranges.put("kerusakan", sr);
            ranges.put("serangan fisik", sr);
            ranges.put("daya serang", sr);
        } else if (upper.contains("ATTACK_SPEED")) {
            ranges.put("attack speed", sr);
            ranges.put("attack-speed", sr);
            ranges.put("kecepatan serangan", sr);
            ranges.put("kecepatan serang", sr);
        } else if (upper.contains("CRITICAL_STRIKE_CHANCE") || upper.contains("CRIT_CHANCE")) {
            ranges.put("crit chance", sr);
            ranges.put("crit strike chance", sr);
            ranges.put("critical chance", sr);
            ranges.put("critical strike chance", sr);
            ranges.put("peluang kritis", sr);
            ranges.put("peluang kritikal", sr);
            ranges.put("peluang critical", sr);
            ranges.put("kritis", sr);
        } else if (upper.contains("CRITICAL_STRIKE_POWER") || upper.contains("CRIT_DAMAGE") || upper.contains("CRIT_POWER") || upper.contains("CRITICAL_POWER") || upper.contains("CRITICAL_DAMAGE")) {
            ranges.put("crit power", sr);
            ranges.put("crit strike power", sr);
            ranges.put("critical strike power", sr);
            ranges.put("critical strike damage", sr);
            ranges.put("crit damage", sr);
            ranges.put("critical damage", sr);
            ranges.put("critical power", sr);
            ranges.put("kerusakan kritis", sr);
            ranges.put("kekuatan kritis", sr);
            ranges.put("damage kritis", sr);
            ranges.put("power kritis", sr);
        } else if (upper.contains("BLOCK_RATING") || upper.contains("BLOCK_POWER") || upper.contains("BLOCK")) {
            ranges.put("block rating", sr);
            ranges.put("block power", sr);
            ranges.put("block", sr);
            ranges.put("tangkisan", sr);
            ranges.put("blokir", sr);
        } else if (upper.contains("PARRY_RATING") || upper.contains("PARRY")) {
            ranges.put("parry rating", sr);
            ranges.put("parry", sr);
            ranges.put("tangkis", sr);
            ranges.put("parir", sr);
        } else if (upper.contains("MAX_HEALTH") || upper.equals("HEALTH")) {
            ranges.put("health", sr);
            ranges.put("max health", sr);
            ranges.put("max-health", sr);
            ranges.put("darah", sr);
            ranges.put("darah maksimal", sr);
            ranges.put("nyawa", sr);
            ranges.put("hp", sr);
            ranges.put("max hp", sr);
        } else if (upper.contains("DEFENSE") || upper.contains("ARMOR")) {
            ranges.put("defense", sr);
            ranges.put("defence", sr);
            ranges.put("armor", sr);
            ranges.put("pertahanan", sr);
            ranges.put("ketahanan", sr);
            ranges.put("pelindung", sr);
        } else if (upper.contains("DAMAGE_REDUCTION")) {
            ranges.put("damage reduction", sr);
            ranges.put("pengurangan kerusakan", sr);
            ranges.put("reduksi kerusakan", sr);
        } else if (upper.contains("MOVEMENT_SPEED") || upper.equals("SPEED")) {
            ranges.put("movement speed", sr);
            ranges.put("movement-speed", sr);
            ranges.put("speed", sr);
            ranges.put("kecepatan", sr);
            ranges.put("kecepatan gerak", sr);
            ranges.put("kecepatan jalan", sr);
        } else if (upper.contains("MAX_MANA") || upper.equals("MANA")) {
            ranges.put("mana", sr);
            ranges.put("max mana", sr);
            ranges.put("mana maksimal", sr);
        } else if (upper.contains("MANA_REGENERATION")) {
            ranges.put("mana regen", sr);
            ranges.put("mana regeneration", sr);
            ranges.put("regenerasi mana", sr);
        } else if (upper.contains("HEALTH_REGENERATION")) {
            ranges.put("health regen", sr);
            ranges.put("health regeneration", sr);
            ranges.put("regenerasi darah", sr);
            ranges.put("regenerasi hp", sr);
        } else if (upper.contains("MAGIC_DAMAGE")) {
            ranges.put("magic damage", sr);
            ranges.put("serangan sihir", sr);
            ranges.put("kerusakan sihir", sr);
        } else if (upper.contains("PVP_DAMAGE")) {
            ranges.put("pvp damage", sr);
            ranges.put("serangan pvp", sr);
        } else if (upper.contains("PVE_DAMAGE")) {
            ranges.put("pve damage", sr);
            ranges.put("serangan pve", sr);
        } else if (upper.contains("LUCK")) {
            ranges.put("luck", sr);
            ranges.put("keberuntungan", sr);
        }
    }

    private Double getFieldValue(Object obj, String fieldName, String methodName) {
        if (obj == null) return null;
        // 1. Coba getter method
        try {
            java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
            Object res = m.invoke(obj);
            if (res instanceof Number n) return n.doubleValue();
        } catch (Exception ignored) {}

        // 2. Coba direct field reflection
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                Object res = f.get(obj);
                if (res instanceof Number n) return n.doubleValue();
            } catch (Exception ignored) {}
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private String formatStatValue(double val) {
        if (val == (long) val) {
            return String.format(java.util.Locale.US, "%d", (long) val);
        } else {
            return String.format(java.util.Locale.US, "%.1f", val);
        }
    }

    private void setSafeStatData(MMOItem mmoItem, String statId, StatData data) {
        if (mmoItem == null || statId == null || data == null) return;
        try {
            net.Indyuce.mmoitems.stat.type.ItemStat stat = MMOItems.plugin.getStats().get(statId);
            if (stat != null) {
                mmoItem.setData(stat, data);
            }
        } catch (Throwable ignored) {}
    }

    private void setSafeStatData(MMOItem mmoItem, net.Indyuce.mmoitems.stat.type.ItemStat stat, StatData data) {
        if (mmoItem == null || stat == null || data == null) return;
        try {
            mmoItem.setData(stat, data);
        } catch (Throwable ignored) {}
    }
}
