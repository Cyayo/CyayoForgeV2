package id.cyayo.forgev2.util;

import id.cyayo.forgev2.CyayoForgeV2;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    private static final NamespacedKey DECORATION_KEY = new NamespacedKey(CyayoForgeV2.getInstance(), "gui_decoration");

    public static ItemStack markAsDecoration(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(DECORATION_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isDecoration(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte val = meta.getPersistentDataContainer().get(DECORATION_KEY, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public static ItemStack createDecorationItem(ConfigurationSection section) {
        if (section == null) return null;
        String matName = section.getString("material", "BLACK_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name");
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            if (section.contains("custom-model-data")) {
                meta.setCustomModelData(section.getInt("custom-model-data"));
            }
            meta.getPersistentDataContainer().set(DECORATION_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) colored.add(ChatColor.translateAlternateColorCodes('&', l));
                meta.setLore(colored);
            }
            meta.getPersistentDataContainer().set(DECORATION_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY_SERIALIZER = 
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                    .character(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.SECTION_CHAR)
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        // 1. Jika mengandung tag MiniMessage (seperti <gradient>, <#rrggbb>, <b>, <u>, dll)
        if (text.contains("<") && text.contains(">")) {
            try {
                String miniMessageInput = toMiniMessage(text);
                net.kyori.adventure.text.Component comp = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(miniMessageInput);
                return LEGACY_SERIALIZER.serialize(comp);
            } catch (Throwable e) {
                // Fallback jika ada tag yang salah ketik di config agar tidak crash
                return ChatColor.translateAlternateColorCodes('&', text);
            }
        }

        // 2. Parse Hex Color (&#RRGGBB) format Bukkit/Spigot jika ada
        text = translateHexColorCodes(text);

        // 3. Parse Legacy Color Codes (&a, &b, dll)
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Mengonversi string yang bercampur dengan kode warna Spigot / Legacy (§x... atau §a)
     * menjadi tag MiniMessage yang sah (<#rrggbb> atau <green>) agar tidak merusak parser XML.
     */
    public static String toMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";

        // 1. Konversi Spigot Hex §x§r§r§g§g§b§b menjadi <#rrggbb>
        final java.util.regex.Pattern spigotHexPattern = java.util.regex.Pattern.compile("§x§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])");
        java.util.regex.Matcher hexMatcher = spigotHexPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = "#" + hexMatcher.group(1) + hexMatcher.group(2) + hexMatcher.group(3) + hexMatcher.group(4) + hexMatcher.group(5) + hexMatcher.group(6);
            hexMatcher.appendReplacement(sb, "<" + hex + ">");
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();

        // 2. Konversi &#RRGGBB menjadi <#RRGGBB>
        final java.util.regex.Pattern ampBHexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher ampBMatcher = ampBHexPattern.matcher(text);
        sb = new StringBuilder();
        while (ampBMatcher.find()) {
            ampBMatcher.appendReplacement(sb, "<#" + ampBMatcher.group(1) + ">");
        }
        ampBMatcher.appendTail(sb);
        text = sb.toString();

        // 3. Konversi legacy formatting codes (§0-§f dan &0-&f)
        return text
                .replace("§0", "<black>").replace("§1", "<dark_blue>").replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>").replace("§4", "<dark_red>").replace("§5", "<dark_purple>")
                .replace("§6", "<gold>").replace("§7", "<gray>").replace("§8", "<dark_gray>")
                .replace("§9", "<blue>").replace("§a", "<green>").replace("§b", "<aqua>")
                .replace("§c", "<red>").replace("§d", "<light_purple>").replace("§e", "<yellow>")
                .replace("§f", "<white>").replace("§k", "<obfuscated>").replace("§l", "<b>")
                .replace("§m", "<strikethrough>").replace("§n", "<u>").replace("§o", "<i>")
                .replace("§r", "<reset>")
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<b>")
                .replace("&m", "<strikethrough>").replace("&n", "<u>").replace("&o", "<i>")
                .replace("&r", "<reset>");
    }

    public static String translateHexColorCodes(String message) {
        if (message == null) return "";
        final java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + group.charAt(0) + "§" + group.charAt(1)
                    + "§" + group.charAt(2) + "§" + group.charAt(3)
                    + "§" + group.charAt(4) + "§" + group.charAt(5));
        }
        return matcher.appendTail(buffer).toString();
    }

    /**
     * Memberikan efek kilau enchant (glint / glow effect) ke ItemMeta tanpa merusak enchantment asli.
     */
    public static void applyGlow(ItemMeta meta) {
        if (meta == null) return;
        try {
            meta.setEnchantmentGlintOverride(true);
        } catch (Throwable t) {
            try {
                if (!meta.hasEnchants()) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
            } catch (Throwable ignored) {}
        }
    }

    public static ItemStack applyGlow(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyGlow(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
}
