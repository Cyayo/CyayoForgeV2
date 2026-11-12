package id.cyayo.forgev2;

import id.cyayo.forgev2.manager.ConfigManager;
import id.cyayo.forgev2.manager.ForgeManager;
import id.cyayo.forgev2.manager.LeaderboardManager;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class CyayoForgeV2 extends JavaPlugin {

    private static CyayoForgeV2 instance;

    @Override
    public void onLoad() {
        registerCustomStats();
    }

    public void registerCustomStats() {
        try {
            registerSingleStat(new StringStat("FORGE_TARGET", Material.ANVIL, "Forge Target Item",
                    new String[] { "Item hasil forge (Format: TYPE:ID, contoh: SWORD:EXCALIBUR_COMMON)." },
                    new String[] { "all" }));
            registerSingleStat(new net.Indyuce.mmoitems.stat.type.StringListStat("FORGE_REQUIREMENTS", Material.ANVIL,
                    "Forge Requirements", new String[] { "Daftar syarat bahan penempaan (Format: KATEGORI:JUMLAH)." },
                    new String[] { "all" }));
            registerSingleStat(new net.Indyuce.mmoitems.stat.type.StringListStat("FORGE_BONUS_STATS", Material.ANVIL,
                    "Forge Bonus Stats",
                    new String[] { "Bonus stat yang diberikan material ini saat ditempa (Format: STAT_ID:NILAI)." },
                    new String[] { "all" }));
            registerSingleStat(new DoubleStat("FORGE_MATERIAL_MULTIPLIER", Material.ANVIL, "Forge Material Multiplier",
                    new String[] { "Nilai pengali (persentase) yang diberikan material ini ke hasil forge." },
                    new String[] { "all" }));
            registerSingleStat(new DoubleStat("FORGE_FINAL_MULTIPLIER", Material.ANVIL, "Forge Final Multiplier",
                    new String[] { "Nilai akhir pengali stat saat item ini ditempa (Internal use)." },
                    new String[] { "all" }));
            registerSingleStat(new StringStat("FORGE_CRAFTER", Material.ANVIL, "Forge Crafter",
                    new String[] { "Nama pemain yang menempa item ini." }, new String[] { "all" }));
            registerSingleStat(new DoubleStat("FORGE_QUALITY", Material.ANVIL, "Forge Quality",
                    new String[] { "Persentase kesempurnaan (quality) item ini saat ditempa." },
                    new String[] { "all" }));
            registerSingleStat(new StringStat("FORGE_DOMINANT_MATERIAL", Material.ANVIL, "Forge Dominant Material",
                    new String[] { "Material yang paling dominan saat penempaan." }, new String[] { "all" }));
            registerSingleStat(new DoubleStat("FORGE_REROLL_COUNT", Material.ANVIL, "Forge Reroll Count",
                    new String[] { "Jumlah berapa kali item ini telah di-reroll." }, new String[] { "all" }));

            // Shiny & Stat Tracker Custom Stats
            registerSingleStat(new StringStat("FORGE_IS_SHINY", Material.NETHER_STAR, "Forge Is Shiny",
                    new String[] { "Menandakan apakah item ini merupakan item Shiny." }, new String[] { "all" }));
            registerSingleStat(new StringStat("FORGE_TRACKER", Material.COMPASS, "Forge Tracker",
                    new String[] { "Stat tracker aktif dan jumlah hitungan pencapaian (StatTrak)." }, new String[] { "all" }));
            registerSingleStat(new StringStat("FORGE_TRACKER_NAME", Material.NAME_TAG, "Forge Tracker Name",
                    new String[] { "Nama jenis tracker yang sedang aktif pada item shiny ini." }, new String[] { "all" }));
            registerSingleStat(new DoubleStat("FORGE_TRACKER_VALUE", Material.EXPERIENCE_BOTTLE, "Forge Tracker Value",
                    new String[] { "Angka capaian statistik pada item shiny ini." }, new String[] { "all" }));

            getLogger().info("Successfully registered clean custom stats to MMOItems!");
        } catch (Throwable e) {
            getLogger().warning("Failed to register MMOItems custom stats: " + e.getMessage());
        }
    }

    private void registerSingleStat(net.Indyuce.mmoitems.stat.type.ItemStat stat) {
        try {
            if (MMOItems.plugin.getStats().get(stat.getId()) == null) {
                MMOItems.plugin.getStats().register(stat);
            }
        } catch (Throwable ignored) {
        }
    }

    private ConfigManager configManager;
    private ForgeManager forgeManager;
    private id.cyayo.forgev2.manager.LeaderboardManager leaderboardManager;
    private id.cyayo.forgev2.manager.ShinyTrackerManager shinyTrackerManager;
    private id.cyayo.forgev2.hook.CyayoProfilesHook cyayoProfilesHook;
    private net.milkbowl.vault.economy.Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;
        registerCustomStats();

        // Inisialisasi Manager
        configManager = new ConfigManager(this);
        forgeManager = new ForgeManager(this);
        leaderboardManager = new id.cyayo.forgev2.manager.LeaderboardManager(this);
        shinyTrackerManager = new id.cyayo.forgev2.manager.ShinyTrackerManager(this);
        cyayoProfilesHook = new id.cyayo.forgev2.hook.CyayoProfilesHook(this);
        if (cyayoProfilesHook.isAvailable()) {
            getLogger().info("Terhubung dengan CyayoProfiles untuk batas level blueprint & inherit level item!");
        }

        // Daftarkan Perintah & TabCompleter
        id.cyayo.forgev2.command.CyayoForgeCommand cmd = new id.cyayo.forgev2.command.CyayoForgeCommand();
        registerCommand("cyayoforge", cmd);
        registerCommand("forge", cmd);
        registerCommand("crusher", cmd);
        registerCommand("reroll", cmd);
        registerCommand("winspect", cmd);

        // Daftarkan Listener
        getServer().getPluginManager().registerEvents(new id.cyayo.forgev2.listener.ForgeListener(this), this);
        getServer().getPluginManager().registerEvents(new id.cyayo.forgev2.listener.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new id.cyayo.forgev2.listener.ShinyTrackerListener(this, shinyTrackerManager), this);

        // Integrasi Vault
        if (!setupEconomy()) {
            getLogger().severe(
                    String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
        }

        // Integrasi PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.cyayo.forgev2.hook.ForgeExpansion(this).register();
            getLogger().info("Terhubung dengan PlaceholderAPI!");
        } else {
            getLogger().warning("PlaceholderAPI tidak ditemukan! Leaderboard placeholder dinonaktifkan.");
        }

        getLogger().info("CyayoForgeV2 enabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null)
            return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void registerCommand(String name, id.cyayo.forgev2.command.CyayoForgeCommand cmd) {
        org.bukkit.command.PluginCommand pc = getCommand(name);
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
    }

    public void reloadConfigs() {
        if (configManager != null) {
            configManager.loadConfigs();
        }
        if (shinyTrackerManager != null) {
            shinyTrackerManager.loadConfig();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ForgeManager getForgeManager() {
        return forgeManager;
    }

    public id.cyayo.forgev2.manager.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public id.cyayo.forgev2.manager.ShinyTrackerManager getShinyTrackerManager() {
        return shinyTrackerManager;
    }

    public id.cyayo.forgev2.hook.CyayoProfilesHook getCyayoProfilesHook() {
        return cyayoProfilesHook;
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        return econ;
    }

    @Override
    public void onDisable() {
        getLogger().info("CyayoForgeV2 disabled!");
    }

    public static CyayoForgeV2 getInstance() {
        return instance;
    }
}
