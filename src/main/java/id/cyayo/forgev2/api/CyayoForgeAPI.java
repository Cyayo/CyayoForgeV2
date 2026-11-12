package id.cyayo.forgev2.api;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.model.BlueprintRequirement;
import id.cyayo.forgev2.model.MaterialStatRange;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Developer API untuk CyayoForgeV2.
 * Kelas ini bisa diakses secara statis oleh plugin eksternal manapun.
 */
public class CyayoForgeAPI {

    /**
     * Mendapatkan instance utama CyayoForgeV2 plugin.
     */
    public static CyayoForgeV2 getPlugin() {
        return CyayoForgeV2.getInstance();
    }

    /**
     * Membuka GUI Forge untuk pemain.
     */
    public static void openForgeGUI(Player player) {
        if (player != null) {
            new id.cyayo.forgev2.gui.ForgeGUI(player).open();
        }
    }

    /**
     * Membuka GUI Crusher untuk pemain.
     */
    public static void openCrusherGUI(Player player) {
        if (player != null) {
            new id.cyayo.forgev2.gui.CrusherGUI(player).open();
        }
    }

    /**
     * Membuka GUI Reroll untuk pemain.
     */
    public static void openRerollGUI(Player player) {
        if (player != null) {
            new id.cyayo.forgev2.gui.RerollGUI(player).open();
        }
    }

    /**
     * Membuka GUI Winspect (Inspect Weapon) untuk pemain.
     */
    public static void openInspectGUI(Player viewer, ItemStack forgedItem) {
        if (viewer != null && forgedItem != null) {
            new id.cyayo.forgev2.gui.WinspectGUI(viewer, forgedItem).open();
        }
    }

    /**
     * Mengecek apakah item merupakan Blueprint yang sah.
     */
    public static boolean isBlueprint(ItemStack item) {
        return getPlugin().getForgeManager().isBlueprint(item);
    }

    /**
     * Mengecek apakah item merupakan hasil tempaan CyayoForge.
     */
    public static boolean isForgedItem(ItemStack item) {
        if (item == null) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;
        return nbt.hasTag("CYAYO_FORGE_CRAFTER") || nbt.hasTag("MMOITEMS_FORGE_CRAFTER") || nbt.hasTag("FORGE_CRAFTER");
    }

    /**
     * Mengambil nama pemain yang menempa item ini.
     */
    public static String getCrafter(ItemStack item) {
        if (item == null) return "";
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return "";
        String crafter = nbt.getString("CYAYO_FORGE_CRAFTER");
        if (crafter.isEmpty()) crafter = nbt.getString("MMOITEMS_FORGE_CRAFTER");
        if (crafter.isEmpty()) crafter = nbt.getString("FORGE_CRAFTER");
        return crafter;
    }

    /**
     * Mengambil persentase kualitas (Quality 1-100%) dari item hasil tempaan.
     */
    public static double getQuality(ItemStack item) {
        if (item == null) return 0.0;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return 0.0;
        if (nbt.hasTag("CYAYO_FORGE_QUALITY")) return nbt.getDouble("CYAYO_FORGE_QUALITY");
        if (nbt.hasTag("MMOITEMS_FORGE_QUALITY")) return nbt.getDouble("MMOITEMS_FORGE_QUALITY");
        if (nbt.hasTag("FORGE_QUALITY")) return nbt.getDouble("FORGE_QUALITY");
        return 0.0;
    }

    /**
     * Mengambil multiplier akhir yang disuntikkan ke item.
     */
    public static double getFinalMultiplier(ItemStack item) {
        if (item == null) return 1.0;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return 1.0;
        if (nbt.hasTag("CYAYO_FORGE_FINAL_MULTIPLIER")) return nbt.getDouble("CYAYO_FORGE_FINAL_MULTIPLIER");
        if (nbt.hasTag("MMOITEMS_FORGE_FINAL_MULTIPLIER")) return nbt.getDouble("MMOITEMS_FORGE_FINAL_MULTIPLIER");
        if (nbt.hasTag("FORGE_FINAL_MULTIPLIER")) return nbt.getDouble("FORGE_FINAL_MULTIPLIER");
        return 1.0;
    }

    /**
     * Mengambil material yang paling dominan saat penempaan.
     */
    public static String getDominantMaterial(ItemStack item) {
        if (item == null) return "";
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return "";
        String dom = nbt.getString("CYAYO_FORGE_DOMINANT");
        if (dom.isEmpty()) dom = nbt.getString("MMOITEMS_FORGE_DOMINANT_MATERIAL");
        if (dom.isEmpty()) dom = nbt.getString("FORGE_DOMINANT_MATERIAL");
        return dom;
    }

    /**
     * Mengambil jumlah reroll yang telah dilakukan pada item.
     */
    public static int getRerollCount(ItemStack item) {
        if (item == null) return 0;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return 0;
        if (nbt.hasTag("CYAYO_FORGE_REROLL_COUNT")) return nbt.getInteger("CYAYO_FORGE_REROLL_COUNT");
        if (nbt.hasTag("MMOITEMS_FORGE_REROLL_COUNT")) return nbt.getInteger("MMOITEMS_FORGE_REROLL_COUNT");
        return 0;
    }

    /**
     * Mengambil daftar syarat material dari sebuah Blueprint.
     */
    public static List<BlueprintRequirement> getBlueprintRequirements(ItemStack blueprint) {
        if (blueprint == null) return java.util.Collections.emptyList();
        NBTItem nbt = NBTItem.get(blueprint);
        return getPlugin().getForgeManager().getRequirements(nbt);
    }

    /**
     * Membaca bonus stats yang diberikan oleh suatu material.
     */
    public static Map<String, MaterialStatRange> getMaterialBonusStats(ItemStack material, String targetType) {
        return getPlugin().getForgeManager().getMaterialBonusStats(material, targetType);
    }

    /**
     * Memulai proses penempaan secara paksa melalui kode.
     * Mengembalikan item hasil tempa.
     *
     * @param crafter   Pemain yang menempa
     * @param blueprint Blueprint item
     * @param mat1List  List material slot 1
     * @param mat2List  List material slot 2
     * @param mat3List  List material slot 3
     * @return ItemStack hasil tempaan
     */
    public static ItemStack craftItem(Player crafter, ItemStack blueprint, List<ItemStack> mat1List, List<ItemStack> mat2List, List<ItemStack> mat3List) {
        return getPlugin().getForgeManager().craftItem(crafter, blueprint, mat1List, mat2List, mat3List);
    }

    /**
     * Memulai proses reroll item secara programatik.
     */
    public static ItemStack rerollItem(ItemStack targetItem) {
        return getPlugin().getForgeManager().rerollItem(targetItem);
    }

    /**
     * Mendapatkan daftar Top 10 Blacksmith berdasarkan skor (senjata 100% perfection).
     * 
     * @return Map berisi Nama Pemain dan Skor mereka (diurutkan dari tertinggi).
     */
    public static Map<String, Integer> getLeaderboard() {
        return getPlugin().getLeaderboardManager().getTopPlayers(10);
    }
    
    /**
     * Mendapatkan skor spesifik milik satu pemain.
     */
    public static int getPlayerScore(String playerName) {
        return getPlugin().getLeaderboardManager().getPlayerScore(playerName);
    }

    /**
     * Mengecek apakah item merupakan item Shiny.
     */
    public static boolean isShiny(ItemStack item) {
        if (item == null) return false;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return false;
        if (nbt.hasTag("CYAYO_FORGE_IS_SHINY") && nbt.getBoolean("CYAYO_FORGE_IS_SHINY")) return true;
        if (nbt.hasTag("MMOITEMS_FORGE_IS_SHINY") && "TRUE".equalsIgnoreCase(nbt.getString("MMOITEMS_FORGE_IS_SHINY"))) return true;
        return false;
    }

    /**
     * Mengambil ID tracker yang terpasang pada item shiny (contoh: mobs_killed, dungeons_won).
     */
    public static String getTrackerId(ItemStack item) {
        if (item == null) return "";
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return "";
        return nbt.getString("CYAYO_SHINY_TRACKER_ID");
    }

    /**
     * Mengambil Display Name dari tracker pada item shiny.
     */
    public static String getTrackerName(ItemStack item) {
        if (item == null) return "";
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return "";
        String name = nbt.getString("CYAYO_SHINY_TRACKER_NAME");
        if (name.isEmpty()) name = nbt.getString("MMOITEMS_FORGE_TRACKER_NAME");
        return name;
    }

    /**
     * Mengambil nilai capaian tracker pada item shiny.
     */
    public static long getTrackerValue(ItemStack item) {
        if (item == null) return 0;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasType()) return 0;
        if (nbt.hasTag("CYAYO_SHINY_TRACKER_VALUE")) {
            try {
                return nbt.getInteger("CYAYO_SHINY_TRACKER_VALUE");
            } catch (Exception e) {
                try {
                    return (long) Math.round(nbt.getDouble("CYAYO_SHINY_TRACKER_VALUE"));
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    /**
     * Menambahkan nilai statistik tracker pada gear pemain yang sedang online (slot mainhand & armor).
     *
     * @param player           Pemain yang memakai gear
     * @param trackerTriggerOrId ID tracker (contoh: "dungeons_won") atau tipe trigger ("CUSTOM", "MOBS_KILLED")
     * @param amount           Jumlah penambahan nilai
     * @return Jumlah item yang berhasil diupdate
     */
    public static int addTrackerValue(Player player, String trackerTriggerOrId, long amount) {
        return addTrackerValue(player, trackerTriggerOrId, amount, "all");
    }

    /**
     * Menambahkan nilai statistik tracker pada gear pemain dengan filter slot.
     *
     * @param player           Pemain yang memakai gear
     * @param trackerTriggerOrId ID tracker atau tipe trigger
     * @param amount           Jumlah penambahan nilai
     * @param slotFilter       "mainhand", "armor", atau "all"
     * @return Jumlah item yang berhasil diupdate
     */
    public static int addTrackerValue(Player player, String trackerTriggerOrId, long amount, String slotFilter) {
        if (getPlugin().getShinyTrackerManager() == null) return 0;
        return getPlugin().getShinyTrackerManager().addTrackerToPlayer(player, trackerTriggerOrId, amount, slotFilter);
    }

    /**
     * Memperbarui nilai tracker langsung pada sebuah ItemStack jika item adalah Shiny.
     *
     * @param item             Item yang akan diupdate
     * @param trackerTriggerOrId ID tracker atau tipe trigger
     * @param amountToAdd      Jumlah penambahan nilai
     * @return ItemStack baru yang telah diperbarui, atau null jika item tidak cocok
     */
    public static ItemStack updateItemTracker(ItemStack item, String trackerTriggerOrId, long amountToAdd) {
        if (getPlugin().getShinyTrackerManager() == null) return null;
        return getPlugin().getShinyTrackerManager().updateItemTracker(item, trackerTriggerOrId, amountToAdd);
    }
}
