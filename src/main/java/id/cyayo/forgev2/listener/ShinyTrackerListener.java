package id.cyayo.forgev2.listener;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.manager.ShinyTrackerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class ShinyTrackerListener implements Listener {

    private final CyayoForgeV2 plugin;
    private final ShinyTrackerManager manager;

    public ShinyTrackerListener(CyayoForgeV2 plugin, ShinyTrackerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        if (entity instanceof Player) {
            manager.addTrackerToPlayer(killer, "PLAYERS_KILLED", 1, "mainhand");
        } else if (isBoss(entity)) {
            manager.addTrackerToPlayer(killer, "BOSSES_KILLED", 1, "mainhand");
            manager.addTrackerToPlayer(killer, "MOBS_KILLED", 1, "mainhand");
        } else {
            manager.addTrackerToPlayer(killer, "MOBS_KILLED", 1, "mainhand");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        manager.addTrackerToPlayer(victim, "DEATHS", 1, "all");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        double damage = event.getFinalDamage();
        if (damage <= 0) return;
        long roundedDmg = Math.max(1, Math.round(damage));

        // 1. Damage Dealt (Serangan keluar dari player)
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker != null) {
            manager.addTrackerToPlayer(attacker, "DAMAGE_DEALT", roundedDmg, "mainhand");
        }

        // 2. Damage Taken (Damage masuk ke player)
        if (event.getEntity() instanceof Player victim) {
            manager.addTrackerToPlayer(victim, "DAMAGE_TAKEN", roundedDmg, "armor");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        manager.addTrackerToPlayer(player, "BLOCKS_MINED", 1, "mainhand");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            manager.addTrackerToPlayer(player, "FISH_CAUGHT", 1, "mainhand");
        }
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    private boolean isBoss(Entity entity) {
        if (entity == null) return false;
        String typeName = entity.getType().name();
        if (typeName.equals("WITHER") || typeName.equals("ENDER_DRAGON")
                || typeName.equals("ELDER_GUARDIAN") || typeName.equals("WARDEN")) {
            return true;
        }

        // Cek tag scoreboard atau metadata
        if (entity.getScoreboardTags().contains("boss") || entity.hasMetadata("boss")) {
            return true;
        }

        // Cek MythicMobs via reflection jika plugin aktif (tanpa butuh compile-time dependency)
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            try {
                Class<?> mbClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                Object mbPlugin = mbClass.getMethod("getPlugin").invoke(null);
                Object mobManager = mbClass.getMethod("getMobManager").invoke(mbPlugin);
                java.lang.reflect.Method isMythicMethod = mobManager.getClass().getMethod("isMythicMob", Entity.class);
                boolean isMythic = (boolean) isMythicMethod.invoke(mobManager, entity);
                if (isMythic) {
                    java.lang.reflect.Method getMobMethod = mobManager.getClass().getMethod("getMythicMobInstance", Entity.class);
                    Object activeMob = getMobMethod.invoke(mobManager, entity);
                    if (activeMob != null) {
                        try {
                            boolean hasBossBar = (boolean) activeMob.getClass().getMethod("hasBossBar").invoke(activeMob);
                            if (hasBossBar) return true;
                        } catch (Throwable ignored) {}
                        try {
                            Object mobType = activeMob.getClass().getMethod("getType").invoke(activeMob);
                            Object config = mobType.getClass().getMethod("getConfig").invoke(mobType);
                            boolean isBossCfg = (boolean) config.getClass().getMethod("getBoolean", String.class, boolean.class).invoke(config, "Boss", false);
                            if (isBossCfg) return true;
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }
}
