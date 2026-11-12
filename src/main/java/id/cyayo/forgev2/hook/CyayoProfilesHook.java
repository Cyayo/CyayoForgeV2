package id.cyayo.forgev2.hook;

import id.cyayo.forgev2.CyayoForgeV2;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integrasi softdepend dengan CyayoProfiles untuk CyayoForgeV2:
 * 1. Mencegah player memakai blueprint yang Required Level-nya di atas level profil player.
 * 2. Mewariskan Required Level blueprint ke item hasil forge (dan menjaga saat reroll).
 */
public class CyayoProfilesHook {

    private static final Pattern LORE_LEVEL_PATTERN = Pattern.compile(
            "(?i)(?:required[\\s_]level|level[\\s_]requirement|syarat[\\s_]level|requires?\\s+lv(?:l|el)?\\.?|min\\.?\\s*lv(?:l|el)?\\.?|level)\\s*:?\\s*(\\d+)"
    );

    private final CyayoForgeV2 plugin;
    private volatile ReflectionApi cachedApi;

    public CyayoProfilesHook(CyayoForgeV2 plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        Plugin p = Bukkit.getPluginManager().getPlugin("CyayoProfiles");
        return p != null && p.isEnabled();
    }

    public boolean shouldInheritBlueprintLevel() {
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        return cfg.getBoolean("cyayoprofiles.enabled", true)
                && cfg.getBoolean("cyayoprofiles.inherit_blueprint_required_level", true);
    }

    private boolean isDebug() {
        return plugin.getConfigManager().getConfig().getBoolean("cyayoprofiles.debug", false);
    }

    /**
     * Mengambil Required Level dari item (VolatileMMOItem stat, NBT MMOITEMS_REQUIRED_LEVEL / REQUIRED_LEVEL
     * dalam format double/int/string, serta fallback pembacaan lore).
     */
    public int getItemRequiredLevel(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        int reqLevel = 0;

        try {
            NBTItem nbt = NBTItem.get(item);
            if (nbt != null) {
                // 1. Baca lewat VolatileMMOItem (paling akurat untuk tipe stat MMOItems)
                if (nbt.hasType()) {
                    try {
                        ItemStat reqStat = ItemStats.REQUIRED_LEVEL != null
                                ? ItemStats.REQUIRED_LEVEL
                                : MMOItems.plugin.getStats().get("REQUIRED_LEVEL");
                        if (reqStat != null) {
                            VolatileMMOItem vItem = new VolatileMMOItem(nbt);
                            if (vItem.hasData(reqStat)) {
                                StatData data = vItem.getData(reqStat);
                                if (data instanceof DoubleData dd && dd.getValue() > 0) {
                                    reqLevel = Math.max(reqLevel, (int) Math.round(dd.getValue()));
                                } else if (data != null) {
                                    double parsed = Double.parseDouble(data.toString().trim());
                                    if (parsed > 0) {
                                        reqLevel = Math.max(reqLevel, (int) Math.round(parsed));
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // 2. Baca langsung tag NBT dalam berbagai variasi nama & tipe (masing-masing terisolasi)
                if (reqLevel <= 0) {
                    String[] keys = new String[]{
                            "MMOITEMS_REQUIRED_LEVEL",
                            "REQUIRED_LEVEL",
                            "required_level",
                            "MMOITEMS_required_level"
                    };
                    for (String k : keys) {
                        if (!nbt.hasTag(k)) continue;
                        try {
                            double d = nbt.getDouble(k);
                            if (d > 0) {
                                reqLevel = Math.max(reqLevel, (int) Math.round(d));
                            }
                        } catch (Throwable ignored) {
                        }
                        try {
                            int i = nbt.getInteger(k);
                            if (i > 0) {
                                reqLevel = Math.max(reqLevel, i);
                            }
                        } catch (Throwable ignored) {
                        }
                        try {
                            String s = nbt.getString(k);
                            if (s != null && !s.trim().isEmpty()) {
                                double parsed = Double.parseDouble(s.trim());
                                if (parsed > 0) {
                                    reqLevel = Math.max(reqLevel, (int) Math.round(parsed));
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                        if (reqLevel > 0) break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 3. Fallback Lore (jika tag NBT tidak terbaca atau item custom lore)
        if (reqLevel <= 0 && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line == null) continue;
                    String stripped = ChatColor.stripColor(line).trim();
                    Matcher m = LORE_LEVEL_PATTERN.matcher(stripped);
                    if (m.find()) {
                        try {
                            reqLevel = Math.max(reqLevel, Integer.parseInt(m.group(1)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        if (isDebug()) {
            plugin.getLogger().info("[CyayoProfilesDebug] getItemRequiredLevel(" + item.getType() + ") = " + reqLevel);
        }
        return reqLevel;
    }

    /**
     * Menyuntikkan ItemStats.REQUIRED_LEVEL ke MMOItem sebelum di-build menjadi ItemStack.
     */
    public void applyRequiredLevelStat(MMOItem mmoItem, int level, String context) {
        if (mmoItem == null || level <= 0) {
            return;
        }
        try {
            ItemStat reqStat = ItemStats.REQUIRED_LEVEL != null
                    ? ItemStats.REQUIRED_LEVEL
                    : MMOItems.plugin.getStats().get("REQUIRED_LEVEL");
            if (reqStat == null) {
                plugin.getLogger().warning("[CyayoForgeV2] Stat REQUIRED_LEVEL MMOItems tidak ditemukan (" + context + ").");
                return;
            }
            mmoItem.setData(reqStat, new DoubleData(level));
            if (isDebug()) {
                plugin.getLogger().info("[CyayoProfilesDebug] Set REQUIRED_LEVEL=" + level + " pada " + context);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[CyayoForgeV2] Gagal menyuntikkan REQUIRED_LEVEL (" + context + "): " + t.getMessage());
        }
    }

    /**
     * Mengecek apakah player boleh menggunakan blueprint berdasarkan level profil CyayoProfiles.
     */
    public BlueprintLevelCheck checkBlueprintLevel(Player player, ItemStack blueprint) {
        if (player == null || blueprint == null || blueprint.getType().isAir()) {
            return BlueprintLevelCheck.ALLOW;
        }
        FileConfiguration cfg = plugin.getConfigManager().getConfig();
        if (!cfg.getBoolean("cyayoprofiles.enabled", true)
                || !cfg.getBoolean("cyayoprofiles.block_higher_level_blueprint", true)
                || !isAvailable()) {
            return BlueprintLevelCheck.ALLOW;
        }

        String bypassPerm = cfg.getString("cyayoprofiles.bypass_permission", "cyayoforgev2.bypass.level");
        if ((bypassPerm != null && !bypassPerm.isEmpty() && player.hasPermission(bypassPerm))
                || player.hasPermission("mmoitems.bypass.level")) {
            return BlueprintLevelCheck.ALLOW;
        }

        int reqLevel = getItemRequiredLevel(blueprint);
        if (reqLevel <= 0) {
            return BlueprintLevelCheck.ALLOW;
        }

        Plugin cyayoPlugin = Bukkit.getPluginManager().getPlugin("CyayoProfiles");
        if (cyayoPlugin == null || !cyayoPlugin.isEnabled()) {
            return BlueprintLevelCheck.ALLOW;
        }
        ReflectionApi api = getOrInitApi(cyayoPlugin);
        if (api == null) {
            return BlueprintLevelCheck.ALLOW;
        }

        try {
            boolean requireActive = cfg.getBoolean("cyayoprofiles.require_active_profile", true);
            boolean hasProfile = (Boolean) api.hasActiveProfileMethod.invoke(null, player.getUniqueId());
            if (requireActive && !hasProfile) {
                return new BlueprintLevelCheck(false, true, reqLevel, 0);
            }
            Object lvlObj = api.getProfileLevelMethod.invoke(null, player);
            int playerLevel = (lvlObj instanceof Number n) ? n.intValue() : 1;
            if (isDebug()) {
                plugin.getLogger().info("[CyayoProfilesDebug] Check blueprint: player=" + player.getName()
                        + " playerLevel=" + playerLevel + " bpReqLevel=" + reqLevel);
            }
            if (playerLevel < reqLevel) {
                return new BlueprintLevelCheck(false, false, reqLevel, playerLevel);
            }
        } catch (Throwable ignored) {
        }
        return BlueprintLevelCheck.ALLOW;
    }

    private ReflectionApi getOrInitApi(Plugin cyayoPlugin) {
        ReflectionApi current = cachedApi;
        if (current != null && current.plugin == cyayoPlugin) {
            return current;
        }
        synchronized (this) {
            current = cachedApi;
            if (current != null && current.plugin == cyayoPlugin) {
                return current;
            }
            ReflectionApi created = ReflectionApi.create(cyayoPlugin);
            cachedApi = created;
            return created;
        }
    }

    public static final class BlueprintLevelCheck {
        public static final BlueprintLevelCheck ALLOW = new BlueprintLevelCheck(true, false, 0, 0);

        public final boolean allowed;
        public final boolean noProfile;
        public final int requiredLevel;
        public final int playerLevel;

        public BlueprintLevelCheck(boolean allowed, boolean noProfile, int requiredLevel, int playerLevel) {
            this.allowed = allowed;
            this.noProfile = noProfile;
            this.requiredLevel = requiredLevel;
            this.playerLevel = playerLevel;
        }
    }

    private static final class ReflectionApi {
        private final Plugin plugin;
        private final Method hasActiveProfileMethod;
        private final Method getProfileLevelMethod;

        private ReflectionApi(Plugin plugin, Method hasActiveProfileMethod, Method getProfileLevelMethod) {
            this.plugin = plugin;
            this.hasActiveProfileMethod = hasActiveProfileMethod;
            this.getProfileLevelMethod = getProfileLevelMethod;
        }

        private static ReflectionApi create(Plugin cyayoPlugin) {
            try {
                ClassLoader cl = cyayoPlugin.getClass().getClassLoader();
                Class<?> apiClass = Class.forName("id.cyayo.profiles.api.CyayoProfilesAPI", true, cl);
                Method hasActive = apiClass.getMethod("hasActiveProfile", UUID.class);
                Method getLevel = apiClass.getMethod("getProfileLevel", Player.class);
                return new ReflectionApi(cyayoPlugin, hasActive, getLevel);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
