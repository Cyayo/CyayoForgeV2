package id.cyayo.forgev2.listener;

import id.cyayo.forgev2.CyayoForgeV2;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final CyayoForgeV2 plugin;
    private static final Map<String, ShowcaseEntry> showcaseCache = new ConcurrentHashMap<>();

    public ChatListener(CyayoForgeV2 plugin) {
        this.plugin = plugin;
    }

    public static class ShowcaseEntry {
        private final ItemStack item;
        private final long createdAt;

        public ShowcaseEntry(ItemStack item) {
            this.item = item.clone();
            this.createdAt = System.currentTimeMillis();
        }

        public ItemStack getItem() {
            return item;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    public static ItemStack getShowcaseItem(String id) {
        ShowcaseEntry entry = showcaseCache.get(id);
        if (entry == null) return null;

        long expiryMinutes = CyayoForgeV2.getInstance().getConfigManager().getConfig().getLong("chat-showcase.expiry-minutes", 10);
        if (System.currentTimeMillis() - entry.getCreatedAt() > expiryMinutes * 60 * 1000) {
            showcaseCache.remove(id);
            return null;
        }

        return entry.getItem();
    }

    public static Set<String> getActiveShowcaseIds() {
        long expiryMinutes = CyayoForgeV2.getInstance().getConfigManager().getConfig().getLong("chat-showcase.expiry-minutes", 10);
        long now = System.currentTimeMillis();
        showcaseCache.entrySet().removeIf(entry -> (now - entry.getValue().getCreatedAt() > expiryMinutes * 60 * 1000));
        return showcaseCache.keySet();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig();
        if (!config.getBoolean("chat-showcase.enabled", true)) return;

        List<String> placeholders = config.getStringList("chat-showcase.placeholders");
        if (placeholders == null || placeholders.isEmpty()) {
            placeholders = Arrays.asList("[winspect]");
        }

        String message = event.getMessage();
        String lowercaseMsg = message.toLowerCase();

        boolean hasPlaceholder = false;
        for (String ph : placeholders) {
            if (lowercaseMsg.contains(ph.toLowerCase())) {
                hasPlaceholder = true;
                break;
            }
        }

        if (!hasPlaceholder) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) return;

        io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
        if (!nbt.hasTag("CYAYO_FORGE_CRAFTER")) return;

        // Bersihkan cache lama
        long expiryMs = config.getLong("chat-showcase.expiry-minutes", 10) * 60 * 1000;
        long now = System.currentTimeMillis();
        showcaseCache.entrySet().removeIf(e -> now - e.getValue().getCreatedAt() > expiryMs);

        String showcaseId = "v2_" + UUID.randomUUID().toString().substring(0, 8);
        showcaseCache.put(showcaseId, new ShowcaseEntry(item));

        String formatPattern = config.getString("chat-showcase.format", "&e[{item_name}&e]");

        List<BaseComponent> messageComponents = new ArrayList<>();
        String currentText = message;

        while (true) {
            String foundPlaceholder = null;
            int foundIdx = -1;

            for (String ph : placeholders) {
                int idx = currentText.toLowerCase().indexOf(ph.toLowerCase());
                if (idx != -1 && (foundIdx == -1 || idx < foundIdx)) {
                    foundIdx = idx;
                    foundPlaceholder = ph;
                }
            }

            if (foundPlaceholder == null) {
                if (!currentText.isEmpty()) {
                    messageComponents.addAll(Arrays.asList(TextComponent.fromLegacyText(currentText)));
                }
                break;
            }

            if (foundIdx > 0) {
                String before = currentText.substring(0, foundIdx);
                messageComponents.addAll(Arrays.asList(TextComponent.fromLegacyText(before)));
            }

            messageComponents.add(createShowcaseComponent(item, showcaseId, formatPattern));
            currentText = currentText.substring(foundIdx + foundPlaceholder.length());
        }

        String customFormat = config.getString("chat-showcase.chat-format", "");
        String prefix, suffix;
        
        if (customFormat != null && !customFormat.isEmpty() && customFormat.contains("{message}")) {
            int index = customFormat.indexOf("{message}");
            String prefixPart = customFormat.substring(0, index);
            String suffixPart = customFormat.substring(index + "{message}".length());

            prefix = prefixPart.replace("%player_displayname%", player.getDisplayName())
                               .replace("%player%", player.getName());
            suffix = suffixPart.replace("%player_displayname%", player.getDisplayName())
                               .replace("%player%", player.getName());
        } else {
            String format = event.getFormat();
            String prefixFormat = format.contains("%2$s") ? format.substring(0, format.indexOf("%2$s")) : format;
            String suffixFormat = format.contains("%2$s") ? format.substring(format.indexOf("%2$s") + 4) : "";
            prefix = String.format(prefixFormat, player.getDisplayName());
            suffix = String.format(suffixFormat, player.getDisplayName());
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            prefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, prefix);
            suffix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, suffix);
        }

        BaseComponent[] prefixComp = TextComponent.fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix));
        BaseComponent[] suffixComp = TextComponent.fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', suffix));

        List<BaseComponent> finalComponents = new ArrayList<>();
        finalComponents.addAll(Arrays.asList(prefixComp));
        finalComponents.addAll(messageComponents);
        finalComponents.addAll(Arrays.asList(suffixComp));

        BaseComponent[] finalArray = finalComponents.toArray(new BaseComponent[0]);

        final String consoleLog = org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + message + suffix);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player recipient : event.getRecipients()) {
                recipient.spigot().sendMessage(finalArray);
            }
            Bukkit.getConsoleSender().sendMessage(consoleLog);
        });

        event.setCancelled(true);
    }

    private BaseComponent createShowcaseComponent(ItemStack item, String id, String formatPattern) {
        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name().replace("_", " ");

        String text = formatPattern.replace("{item_name}", displayName);
        text = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
        
        TextComponent comp = new TextComponent(TextComponent.fromLegacyText(text));

        List<String> hoverLines = new ArrayList<>();
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta.hasDisplayName()) hoverLines.add(meta.getDisplayName());
            else hoverLines.add("§f" + item.getType().name());
            
            if (meta.hasLore()) hoverLines.addAll(meta.getLore());
        } else {
            hoverLines.add("§f" + item.getType().name());
        }

        String hoverText = String.join("\n", hoverLines);
        String hoverConfigText = plugin.getConfigManager().getConfig().getString("chat-showcase.hover-text", "&eKlik untuk Winspect!");
        hoverText += "\n\n" + org.bukkit.ChatColor.translateAlternateColorCodes('&', hoverConfigText);
        
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText)));
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/winspect " + id));

        return comp;
    }
}
