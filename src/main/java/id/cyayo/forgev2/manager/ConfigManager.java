package id.cyayo.forgev2.manager;

import id.cyayo.forgev2.CyayoForgeV2;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

public class ConfigManager {

    private final CyayoForgeV2 plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration sounds;
    private FileConfiguration materials;
    private FileConfiguration abilities;
    private FileConfiguration traits;
    private FileConfiguration forgeGui;
    private FileConfiguration crusherGui;
    private FileConfiguration rerollGui;
    private FileConfiguration winspectGui;

    public ConfigManager(CyayoForgeV2 plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        messages = loadYaml("messages.yml");
        sounds = loadYaml("sounds.yml");
        materials = loadYaml("materials.yml");
        traits = loadYaml("trait.yml");
        abilities = loadYaml("abilities.yml");
        forgeGui = loadYaml("guis/forge.yml");
        crusherGui = loadYaml("guis/crusher.yml");
        rerollGui = loadYaml("guis/reroll.yml");
        winspectGui = loadYaml("guis/winspect.yml");
    }

    private FileConfiguration loadYaml(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public String getMessage(String path) {
        if (messages == null) return "";
        String msg = messages.getString(path);
        if (msg == null || msg.trim().isEmpty() || msg.equalsIgnoreCase("none") || msg.equalsIgnoreCase("\"\"")) return "";
        String prefix = messages.getString("prefix", "&8[&6CyayoForge&8] ");
        return id.cyayo.forgev2.util.ItemUtil.colorize(prefix + msg);
    }
    
    public String getRawMessage(String path) {
        if (messages == null) return "";
        String msg = messages.getString(path);
        if (msg == null || msg.trim().isEmpty() || msg.equalsIgnoreCase("none") || msg.equalsIgnoreCase("\"\"")) return "";
        return id.cyayo.forgev2.util.ItemUtil.colorize(msg);
    }

    public String getRawMessage(String path, String def) {
        if (messages == null) return (def == null || def.isEmpty()) ? "" : id.cyayo.forgev2.util.ItemUtil.colorize(def);
        String msg = messages.getString(path, def);
        if (msg == null || msg.trim().isEmpty() || msg.equalsIgnoreCase("none") || msg.equalsIgnoreCase("\"\"")) return "";
        return id.cyayo.forgev2.util.ItemUtil.colorize(msg);
    }

    public void sendRawMessage(CommandSender sender, String path) {
        if (sender == null) return;
        String msg = getRawMessage(path);
        if (!msg.isEmpty() && !ChatColor.stripColor(msg).trim().isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public void sendRawMessage(CommandSender sender, String path, String placeholder, String replacement) {
        if (sender == null) return;
        String raw = messages != null ? messages.getString(path, "") : "";
        if (raw == null || raw.isEmpty()) return;
        if (placeholder != null && replacement != null) {
            raw = raw.replace(placeholder, replacement);
        }
        String msg = id.cyayo.forgev2.util.ItemUtil.colorize(raw);
        if (!msg.isEmpty() && !ChatColor.stripColor(msg).trim().isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public void sendRawMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender == null) return;
        String raw = messages != null ? messages.getString(path, "") : "";
        if (raw == null || raw.isEmpty()) return;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    raw = raw.replace(entry.getKey(), entry.getValue());
                }
            }
        }
        String msg = id.cyayo.forgev2.util.ItemUtil.colorize(raw);
        if (!msg.isEmpty() && !ChatColor.stripColor(msg).trim().isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public void sendMessage(CommandSender sender, String path) {
        if (sender == null) return;
        String msg = getMessage(path);
        if (!msg.isEmpty() && !ChatColor.stripColor(msg).trim().isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    /**
     * Memainkan suara dari sounds.yml (sounds.<soundKey> atau <soundKey>) secara dinamis.
     */
    public void playSound(Player player, String soundKey) {
        if (player == null || soundKey == null) return;
        FileConfiguration soundConfig = (sounds != null) ? sounds : config;
        if (soundConfig == null) return;

        String key = soundKey.startsWith("sounds.") ? soundKey : "sounds." + soundKey;
        String soundStr = soundConfig.getString(key + ".sound", soundConfig.getString(soundKey + ".sound", soundConfig.getString(key, soundConfig.getString(soundKey, ""))));
        if (soundStr == null || soundStr.trim().isEmpty() || soundStr.equalsIgnoreCase("none")) return;

        double volume = soundConfig.getDouble(key + ".volume", soundConfig.getDouble(soundKey + ".volume", 1.0));
        double pitch = soundConfig.getDouble(key + ".pitch", soundConfig.getDouble(soundKey + ".pitch", 1.0));

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundStr.trim().toUpperCase().replace(" ", "_").replace(".", "_"));
            player.playSound(player.getLocation(), sound, (float) volume, (float) pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("[CyayoForgeV2] Sound tidak valid di sounds.yml (" + soundKey + "): " + soundStr);
        }
    }

    public String getMessageOr(String path, String def) {
        return getRawMessage(path, def);
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getSounds() { return sounds; }
    public FileConfiguration getMaterials() { return materials; }
    public FileConfiguration getAbilities() { return traits != null ? traits : abilities; }
    public FileConfiguration getTraits() { return traits != null ? traits : abilities; }
    public FileConfiguration getForgeGui() { return forgeGui; }
    public FileConfiguration getCrusherGui() { return crusherGui; }
    public FileConfiguration getRerollGui() { return rerollGui; }
    public FileConfiguration getWinspectGui() { return winspectGui; }
}
