package id.cyayo.forgev2.gui;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeGUI implements CustomForgeGUI {

    private final Inventory inventory;
    private final Player player;
    private final Map<Integer, ItemStack> defaultPlaceholders = new HashMap<>();

    public ForgeGUI(Player player) {
        this.player = player;
        FileConfiguration guiConfig = CyayoForgeV2.getInstance().getConfigManager().getForgeGui();
        int size = guiConfig.getInt("size", 54);
        String title = ChatColor.translateAlternateColorCodes('&',
                guiConfig.getString("title", guiConfig.getString("name", "&8⚒ &6Advanced Forge")));

        this.inventory = Bukkit.createInventory(this, size, title);
        setupDecorations();
    }

    private void setupDecorations() {
        FileConfiguration guiConfig = CyayoForgeV2.getInstance().getConfigManager().getForgeGui();

        // 1. Setup Decorations (Termasuk placeholder untuk slot input/preview)
        ConfigurationSection decorations = guiConfig.getConfigurationSection("decorations");
        if (decorations != null) {
            for (String key : decorations.getKeys(false)) {
                ConfigurationSection section = decorations.getConfigurationSection(key);
                if (section == null) continue;

                ItemStack item = ItemUtil.createDecorationItem(section);
                if (item == null) continue;

                if (section.getBoolean("fill", false)) {
                    for (int i = 0; i < inventory.getSize(); i++) {
                        if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                            inventory.setItem(i, item.clone());
                            defaultPlaceholders.put(i, item.clone());
                        }
                    }
                } else {
                    List<Integer> slots = section.getIntegerList("slots");
                    for (int slot : slots) {
                        if (slot >= 0 && slot < inventory.getSize()) {
                            inventory.setItem(slot, item.clone());
                            defaultPlaceholders.put(slot, item.clone());
                        }
                    }
                }
            }
        }

        // 2. Setup Forge Button
        int buttonSlot = guiConfig.getInt("slots.forge-button", 49);
        ConfigurationSection btnSection = guiConfig.getConfigurationSection("forge-button-item");
        ItemStack button = null;
        if (btnSection != null) {
            Material mat = Material.matchMaterial(btnSection.getString("material", "ANVIL"));
            if (mat == null) mat = Material.ANVIL;
            ConfigurationSection emptySec = btnSection.getConfigurationSection("empty");
            if (emptySec != null) {
                button = ItemUtil.createButton(mat, emptySec.getString("name", "&c&lTEMPA SEKARANG"), emptySec.getStringList("lore"));
            } else {
                button = ItemUtil.createDecorationItem(btnSection);
            }
        }
        if (button == null) {
            button = ItemUtil.createButton(Material.ANVIL, "&c&lTEMPA SEKARANG", null);
        }
        if (buttonSlot >= 0 && buttonSlot < inventory.getSize()) {
            inventory.setItem(buttonSlot, button);
            defaultPlaceholders.put(buttonSlot, button.clone());
        }
    }

    @Override
    public ItemStack getPlaceholder(int slot) {
        ItemStack item = defaultPlaceholders.get(slot);
        return item != null ? item.clone() : null;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
