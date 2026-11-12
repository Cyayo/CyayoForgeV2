package id.cyayo.forgev2.listener;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.gui.CrusherGUI;
import id.cyayo.forgev2.gui.CustomForgeGUI;
import id.cyayo.forgev2.gui.ForgeGUI;
import id.cyayo.forgev2.gui.RerollGUI;
import id.cyayo.forgev2.gui.WinspectGUI;
import id.cyayo.forgev2.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeListener implements Listener {

    private final CyayoForgeV2 plugin;

    public ForgeListener(CyayoForgeV2 plugin) {
        this.plugin = plugin;
    }

    /**
     * Blokir semua drag event ke dalam inventori GUI atas untuk mencegah
     * item dekorasi tergeser, tertimpa, atau diduplikasi.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();
        if (holder instanceof CustomForgeGUI || holder instanceof WinspectGUI) {
            int topSize = topInv.getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();

        // Abaikan jika bukan GUI milik plugin ini
        if (!(holder instanceof CustomForgeGUI) && !(holder instanceof WinspectGUI)) {
            return;
        }

        // WinspectGUI murni view-only: semua interaksi dibatalkan total
        if (holder instanceof WinspectGUI) {
            event.setCancelled(true);
            return;
        }

        CustomForgeGUI gui = (CustomForgeGUI) holder;
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInv = event.getClickedInventory();

        // 1. Pengaman Double-Click: Mencegah Bukkit mengumpulkan item/kaca sejenis dari GUI
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        // 2. Pengaman Hotbar Swap (angka 1-9) dan Offhand Swap (F)
        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }

        // 3. Klik di luar jendela inventori
        if (clickedInv == null) {
            return;
        }

        // 4. Interaksi di inventori player (Bottom Inventory)
        if (clickedInv.equals(event.getView().getBottomInventory())) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                handleShiftClickToGui(player, topInv, gui, event.getCurrentItem(), event.getSlot());
            }
            // Jika bukan shift-click, izinkan interaksi normal di dalam tas sendiri
            return;
        }

        // 5. Interaksi di dalam inventori GUI (Top Inventory)
        // Baseline: Selalu cancel agar dekorasi dan slot non-editable tidak bisa diambil sama sekali
        event.setCancelled(true);

        if (holder instanceof ForgeGUI) {
            handleForgeClick(event, player, topInv, (ForgeGUI) gui);
        } else if (holder instanceof CrusherGUI) {
            handleCrusherClick(event, player, topInv, (CrusherGUI) gui);
        } else if (holder instanceof RerollGUI) {
            handleRerollClick(event, player, topInv, (RerollGUI) gui);
        }
    }

    // =========================================================================
    // FORGE GUI LOGIC
    // =========================================================================
    private void handleForgeClick(InventoryClickEvent event, Player player, Inventory inv, ForgeGUI gui) {
        FileConfiguration guiConfig = plugin.getConfigManager().getForgeGui();
        int bpSlot = guiConfig.getInt("slots.blueprint-slot", 4);
        int buttonSlot = guiConfig.getInt("slots.forge-button", 49);
        int previewSlot = guiConfig.getInt("slots.preview-slot", 40);
        
        List<Integer> mat1Slots = guiConfig.getIntegerList("slots.material-1");
        if (mat1Slots.isEmpty()) mat1Slots = guiConfig.getIntegerList("slots.material-mob");

        List<Integer> mat2Slots = guiConfig.getIntegerList("slots.material-2");
        if (mat2Slots.isEmpty()) mat2Slots = guiConfig.getIntegerList("slots.material-fish");

        List<Integer> mat3Slots = guiConfig.getIntegerList("slots.material-3");
        if (mat3Slots.isEmpty()) mat3Slots = guiConfig.getIntegerList("slots.material-mine");

        int slot = event.getSlot();

        // 1. Tombol Forge
        if (slot == buttonSlot) {
            ItemStack bp = inv.getItem(bpSlot);
            if (bp == null || bp.getType().isAir() || ItemUtil.isDecoration(bp) || !plugin.getForgeManager().isBlueprint(bp)) {
                plugin.getConfigManager().sendRawMessage(player, "error.put-blueprint-first");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }
            if (!canUseBlueprint(player, bp)) {
                return;
            }

            io.lumine.mythic.lib.api.item.NBTItem bpNbt = io.lumine.mythic.lib.api.item.NBTItem.get(bp);
            List<id.cyayo.forgev2.model.BlueprintRequirement> reqList = plugin.getForgeManager().getRequirements(bpNbt);

            id.cyayo.forgev2.model.BlueprintRequirement r1 = (reqList.size() > 0) ? reqList.get(0) : null;
            id.cyayo.forgev2.model.BlueprintRequirement r2 = (reqList.size() > 1) ? reqList.get(1) : null;
            id.cyayo.forgev2.model.BlueprintRequirement r3 = (reqList.size() > 2) ? reqList.get(2) : null;

            int req1 = (r1 != null) ? r1.getAmountRequired() : 0;
            int req2 = (r2 != null) ? r2.getAmountRequired() : 0;
            int req3 = (r3 != null) ? r3.getAmountRequired() : 0;

            String cat1 = (r1 != null) ? r1.getCategoryKey() : "NONE";
            String cat2 = (r2 != null) ? r2.getCategoryKey() : "NONE";
            String cat3 = (r3 != null) ? r3.getCategoryKey() : "NONE";

            int total1 = (r1 != null) ? countMaterials(inv, mat1Slots, cat1) : 0;
            int total2 = (r2 != null) ? countMaterials(inv, mat2Slots, cat2) : 0;
            int total3 = (r3 != null) ? countMaterials(inv, mat3Slots, cat3) : 0;

            // Validasi kelengkapan material
            if (total1 < req1 || total2 < req2 || total3 < req3) {
                plugin.getConfigManager().sendRawMessage(player, "error.missing-materials");

                String name1 = plugin.getForgeManager().getCategoryName(cat1);
                String name2 = plugin.getForgeManager().getCategoryName(cat2);
                String name3 = plugin.getForgeManager().getCategoryName(cat3);

                String statusPattern = plugin.getConfigManager().getRawMessage("error.material-status", "&7Syarat: %status%");
                String itemPattern = plugin.getConfigManager().getRawMessage("materials.status-format", "%category%: &f%current%/%required%");
                String sep = plugin.getConfigManager().getRawMessage("materials.status-separator", " &8| ");

                List<String> statusParts = new ArrayList<>();
                if (r1 != null && r1.isValid()) {
                    statusParts.add(itemPattern.replace("%category%", name1).replace("%current%", String.valueOf(total1)).replace("%required%", String.valueOf(req1)));
                }
                if (r2 != null && r2.isValid()) {
                    statusParts.add(itemPattern.replace("%category%", name2).replace("%current%", String.valueOf(total2)).replace("%required%", String.valueOf(req2)));
                }
                if (r3 != null && r3.isValid()) {
                    statusParts.add(itemPattern.replace("%category%", name3).replace("%current%", String.valueOf(total3)).replace("%required%", String.valueOf(req3)));
                }

                String joined = String.join(sep, statusParts);
                String formattedStatus = statusPattern.replace("%status%", joined);
                if (formattedStatus != null && !formattedStatus.trim().isEmpty() && !ChatColor.stripColor(formattedStatus).trim().isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedStatus));
                }
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            List<Integer> ingSlots = guiConfig.getIntegerList("slots.ingredients");
            Map<Integer, ItemStack> ingredients = collectActiveIngredients(inv, ingSlots);

            // Konsumsi material sejumlah syarat yang ditentukan
            List<ItemStack> consumed1 = (r1 != null && req1 > 0) ? consumeMaterials(inv, mat1Slots, req1, gui) : Collections.emptyList();
            List<ItemStack> consumed2 = (r2 != null && req2 > 0) ? consumeMaterials(inv, mat2Slots, req2, gui) : Collections.emptyList();
            List<ItemStack> consumed3 = (r3 != null && req3 > 0) ? consumeMaterials(inv, mat3Slots, req3, gui) : Collections.emptyList();

            // Eksekusi proses forging
            ItemStack result = plugin.getForgeManager().craftItem(player, bp, consumed1, consumed2, consumed3, ingredients);
            if (result == null) {
                plugin.getConfigManager().sendRawMessage(player, "error.craft-error");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            // Konsumsi 1 item dari setiap slot ingredient yang terisi
            for (int is : ingSlots) {
                if (is < 0 || is >= inv.getSize()) continue;
                ItemStack curIng = inv.getItem(is);
                if (curIng != null && !curIng.getType().isAir() && !ItemUtil.isDecoration(curIng)) {
                    if (curIng.getAmount() > 1) {
                        curIng.setAmount(curIng.getAmount() - 1);
                        inv.setItem(is, curIng);
                    } else {
                        inv.setItem(is, gui.getPlaceholder(is));
                    }
                }
            }

            // Konsumsi 1 Blueprint
            if (bp.getAmount() > 1) {
                bp.setAmount(bp.getAmount() - 1);
                inv.setItem(bpSlot, bp);
            } else {
                inv.setItem(bpSlot, gui.getPlaceholder(bpSlot));
            }

            // Perbarui slot preview (jika blueprint habis, kembalikan ke placeholder)
            updatePreviewSlot(player, inv, gui);

            // Hasil tempaan LANGSUNG masuk ke Inventory Pemain (atau drop jika tas penuh)
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }

            plugin.getConfigManager().sendRawMessage(player, "success.forged");
            plugin.getConfigManager().playSound(player, "forge-success");
            checkBroadcast(player, result);
            updateForgeButton(player, inv);
            return;
        }

        // 2. Slot Output (Preview) - Murni Display Pratinjau
        if (slot == previewSlot) {
            return;
        }

        // 3. Slot Input (Blueprint & Material)
        if (slot == bpSlot) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isBlueprint(cursor)) {
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-blueprint");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                if (!canUseBlueprint(player, cursor)) {
                    return;
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event);
            updatePreviewSlot(player, inv, gui);
            updateForgeButton(player, inv);
            return;
        }

        // Cek Blueprint yang sedang terpasang untuk validasi kategori dinamis
        ItemStack activeBp = inv.getItem(bpSlot);
        io.lumine.mythic.lib.api.item.NBTItem activeBpNbt = (activeBp != null && !activeBp.getType().isAir() && !ItemUtil.isDecoration(activeBp)) ? io.lumine.mythic.lib.api.item.NBTItem.get(activeBp) : null;
        List<id.cyayo.forgev2.model.BlueprintRequirement> activeReqs = (activeBpNbt != null) ? plugin.getForgeManager().getRequirements(activeBpNbt) : Collections.emptyList();

        id.cyayo.forgev2.model.BlueprintRequirement req1 = (activeReqs.size() > 0) ? activeReqs.get(0) : null;
        id.cyayo.forgev2.model.BlueprintRequirement req2 = (activeReqs.size() > 1) ? activeReqs.get(1) : null;
        id.cyayo.forgev2.model.BlueprintRequirement req3 = (activeReqs.size() > 2) ? activeReqs.get(2) : null;

        if (mat1Slots.contains(slot)) {
            String catKey = (req1 != null) ? req1.getCategoryKey() : "MATERIAL_MOB";
            int reqAmount = (req1 != null) ? req1.getAmountRequired() : 64;
            int curColTotal = countMaterials(inv, mat1Slots, catKey);
            int remainingNeeded = Math.max(0, reqAmount - curColTotal);

            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isValidMaterial(cursor, catKey)) {
                    String catName = plugin.getForgeManager().getCategoryName(catKey);
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-material-slot", "%category%", catName);
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event, remainingNeeded);
            updatePreviewSlot(player, inv, gui);
            updateForgeButton(player, inv);
            return;
        }

        if (mat2Slots.contains(slot)) {
            String catKey = (req2 != null) ? req2.getCategoryKey() : "MATERIAL_FISH";
            if (req2 == null && activeBpNbt != null) {
                plugin.getConfigManager().playSound(player, "error");
                return; // Slot ini tidak dibutuhkan oleh blueprint aktif
            }
            int reqAmount = (req2 != null) ? req2.getAmountRequired() : 64;
            int curColTotal = countMaterials(inv, mat2Slots, catKey);
            int remainingNeeded = Math.max(0, reqAmount - curColTotal);

            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isValidMaterial(cursor, catKey)) {
                    String catName = plugin.getForgeManager().getCategoryName(catKey);
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-material-slot", "%category%", catName);
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event, remainingNeeded);
            updatePreviewSlot(player, inv, gui);
            updateForgeButton(player, inv);
            return;
        }

        if (mat3Slots.contains(slot)) {
            String catKey = (req3 != null) ? req3.getCategoryKey() : "MATERIAL_MINE";
            if (req3 == null && activeBpNbt != null) {
                plugin.getConfigManager().playSound(player, "error");
                return; // Slot ini tidak dibutuhkan oleh blueprint aktif
            }
            int reqAmount = (req3 != null) ? req3.getAmountRequired() : 64;
            int curColTotal = countMaterials(inv, mat3Slots, catKey);
            int remainingNeeded = Math.max(0, reqAmount - curColTotal);

            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isValidMaterial(cursor, catKey)) {
                    String catName = plugin.getForgeManager().getCategoryName(catKey);
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-material-slot", "%category%", catName);
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event, remainingNeeded);
            updatePreviewSlot(player, inv, gui);
            updateForgeButton(player, inv);
            return;
        }

        List<Integer> ingSlots = guiConfig.getIntegerList("slots.ingredients");
        if (ingSlots.contains(slot)) {
            handleIngredientSlotClick(player, inv, gui, slot, event);
            updatePreviewSlot(player, inv, gui);
            updateForgeButton(player, inv);
            return;
        }
        // Slot dekorasi lain otomatis dibatalkan (terproteksi)
    }

    private int countMaterials(Inventory inv, List<Integer> slots, String categoryKey) {
        int total = 0;
        for (int s : slots) {
            if (s < 0 || s >= inv.getSize()) continue;
            ItemStack item = inv.getItem(s);
            if (item != null && !item.getType().isAir() && !ItemUtil.isDecoration(item)) {
                if (plugin.getForgeManager().isValidMaterial(item, categoryKey)) {
                    total += item.getAmount();
                }
            }
        }
        return total;
    }

    private List<ItemStack> consumeMaterials(Inventory inv, List<Integer> slots, int amountToConsume, CustomForgeGUI gui) {
        List<ItemStack> consumed = new ArrayList<>();
        int remaining = amountToConsume;
        for (int s : slots) {
            if (remaining <= 0) break;
            if (s < 0 || s >= inv.getSize()) continue;
            ItemStack item = inv.getItem(s);
            if (item == null || item.getType().isAir() || ItemUtil.isDecoration(item)) continue;

            int currentAmount = item.getAmount();
            if (currentAmount <= remaining) {
                consumed.add(item.clone());
                remaining -= currentAmount;
                inv.setItem(s, gui.getPlaceholder(s));
            } else {
                ItemStack part = item.clone();
                part.setAmount(remaining);
                consumed.add(part);

                item.setAmount(currentAmount - remaining);
                inv.setItem(s, item);
                remaining = 0;
            }
        }
        return consumed;
    }

    private void checkBroadcast(Player player, ItemStack item) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (!config.getBoolean("broadcast.enabled", true)) return;
        if (item == null || item.getType().isAir()) return;

        io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
        if (!nbt.hasType()) return;

        String tier = nbt.getString("MMOITEMS_TIER");
        if (tier == null || tier.isEmpty()) return;

        List<String> triggerTiers = config.getStringList("broadcast.trigger-tiers");
        if (triggerTiers.contains(tier)) {
            List<String> lines = new ArrayList<>();
            if (config.isList("broadcast.format." + tier)) {
                lines.addAll(config.getStringList("broadcast.format." + tier));
            } else if (config.isString("broadcast.format." + tier)) {
                lines.add(config.getString("broadcast.format." + tier));
            } else if (config.isList("broadcast.format.default")) {
                lines.addAll(config.getStringList("broadcast.format.default"));
            } else if (config.isString("broadcast.format.default")) {
                lines.add(config.getString("broadcast.format.default"));
            }

            if (!lines.isEmpty()) {
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

                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ?
                        item.getItemMeta().getDisplayName() : item.getType().name();

                String rawQualityStr = (quality == (long) quality) ? String.valueOf((long) quality) : String.format(java.util.Locale.US, "%.1f", quality);
                String qualityTemplateStr = plugin.getForgeManager().getQualityMiniMessageTemplate(quality);
                String coloredQualityStr = plugin.getForgeManager().formatQualityPercentage(quality);

                for (String rawLine : lines) {
                    if (rawLine == null) continue;
                    String formatted = rawLine
                            .replace("%player%", player.getName())
                            .replace("%item%", itemName)
                            .replace("%percentage%", qualityTemplateStr)
                            .replace("%quality%", rawQualityStr)
                            .replace("%colored_percentage%", coloredQualityStr);

                    String msg = ItemUtil.colorize(formatted);
                    org.bukkit.Bukkit.broadcastMessage(msg);
                }
            }
        }
    }

    private void refreshPlayerView(Player player, Inventory inv) {
        if (player == null) return;
        player.updateInventory();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) {
                player.updateInventory();
            }
        });
    }

    private List<ItemStack> collectValidMaterials(Inventory inv, List<Integer> slots) {
        List<ItemStack> list = new ArrayList<>();
        if (slots == null || slots.isEmpty()) return list;
        for (int s : slots) {
            if (s >= 0 && s < inv.getSize()) {
                ItemStack item = inv.getItem(s);
                if (item != null && !item.getType().isAir() && !ItemUtil.isDecoration(item)) {
                    list.add(item.clone());
                }
            }
        }
        return list;
    }

    private void updatePreviewSlot(Player player, Inventory inv, ForgeGUI gui) {
        FileConfiguration guiConfig = plugin.getConfigManager().getForgeGui();
        int bpSlot = guiConfig.getInt("slots.blueprint-slot", 4);
        int previewSlot = guiConfig.getInt("slots.preview-slot", 40);

        ItemStack bp = inv.getItem(bpSlot);
        if (bp != null && !bp.getType().isAir() && !ItemUtil.isDecoration(bp) && plugin.getForgeManager().isBlueprint(bp)) {
            List<Integer> mat1Slots = guiConfig.getIntegerList("slots.material-1");
            if (mat1Slots.isEmpty()) mat1Slots = guiConfig.getIntegerList("slots.material-mob");
            List<Integer> mat2Slots = guiConfig.getIntegerList("slots.material-2");
            if (mat2Slots.isEmpty()) mat2Slots = guiConfig.getIntegerList("slots.material-fish");
            List<Integer> mat3Slots = guiConfig.getIntegerList("slots.material-3");
            if (mat3Slots.isEmpty()) mat3Slots = guiConfig.getIntegerList("slots.material-mine");

            List<ItemStack> mat1List = collectValidMaterials(inv, mat1Slots);
            List<ItemStack> mat2List = collectValidMaterials(inv, mat2Slots);
            List<ItemStack> mat3List = collectValidMaterials(inv, mat3Slots);

            List<Integer> ingSlots = guiConfig.getIntegerList("slots.ingredients");
            Map<Integer, ItemStack> ingredients = collectActiveIngredients(inv, ingSlots);

            ItemStack preview = plugin.getForgeManager().generatePreview(player, bp, mat1List, mat2List, mat3List, ingredients);
            if (preview != null) {
                inv.setItem(previewSlot, preview);
                refreshPlayerView(player, inv);
                return;
            }
        }
        inv.setItem(previewSlot, gui.getPlaceholder(previewSlot));
        refreshPlayerView(player, inv);
    }

    private void updatePreviewSlot(Inventory inv, ForgeGUI gui) {
        Player player = null;
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof Player p) {
            player = p;
        }
        updatePreviewSlot(player, inv, gui);
    }

    public void updateForgeButton(Player player, Inventory inv) {
        FileConfiguration guiConfig = plugin.getConfigManager().getForgeGui();
        int buttonSlot = guiConfig.getInt("slots.forge-button", 49);
        int bpSlot = guiConfig.getInt("slots.blueprint-slot", 4);
        
        List<Integer> mat1Slots = guiConfig.getIntegerList("slots.material-1");
        if (mat1Slots.isEmpty()) mat1Slots = guiConfig.getIntegerList("slots.material-mob");
        List<Integer> mat2Slots = guiConfig.getIntegerList("slots.material-2");
        if (mat2Slots.isEmpty()) mat2Slots = guiConfig.getIntegerList("slots.material-fish");
        List<Integer> mat3Slots = guiConfig.getIntegerList("slots.material-3");
        if (mat3Slots.isEmpty()) mat3Slots = guiConfig.getIntegerList("slots.material-mine");

        org.bukkit.configuration.ConfigurationSection btnSec = guiConfig.getConfigurationSection("forge-button-item");
        if (btnSec == null) return;

        Material mat = Material.matchMaterial(btnSec.getString("material", "ANVIL"));
        if (mat == null) mat = Material.ANVIL;

        String fulfilledFormat = btnSec.getString("status-format.fulfilled", "&a%current%/%required% &a✔");
        String unfulfilledFormat = btnSec.getString("status-format.unfulfilled", "&c%current%/%required% &c✗");

        ItemStack bp = inv.getItem(bpSlot);
        boolean hasValidBp = (bp != null && !bp.getType().isAir() && !ItemUtil.isDecoration(bp) && plugin.getForgeManager().isBlueprint(bp));

        String state = "empty";
        String targetItemName = "None";

        String name1 = "None", name2 = "None", name3 = "None";
        String s1 = "", s2 = "", s3 = "";
        boolean allFulfilled = true;

        if (hasValidBp) {
            io.lumine.mythic.lib.api.item.NBTItem bpNbt = io.lumine.mythic.lib.api.item.NBTItem.get(bp);
            List<id.cyayo.forgev2.model.BlueprintRequirement> reqList = plugin.getForgeManager().getRequirements(bpNbt);

            id.cyayo.forgev2.model.BlueprintRequirement r1 = (reqList.size() > 0) ? reqList.get(0) : null;
            id.cyayo.forgev2.model.BlueprintRequirement r2 = (reqList.size() > 1) ? reqList.get(1) : null;
            id.cyayo.forgev2.model.BlueprintRequirement r3 = (reqList.size() > 2) ? reqList.get(2) : null;

            int req1 = (r1 != null) ? r1.getAmountRequired() : 0;
            int req2 = (r2 != null) ? r2.getAmountRequired() : 0;
            int req3 = (r3 != null) ? r3.getAmountRequired() : 0;

            String cat1 = (r1 != null) ? r1.getCategoryKey() : "NONE";
            String cat2 = (r2 != null) ? r2.getCategoryKey() : "NONE";
            String cat3 = (r3 != null) ? r3.getCategoryKey() : "NONE";

            int tot1 = (r1 != null) ? countMaterials(inv, mat1Slots, cat1) : 0;
            int tot2 = (r2 != null) ? countMaterials(inv, mat2Slots, cat2) : 0;
            int tot3 = (r3 != null) ? countMaterials(inv, mat3Slots, cat3) : 0;

            name1 = (r1 != null) ? plugin.getForgeManager().getCategoryName(cat1) : "-";
            name2 = (r2 != null) ? plugin.getForgeManager().getCategoryName(cat2) : "-";
            name3 = (r3 != null) ? plugin.getForgeManager().getCategoryName(cat3) : "-";

            s1 = (r1 != null && req1 > 0) ? (tot1 >= req1 ? fulfilledFormat : unfulfilledFormat).replace("%current%", String.valueOf(tot1)).replace("%required%", String.valueOf(req1)) : "§7-";
            s2 = (r2 != null && req2 > 0) ? (tot2 >= req2 ? fulfilledFormat : unfulfilledFormat).replace("%current%", String.valueOf(tot2)).replace("%required%", String.valueOf(req2)) : "§7-";
            s3 = (r3 != null && req3 > 0) ? (tot3 >= req3 ? fulfilledFormat : unfulfilledFormat).replace("%current%", String.valueOf(tot3)).replace("%required%", String.valueOf(req3)) : "§7-";

            if (r1 != null && req1 > 0 && tot1 < req1) allFulfilled = false;
            if (r2 != null && req2 > 0 && tot2 < req2) allFulfilled = false;
            if (r3 != null && req3 > 0 && tot3 < req3) allFulfilled = false;

            String targetType = plugin.getForgeManager().getTargetType(bpNbt);
            String targetId = plugin.getForgeManager().getTargetId(bpNbt);
            net.Indyuce.mmoitems.api.Type t = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(targetType);
            if (t == null) t = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(targetType.toUpperCase());
            if (t != null) {
                net.Indyuce.mmoitems.api.item.template.MMOItemTemplate tmpl = net.Indyuce.mmoitems.MMOItems.plugin.getTemplates().getTemplate(t, targetId);
                if (tmpl == null) tmpl = net.Indyuce.mmoitems.MMOItems.plugin.getTemplates().getTemplate(t, targetId.toUpperCase());
                if (tmpl != null) {
                    ItemStack dummy = tmpl.newBuilder(0, null).build().newBuilder().build();
                    if (dummy != null && dummy.hasItemMeta() && dummy.getItemMeta().hasDisplayName()) {
                        targetItemName = dummy.getItemMeta().getDisplayName();
                    } else {
                        targetItemName = targetId.replace("_", " ");
                    }
                }
            }

            state = allFulfilled ? "ready" : "missing-materials";
        }

        String name = btnSec.getString(state + ".name", "&a&lTEMPA SEKARANG");
        List<String> rawLore = btnSec.getStringList(state + ".lore");

        List<String> formattedLore = new ArrayList<>();
        for (String l : rawLore) {
            String f = l.replace("%target_item%", targetItemName)
                        .replace("%material_1%", name1).replace("%status_1%", s1)
                        .replace("%material_2%", name2).replace("%status_2%", s2)
                        .replace("%material_3%", name3).replace("%status_3%", s3);
            formattedLore.add(ChatColor.translateAlternateColorCodes('&', f));
        }

        ItemStack button = ItemUtil.createButton(mat, name, formattedLore);
        inv.setItem(buttonSlot, button);
        refreshPlayerView(player, inv);
    }

    public void updateForgeButton(Inventory inv) {
        Player player = null;
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof Player p) {
            player = p;
        }
        updateForgeButton(player, inv);
    }

    public void updateCrusherButton(Player player, Inventory inv) {
        FileConfiguration guiConfig = plugin.getConfigManager().getCrusherGui();
        int buttonSlot = guiConfig.getInt("slots.crush-button", 49);
        List<Integer> itemSlots = guiConfig.getIntegerList("slots.items-to-crush");

        org.bukkit.configuration.ConfigurationSection btnSec = guiConfig.getConfigurationSection("crush-button-item");
        if (btnSec == null) return;

        Material mat = Material.matchMaterial(btnSec.getString("material", "ANVIL"));
        if (mat == null) mat = Material.ANVIL;

        int itemCount = 0;
        Map<String, Integer> estimatedFrags = new HashMap<>();

        for (int s : itemSlots) {
            if (s < 0 || s >= inv.getSize()) continue;
            ItemStack item = inv.getItem(s);
            if (item == null || item.getType().isAir() || ItemUtil.isDecoration(item)) continue;

            if (plugin.getForgeManager().isValidCrushItem(item)) {
                itemCount += item.getAmount();
                io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                String tier = nbt.getString("MMOITEMS_TIER");
                if (tier == null || tier.isEmpty()) tier = nbt.getString("CYAYO_FORGE_TIER");
                if (tier == null || tier.isEmpty()) tier = plugin.getForgeManager().getTargetTier(nbt);

                String fragKey = plugin.getConfigManager().getConfig().getString("fragments.mapping." + tier,
                        plugin.getConfigManager().getConfig().getString("fragments.default-fragment", "NONE"));
                if (!fragKey.equalsIgnoreCase("NONE") && !fragKey.isEmpty()) {
                    estimatedFrags.put(fragKey, estimatedFrags.getOrDefault(fragKey, 0) + item.getAmount());
                }
            }
        }

        String state = (itemCount > 0 && !estimatedFrags.isEmpty()) ? "ready" : "empty";
        String name = btnSec.getString(state + ".name", "&d&lHANCURKAN GEAR");
        List<String> rawLore = btnSec.getStringList(state + ".lore");
        String resultLineFormat = btnSec.getString("fragment-result-line", "  &8✦ &d%fragment_name%: &f%amount%x");

        List<String> formattedLore = new ArrayList<>();
        for (String l : rawLore) {
            if (l.contains("%fragment_results%")) {
                for (Map.Entry<String, Integer> entry : estimatedFrags.entrySet()) {
                    String fragKey = entry.getKey();
                    int amt = entry.getValue();
                    String fragName = plugin.getForgeManager().getFragmentDisplayName(fragKey);
                    String line = resultLineFormat.replace("%fragment_name%", fragName).replace("%amount%", String.valueOf(amt));
                    formattedLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            } else {
                String f = l.replace("%item_count%", String.valueOf(itemCount));
                formattedLore.add(ChatColor.translateAlternateColorCodes('&', f));
            }
        }

        ItemStack button = ItemUtil.createButton(mat, name, formattedLore);
        inv.setItem(buttonSlot, button);
        refreshPlayerView(player, inv);
    }

    public void updateCrusherButton(Inventory inv) {
        Player player = null;
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof Player p) {
            player = p;
        }
        updateCrusherButton(player, inv);
    }

    public void updateRerollButton(Player player, Inventory inv) {
        FileConfiguration guiConfig = plugin.getConfigManager().getRerollGui();
        int buttonSlot = guiConfig.getInt("slots.reroll-button", 40);
        int itemSlot = guiConfig.getInt("slots.item-target", 21);
        int fragSlot = guiConfig.getInt("slots.fragment-slot", 23);

        org.bukkit.configuration.ConfigurationSection btnSec = guiConfig.getConfigurationSection("reroll-button-item");
        if (btnSec == null) return;

        Material mat = Material.matchMaterial(btnSec.getString("material", "ANVIL"));
        if (mat == null) mat = Material.ANVIL;

        String fulfilledFormat = btnSec.getString("status-format.fulfilled", "&a%current%/%required% &a✔");
        String unfulfilledFormat = btnSec.getString("status-format.unfulfilled", "&c%current%/%required% &c✗");

        ItemStack targetItem = inv.getItem(itemSlot);
        ItemStack fragItem = inv.getItem(fragSlot);

        boolean hasTarget = (targetItem != null && !targetItem.getType().isAir() && !ItemUtil.isDecoration(targetItem) && plugin.getForgeManager().isValidRerollTarget(targetItem));

        String state = "empty";
        String targetItemName = "None";
        String tier = "None";
        String fragName = "None";
        int currentFrag = 0;
        int reqFrag = 1;
        boolean matchingFrag = false;

        id.cyayo.forgev2.manager.ForgeManager.RerollCostInfo costInfo = new id.cyayo.forgev2.manager.ForgeManager.RerollCostInfo(0, 0, 0, "", "0");

        if (hasTarget) {
            targetItemName = targetItem.hasItemMeta() && targetItem.getItemMeta().hasDisplayName() ? targetItem.getItemMeta().getDisplayName() : targetItem.getType().name();
            io.lumine.mythic.lib.api.item.NBTItem targetNbt = io.lumine.mythic.lib.api.item.NBTItem.get(targetItem);
            
            String tierId = targetNbt.getString("CYAYO_FORGE_TIER");
            if (tierId == null || tierId.isEmpty()) tierId = targetNbt.getString("MMOITEMS_TIER");
            if (tierId == null || tierId.isEmpty()) tierId = plugin.getForgeManager().getTargetTier(targetNbt);

            net.Indyuce.mmoitems.api.ItemTier mmoTier = plugin.getForgeManager().resolveItemTier(tierId);
            tier = (mmoTier != null && mmoTier.getName() != null && !mmoTier.getName().isEmpty())
                    ? ChatColor.translateAlternateColorCodes('&', mmoTier.getName())
                    : (tierId != null && !tierId.isEmpty() ? tierId : "None");

            costInfo = plugin.getForgeManager().calculateRerollCost(player, tierId);

            String fragKey = plugin.getConfigManager().getConfig().getString("fragments.mapping." + tierId);
            if (fragKey == null || fragKey.isEmpty()) {
                fragKey = plugin.getConfigManager().getConfig().getString("fragments.default-fragment", "NONE");
            }

            if (fragKey != null && fragKey.contains(":")) {
                String[] split = fragKey.split(":");
                ItemStack sampleFrag = net.Indyuce.mmoitems.MMOItems.plugin.getItem(split[0], split[1]);
                if (sampleFrag != null && sampleFrag.hasItemMeta() && sampleFrag.getItemMeta().hasDisplayName()) {
                    fragName = sampleFrag.getItemMeta().getDisplayName();
                } else {
                    fragName = split[1].replace("_", " ");
                }
            }

            if (fragItem != null && !fragItem.getType().isAir() && !ItemUtil.isDecoration(fragItem) && plugin.getForgeManager().isValidFragment(fragItem)) {
                if (plugin.getForgeManager().isMatchingFragment(targetItem, fragItem)) {
                    matchingFrag = true;
                    currentFrag = fragItem.getAmount();
                }
            }

            if (matchingFrag && currentFrag >= reqFrag) {
                state = "ready";
            } else {
                state = "not-ready";
            }
        }

        int rerollCount = 0;
        int pityCount = 0;
        List<String> targetLoreList = new ArrayList<>();
        if (hasTarget) {
            io.lumine.mythic.lib.api.item.NBTItem targetNbt = io.lumine.mythic.lib.api.item.NBTItem.get(targetItem);
            if (targetNbt.hasTag("CYAYO_FORGE_REROLL_COUNT")) {
                rerollCount = targetNbt.getInteger("CYAYO_FORGE_REROLL_COUNT");
            } else if (targetNbt.hasTag("FORGE_REROLL_COUNT")) {
                try {
                    rerollCount = (int) Math.round(targetNbt.getDouble("FORGE_REROLL_COUNT"));
                } catch (Exception ignored) {}
            }

            if (targetNbt.hasTag("CYAYO_FORGE_PITY_COUNT")) {
                pityCount = targetNbt.getInteger("CYAYO_FORGE_PITY_COUNT");
            } else {
                pityCount = rerollCount;
            }

            if (targetItem.hasItemMeta() && targetItem.getItemMeta().hasLore()) {
                targetLoreList = targetItem.getItemMeta().getLore();
            }
        }

        String name = btnSec.getString(state + ".name", "&b&lREROLL STATS");
        List<String> rawLore = btnSec.getStringList(state + ".lore");

        String fragStatus = (matchingFrag && currentFrag >= reqFrag ? fulfilledFormat : unfulfilledFormat)
                .replace("%current%", String.valueOf(currentFrag))
                .replace("%required%", String.valueOf(reqFrag));

        List<String> formattedLore = new ArrayList<>();
        for (String l : rawLore) {
            if (l.contains("%item_lore%") || l.contains("%target_lore%")) {
                if (targetLoreList != null && !targetLoreList.isEmpty()) {
                    for (String loreLine : targetLoreList) {
                        formattedLore.add(loreLine);
                    }
                } else {
                    formattedLore.add(ChatColor.translateAlternateColorCodes('&', "  &8- &7(Tidak ada stat)"));
                }
            } else {
                String f = l.replace("%target_item%", targetItemName)
                            .replace("%tier%", tier)
                            .replace("%cost%", costInfo.displayCost)
                            .replace("%base_cost%", String.valueOf((long) costInfo.baseCost))
                            .replace("%final_cost%", String.valueOf((long) costInfo.finalCost))
                            .replace("%discount_badge%", costInfo.badge)
                            .replace("%discount_percent%", String.valueOf(costInfo.discountPercent))
                            .replace("%fragment_name%", fragName)
                            .replace("%fragment_status%", fragStatus)
                            .replace("%pity_count%", String.valueOf(pityCount))
                            .replace("%reroll_count%", String.valueOf(rerollCount));
                formattedLore.add(ChatColor.translateAlternateColorCodes('&', f));
            }
        }

        ItemStack button = ItemUtil.createButton(mat, name, formattedLore);
        inv.setItem(buttonSlot, button);
        refreshPlayerView(player, inv);
    }

    public void updateRerollButton(Inventory inv) {
        Player player = null;
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof Player p) {
            player = p;
        }
        updateRerollButton(player, inv);
    }

    // =========================================================================
    // CRUSHER GUI LOGIC
    // =========================================================================
    private void handleCrusherClick(InventoryClickEvent event, Player player, Inventory inv, CrusherGUI gui) {
        FileConfiguration guiConfig = plugin.getConfigManager().getCrusherGui();
        int buttonSlot = guiConfig.getInt("slots.crush-button", 49);
        List<Integer> itemSlots = guiConfig.getIntegerList("slots.items-to-crush");
        List<Integer> resultSlots = guiConfig.getIntegerList("slots.fragment-results");

        int slot = event.getSlot();

        // 1. Tombol Crush
        if (slot == buttonSlot) {
            FileConfiguration config = plugin.getConfigManager().getConfig();
            boolean crushedAny = false;
            boolean hadOccupiedConflict = false;

            int min = config.getInt("fragments.amount.min", 1);
            int max = config.getInt("fragments.amount.max", 3);

            for (int i = 0; i < itemSlots.size(); i++) {
                int inSlot = itemSlots.get(i);
                int outSlot = (i < resultSlots.size()) ? resultSlots.get(i) : -1;

                ItemStack item = inv.getItem(inSlot);
                if (item == null || item.getType().isAir() || ItemUtil.isDecoration(item)) continue;

                io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                if (!nbt.hasType()) {
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-item");
                    continue;
                }

                String tier = nbt.getString("MMOITEMS_TIER");
                if (tier == null || tier.isEmpty()) tier = nbt.getString("CYAYO_FORGE_TIER");
                if (tier == null || tier.isEmpty()) tier = plugin.getForgeManager().getTargetTier(nbt);

                String fragKey = config.getString("fragments.mapping." + tier, config.getString("fragments.default-fragment", "NONE"));

                if (fragKey.equalsIgnoreCase("NONE") || fragKey.isEmpty()) {
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-tier");
                    continue;
                }

                int countPerGear = (min >= max) ? min : min + (int)(Math.random() * ((max - min) + 1));
                int totalAmount = countPerGear * item.getAmount();

                ItemStack fragStack = plugin.getForgeManager().createFragmentItem(fragKey, totalAmount);
                if (fragStack == null || fragStack.getType().isAir()) {
                    plugin.getLogger().warning("[CyayoForgeV2] Gagal membuat fragment untuk key: " + fragKey);
                    continue;
                }

                if (outSlot != -1) {
                    ItemStack currentOut = inv.getItem(outSlot);
                    boolean isOutEmpty = (currentOut == null || currentOut.getType().isAir() || ItemUtil.isDecoration(currentOut));

                    if (isOutEmpty) {
                        // Slot output kosong/placeholder -> sukses taruh hasil fragment
                        inv.setItem(inSlot, gui.getPlaceholder(inSlot));
                        inv.setItem(outSlot, fragStack);
                        crushedAny = true;
                    } else if (currentOut.isSimilar(fragStack)) {
                        // Slot output sudah terisi item yang sama
                        int maxStack = currentOut.getMaxStackSize();
                        int space = maxStack - currentOut.getAmount();
                        if (space >= totalAmount) {
                            // Jumlah muat -> tambahkan jumlahnya
                            inv.setItem(inSlot, gui.getPlaceholder(inSlot));
                            currentOut.setAmount(currentOut.getAmount() + totalAmount);
                            inv.setItem(outSlot, currentOut);
                            crushedAny = true;
                        } else {
                            // Kapasitas slot output penuh -> batalkan crush untuk slot ini (item tetap aman)
                            hadOccupiedConflict = true;
                        }
                    } else {
                        // Item di slot output BERBEDA -> batalkan crush untuk slot ini (item tetap aman)
                        hadOccupiedConflict = true;
                    }
                } else {
                    // Fallback jika tidak ada slot output terkonfigurasi
                    inv.setItem(inSlot, gui.getPlaceholder(inSlot));
                    java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(fragStack);
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), drop);
                    }
                    crushedAny = true;
                }
            }

            if (crushedAny) {
                plugin.getConfigManager().sendRawMessage(player, "success.crushed");
                plugin.getConfigManager().playSound(player, "crush-success");
            } else if (hadOccupiedConflict) {
                plugin.getConfigManager().sendRawMessage(player, "error.crusher-output-occupied");
                plugin.getConfigManager().playSound(player, "error");
            }

            updateCrusherButton(player, inv);
            refreshPlayerView(player, inv);
            return;
        }

        // 2. Slot Input (Items to Crush)
        if (itemSlots.contains(slot)) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isValidCrushItem(cursor)) {
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-tier");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event);
            updateCrusherButton(player, inv);
            refreshPlayerView(player, inv);
            return;
        }

        // 3. Slot Output (Fragment Results)
        if (resultSlots.contains(slot)) {
            handleOutputSlotClick(player, inv, gui, slot, event);
            updateCrusherButton(player, inv);
            refreshPlayerView(player, inv);
            return;
        }
    }

    // =========================================================================
    // REROLL GUI LOGIC
    // =========================================================================
    private void handleRerollClick(InventoryClickEvent event, Player player, Inventory inv, RerollGUI gui) {
        FileConfiguration guiConfig = plugin.getConfigManager().getRerollGui();
        int buttonSlot = guiConfig.getInt("slots.reroll-button", 40);
        int itemSlot = guiConfig.getInt("slots.item-target", 21);
        int fragSlot = guiConfig.getInt("slots.fragment-slot", 23);

        int slot = event.getSlot();

        // 1. Tombol Reroll
        if (slot == buttonSlot) {
            ItemStack targetItem = inv.getItem(itemSlot);
            ItemStack fragmentItem = inv.getItem(fragSlot);

            if (targetItem == null || targetItem.getType().isAir() || ItemUtil.isDecoration(targetItem) ||
                    fragmentItem == null || fragmentItem.getType().isAir() || ItemUtil.isDecoration(fragmentItem)) {
                plugin.getConfigManager().sendRawMessage(player, "error.put-target-first");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            io.lumine.mythic.lib.api.item.NBTItem targetNbt = io.lumine.mythic.lib.api.item.NBTItem.get(targetItem);
            if (!targetNbt.hasType()) {
                plugin.getConfigManager().sendRawMessage(player, "error.invalid-item");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            FileConfiguration config = plugin.getConfigManager().getConfig();
            String tier = targetNbt.getString("CYAYO_FORGE_TIER");
            if (tier == null || tier.isEmpty()) tier = targetNbt.getString("MMOITEMS_TIER");
            if (tier == null || tier.isEmpty()) tier = plugin.getForgeManager().getTargetTier(targetNbt);

            String expectedFragKey = config.getString("fragments.mapping." + tier, config.getString("fragments.default-fragment", "NONE"));

            if (expectedFragKey.equalsIgnoreCase("NONE") || expectedFragKey.isEmpty()) {
                plugin.getConfigManager().sendRawMessage(player, "error.invalid-tier");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            // Validasi kesesuaian Fragment
            io.lumine.mythic.lib.api.item.NBTItem fragNbt = io.lumine.mythic.lib.api.item.NBTItem.get(fragmentItem);
            if (!fragNbt.hasType()) return;

            String actualFragKey = fragNbt.getString("MMOITEMS_ITEM_TYPE") + ":" + fragNbt.getString("MMOITEMS_ITEM_ID");
            if (!actualFragKey.equalsIgnoreCase(expectedFragKey)) {
                plugin.getConfigManager().sendRawMessage(player, "error.fragment-mismatch");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            // Validasi biaya Vault Ekonomi (dengan memperhitungkan diskon rank/permission)
            id.cyayo.forgev2.manager.ForgeManager.RerollCostInfo costInfo = plugin.getForgeManager().calculateRerollCost(player, tier);
            double cost = costInfo.finalCost;

            if (plugin.getEconomy() != null && !plugin.getEconomy().has(player, cost)) {
                plugin.getConfigManager().sendRawMessage(player, "error.not-enough-money", "%cost%", String.valueOf(cost));
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            ItemStack rerolledItem = plugin.getForgeManager().rerollItem(player, targetItem);
            if (rerolledItem == null) {
                plugin.getConfigManager().sendRawMessage(player, "error.craft-error");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            // Potong uang
            if (plugin.getEconomy() != null) {
                plugin.getEconomy().withdrawPlayer(player, cost);
            }

            // Konsumsi 1 fragment
            if (fragmentItem.getAmount() - 1 <= 0) {
                inv.setItem(fragSlot, gui.getPlaceholder(fragSlot));
            } else {
                fragmentItem.setAmount(fragmentItem.getAmount() - 1);
                inv.setItem(fragSlot, fragmentItem);
            }

            // Update item hasil reroll ke slot target
            inv.setItem(itemSlot, rerolledItem);

            String costFormatted = (cost == (long) cost) ? String.valueOf((long) cost) : String.format(java.util.Locale.US, "%.2f", cost);
            plugin.getConfigManager().sendRawMessage(player, "success.rerolled", "%cost%", costFormatted);
            plugin.getConfigManager().playSound(player, "reroll-success");
            updateRerollButton(player, inv);
            refreshPlayerView(player, inv);
            return;
        }

        // 2. Slot Input (Item Target)
        if (slot == itemSlot) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isValidRerollTarget(cursor)) {
                    plugin.getConfigManager().sendRawMessage(player, "error.invalid-tier");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event);
            updateRerollButton(player, inv);
            refreshPlayerView(player, inv);
            return;
        }

        // 3. Slot Input (Fragment Slot)
        if (slot == fragSlot) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!plugin.getForgeManager().isValidFragment(cursor)) {
                    plugin.getConfigManager().sendRawMessage(player, "error.fragment-not-registered");
                    plugin.getConfigManager().playSound(player, "error");
                    return;
                }
                ItemStack currentTarget = inv.getItem(itemSlot);
                if (currentTarget != null && !currentTarget.getType().isAir() && !ItemUtil.isDecoration(currentTarget)) {
                    if (!plugin.getForgeManager().isMatchingFragment(currentTarget, cursor)) {
                        plugin.getConfigManager().sendRawMessage(player, "error.fragment-mismatch");
                        plugin.getConfigManager().playSound(player, "error");
                        return;
                    }
                }
            }
            handleInputSlotClick(player, inv, gui, slot, event);
            updateRerollButton(player, inv);
            refreshPlayerView(player, inv);
            return;
        }
    }

    // =========================================================================
    // UNIVERSAL HELPER: INPUT SLOTS
    // =========================================================================
    private void handleInputSlotClick(Player player, Inventory inv, CustomForgeGUI gui, int slot, InventoryClickEvent event) {
        handleInputSlotClick(player, inv, gui, slot, event, Integer.MAX_VALUE);
    }

    private void handleInputSlotClick(Player player, Inventory inv, CustomForgeGUI gui, int slot, InventoryClickEvent event, int maxCap) {
        ItemStack current = inv.getItem(slot);
        ItemStack cursor = event.getCursor();

        boolean isCurrentPlaceholder = (current == null || current.getType().isAir() || ItemUtil.isDecoration(current));
        boolean isCursorEmpty = (cursor == null || cursor.getType().isAir());

        if (isCurrentPlaceholder) {
            // Jika slot sedang berisi placeholder
            if (isCursorEmpty) {
                // Tangan kosong: Player tidak boleh mengambil item dekorasi/placeholder
                return;
            }

            if (maxCap <= 0) {
                return; // Batas kebutuhan material ini sudah penuh
            }

            // Player memegang item dan ingin menaruhnya di slot ini
            if (event.isRightClick()) {
                // Taruh 1 buah
                ItemStack single = cursor.clone();
                single.setAmount(1);
                inv.setItem(slot, single);

                cursor.setAmount(cursor.getAmount() - 1);
                event.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
            } else {
                // Klik kiri: Taruh sejumlah yang dibutuhkan (dibatasi maxCap)
                int toPlace = Math.min(maxCap, cursor.getAmount());
                ItemStack placed = cursor.clone();
                placed.setAmount(toPlace);
                inv.setItem(slot, placed);

                cursor.setAmount(cursor.getAmount() - toPlace);
                event.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
            }
            player.updateInventory();
        } else {
            // Slot sedang berisi item sungguhan milik player
            if (event.isShiftClick()) {
                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(current.clone());
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
                inv.setItem(slot, gui.getPlaceholder(slot));
                player.updateInventory();
                return;
            }

            if (isCursorEmpty) {
                // Player mengambil kembali itemnya -> Pulihkan placeholder di slot tersebut
                if (event.isRightClick() && current.getAmount() > 1) {
                    // Ambil separuh
                    int half = (current.getAmount() + 1) / 2;
                    ItemStack taken = current.clone();
                    taken.setAmount(half);
                    event.getView().setCursor(taken);

                    current.setAmount(current.getAmount() - half);
                    inv.setItem(slot, current);
                } else {
                    // Ambil semuanya -> Pasang kembali placeholder
                    event.getView().setCursor(current.clone());
                    inv.setItem(slot, gui.getPlaceholder(slot));
                }
                player.updateInventory();
            } else {
                // Player memegang item dan mengklik slot yang sudah ada isinya
                if (current.isSimilar(cursor)) {
                    // Item sama -> Tumpuk (stack) dibatasi maxCap yang masih tersisa
                    int space = Math.min(maxCap, current.getMaxStackSize() - current.getAmount());
                    if (space > 0) {
                        if (event.isRightClick()) {
                            current.setAmount(current.getAmount() + 1);
                            inv.setItem(slot, current);
                            cursor.setAmount(cursor.getAmount() - 1);
                            event.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
                        } else {
                            int toAdd = Math.min(space, cursor.getAmount());
                            current.setAmount(current.getAmount() + toAdd);
                            inv.setItem(slot, current);
                            cursor.setAmount(cursor.getAmount() - toAdd);
                            event.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
                        }
                        player.updateInventory();
                    }
                } else {
                    // Item berbeda: Jika ada batas maxCap, pastikan item baru tidak melebihi maxCap
                    if (maxCap <= 0) return;
                    int toPlace = Math.min(maxCap, cursor.getAmount());
                    ItemStack newPlaced = cursor.clone();
                    newPlaced.setAmount(toPlace);
                    
                    cursor.setAmount(cursor.getAmount() - toPlace);
                    
                    // Kembalikan current ke inventory player
                    java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(current.clone());
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), drop);
                    }
                    
                    inv.setItem(slot, newPlaced);
                    event.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
                    player.updateInventory();
                }
            }
        }
    }

    // =========================================================================
    // UNIVERSAL HELPER: OUTPUT SLOTS
    // =========================================================================
    private void handleOutputSlotClick(Player player, Inventory inv, CustomForgeGUI gui, int slot, InventoryClickEvent event) {
        ItemStack current = inv.getItem(slot);
        ItemStack cursor = event.getCursor();

        // Jika slot masih berisi placeholder kaca, cegah player mengambilnya!
        if (current == null || current.getType().isAir() || ItemUtil.isDecoration(current)) {
            return;
        }

        // 1. Handle Shift-Click pengambilan ke inventory player
        if (event.isShiftClick()) {
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(current.clone());
            if (leftover.isEmpty()) {
                inv.setItem(slot, gui.getPlaceholder(slot));
            } else {
                for (ItemStack drop : leftover.values()) {
                    current.setAmount(drop.getAmount());
                }
                inv.setItem(slot, current);
            }
            player.updateInventory();
            return;
        }

        // 2. Handle cursor kosong (ambil item ke tangan)
        if (cursor == null || cursor.getType().isAir()) {
            if (event.isRightClick() && current.getAmount() > 1) {
                int half = (current.getAmount() + 1) / 2;
                ItemStack taken = current.clone();
                taken.setAmount(half);
                event.getView().setCursor(taken);

                current.setAmount(current.getAmount() - half);
                inv.setItem(slot, current);
            } else {
                event.getView().setCursor(current.clone());
                inv.setItem(slot, gui.getPlaceholder(slot));
            }
            player.updateInventory();
            return;
        }

        // 3. Handle cursor yang memegang item sejenis (merge stack)
        if (cursor.isSimilar(current)) {
            int max = cursor.getMaxStackSize();
            int space = max - cursor.getAmount();
            if (space > 0) {
                if (event.isRightClick()) {
                    cursor.setAmount(cursor.getAmount() + 1);
                    event.getView().setCursor(cursor);
                    current.setAmount(current.getAmount() - 1);
                    if (current.getAmount() <= 0) {
                        inv.setItem(slot, gui.getPlaceholder(slot));
                    } else {
                        inv.setItem(slot, current);
                    }
                } else {
                    int toAdd = Math.min(space, current.getAmount());
                    cursor.setAmount(cursor.getAmount() + toAdd);
                    event.getView().setCursor(cursor);
                    current.setAmount(current.getAmount() - toAdd);
                    if (current.getAmount() <= 0) {
                        inv.setItem(slot, gui.getPlaceholder(slot));
                    } else {
                        inv.setItem(slot, current);
                    }
                }
                player.updateInventory();
            }
        }
    }

    // =========================================================================
    // UNIVERSAL HELPER: SHIFT CLICK INSERTION
    // =========================================================================
    private void handleShiftClickToGui(Player player, Inventory topInv, CustomForgeGUI gui, ItemStack item, int playerSlot) {
        if (item == null || item.getType().isAir()) return;

        if (gui instanceof ForgeGUI forgeGui) {
            FileConfiguration cfg = plugin.getConfigManager().getForgeGui();
            int bpSlot = cfg.getInt("slots.blueprint-slot", 4);
            List<Integer> mat1Slots = cfg.getIntegerList("slots.material-1");
            if (mat1Slots.isEmpty()) mat1Slots = cfg.getIntegerList("slots.material-mob");

            List<Integer> mat2Slots = cfg.getIntegerList("slots.material-2");
            if (mat2Slots.isEmpty()) mat2Slots = cfg.getIntegerList("slots.material-fish");

            List<Integer> mat3Slots = cfg.getIntegerList("slots.material-3");
            if (mat3Slots.isEmpty()) mat3Slots = cfg.getIntegerList("slots.material-mine");

            // 1. Cek Blueprint
            if (plugin.getForgeManager().isBlueprint(item)) {
                if (!canUseBlueprint(player, item)) {
                    return;
                }
                ItemStack cur = topInv.getItem(bpSlot);
                if (cur == null || cur.getType().isAir() || ItemUtil.isDecoration(cur)) {
                    topInv.setItem(bpSlot, item.clone());
                    player.getInventory().setItem(playerSlot, null);
                    updatePreviewSlot(player, topInv, forgeGui);
                    updateForgeButton(player, topInv);
                    refreshPlayerView(player, topInv);
                    return;
                } else if (cur.isSimilar(item)) {
                    int max = cur.getMaxStackSize();
                    int space = max - cur.getAmount();
                    if (space > 0) {
                        int toAdd = Math.min(space, item.getAmount());
                        cur.setAmount(cur.getAmount() + toAdd);
                        topInv.setItem(bpSlot, cur);
                        int rem = item.getAmount() - toAdd;
                        player.getInventory().setItem(playerSlot, rem <= 0 ? null : item);
                        if (rem > 0) item.setAmount(rem);
                        updatePreviewSlot(player, topInv, forgeGui);
                        updateForgeButton(player, topInv);
                        refreshPlayerView(player, topInv);
                        return;
                    }
                }
            }

            ItemStack activeBp = topInv.getItem(bpSlot);
            io.lumine.mythic.lib.api.item.NBTItem activeBpNbt = (activeBp != null && !activeBp.getType().isAir() && !ItemUtil.isDecoration(activeBp)) ? io.lumine.mythic.lib.api.item.NBTItem.get(activeBp) : null;
            List<id.cyayo.forgev2.model.BlueprintRequirement> activeReqs = (activeBpNbt != null) ? plugin.getForgeManager().getRequirements(activeBpNbt) : Collections.emptyList();

            String cat1 = (activeReqs.size() > 0) ? activeReqs.get(0).getCategoryKey() : "MATERIAL_MOB";
            String cat2 = (activeReqs.size() > 1) ? activeReqs.get(1).getCategoryKey() : "MATERIAL_FISH";
            String cat3 = (activeReqs.size() > 2) ? activeReqs.get(2).getCategoryKey() : "MATERIAL_MINE";

            // Cari kolom mana yang menerima item ini
            List<Integer> targetSlots = null;
            int reqAmount = 64;
            String matchedCat = null;

            if (plugin.getForgeManager().isValidMaterial(item, cat1)) {
                targetSlots = mat1Slots;
                reqAmount = (activeReqs.size() > 0) ? activeReqs.get(0).getAmountRequired() : 64;
                matchedCat = cat1;
            } else if (activeReqs.size() > 1 && plugin.getForgeManager().isValidMaterial(item, cat2)) {
                targetSlots = mat2Slots;
                reqAmount = activeReqs.get(1).getAmountRequired();
                matchedCat = cat2;
            } else if (activeReqs.size() > 2 && plugin.getForgeManager().isValidMaterial(item, cat3)) {
                targetSlots = mat3Slots;
                reqAmount = activeReqs.get(2).getAmountRequired();
                matchedCat = cat3;
            }

            if (targetSlots != null) {
                int curTotal = countMaterials(topInv, targetSlots, matchedCat);
                int remainingNeeded = Math.max(0, reqAmount - curTotal);

                if (remainingNeeded <= 0) {
                    if (!plugin.getForgeManager().isValidIngredient(item)) {
                        return;
                    }
                } else {
                    int toTransfer = Math.min(remainingNeeded, item.getAmount());
                    int transferred = 0;

                    // Tahap 1: Coba stack ke slot yang sudah ada item serupa
                    for (int s : targetSlots) {
                        if (transferred >= toTransfer) break;
                        ItemStack cur = topInv.getItem(s);
                        if (cur != null && !cur.getType().isAir() && !ItemUtil.isDecoration(cur) && cur.isSimilar(item)) {
                            int max = cur.getMaxStackSize();
                            int space = max - cur.getAmount();
                            if (space > 0) {
                                int toAdd = Math.min(space, toTransfer - transferred);
                                cur.setAmount(cur.getAmount() + toAdd);
                                topInv.setItem(s, cur);
                                transferred += toAdd;
                            }
                        }
                    }

                    // Tahap 2: Jika masih ada sisa yang perlu ditransfer, masukkan ke slot kosong / placeholder
                    if (transferred < toTransfer) {
                        for (int s : targetSlots) {
                            if (transferred >= toTransfer) break;
                            ItemStack cur = topInv.getItem(s);
                            if (cur == null || cur.getType().isAir() || ItemUtil.isDecoration(cur)) {
                                int toAdd = toTransfer - transferred;
                                ItemStack placed = item.clone();
                                placed.setAmount(toAdd);
                                topInv.setItem(s, placed);
                                transferred += toAdd;
                            }
                        }
                    }

                    // Kurangi amount item dari player inventory
                    int newPlayerAmt = item.getAmount() - transferred;
                    item.setAmount(newPlayerAmt);
                    player.getInventory().setItem(playerSlot, newPlayerAmt <= 0 ? null : item);
                    updatePreviewSlot(player, topInv, forgeGui);
                    updateForgeButton(player, topInv);
                    refreshPlayerView(player, topInv);
                    return;
                }
            }

            // 3. Cek apakah ini Ingredient yang sah
            if (plugin.getForgeManager().isValidIngredient(item)) {
                List<Integer> ingSlots = cfg.getIntegerList("slots.ingredients");
                List<Integer> sortedIng = new ArrayList<>(ingSlots);
                Collections.sort(sortedIng);

                for (int s : sortedIng) {
                    if (s < 0 || s >= topInv.getSize()) continue;
                    ItemStack cur = topInv.getItem(s);
                    if (cur == null || cur.getType().isAir() || ItemUtil.isDecoration(cur)) {
                        ItemStack placed = item.clone();
                        placed.setAmount(1);
                        topInv.setItem(s, placed);

                        int newPlayerAmt = item.getAmount() - 1;
                        item.setAmount(newPlayerAmt);
                        player.getInventory().setItem(playerSlot, newPlayerAmt <= 0 ? null : item);

                        updatePreviewSlot(player, topInv, forgeGui);
                        updateForgeButton(player, topInv);
                        refreshPlayerView(player, topInv);
                        return;
                    }
                }
                return;
            }

            plugin.getConfigManager().sendRawMessage(player, "error.invalid-slot-item");
            plugin.getConfigManager().playSound(player, "error");
            return;
        } else if (gui instanceof CrusherGUI crusherGui) {
            FileConfiguration cfg = plugin.getConfigManager().getCrusherGui();
            List<Integer> itemSlots = cfg.getIntegerList("slots.items-to-crush");

            if (!plugin.getForgeManager().isValidCrushItem(item)) {
                plugin.getConfigManager().sendRawMessage(player, "error.invalid-tier");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            // Tahap 1: Coba stack
            for (int s : itemSlots) {
                if (item.getAmount() <= 0) break;
                ItemStack cur = topInv.getItem(s);
                if (cur != null && !cur.getType().isAir() && !ItemUtil.isDecoration(cur) && cur.isSimilar(item)) {
                    int max = cur.getMaxStackSize();
                    int space = max - cur.getAmount();
                    if (space > 0) {
                        int toAdd = Math.min(space, item.getAmount());
                        cur.setAmount(cur.getAmount() + toAdd);
                        topInv.setItem(s, cur);
                        item.setAmount(item.getAmount() - toAdd);
                    }
                }
            }

            // Tahap 2: Masukkan ke slot kosong
            if (item.getAmount() > 0) {
                for (int s : itemSlots) {
                    if (item.getAmount() <= 0) break;
                    ItemStack cur = topInv.getItem(s);
                    if (cur == null || cur.getType().isAir() || ItemUtil.isDecoration(cur)) {
                        topInv.setItem(s, item.clone());
                        item.setAmount(0);
                    }
                }
            }

            player.getInventory().setItem(playerSlot, item.getAmount() <= 0 ? null : item);
            updateCrusherButton(player, topInv);
            refreshPlayerView(player, topInv);
            return;
        } else if (gui instanceof RerollGUI) {
            FileConfiguration cfg = plugin.getConfigManager().getRerollGui();
            int itemSlot = cfg.getInt("slots.item-target", 21);
            int fragSlot = cfg.getInt("slots.fragment-slot", 23);

            if (plugin.getForgeManager().isValidFragment(item)) {
                ItemStack currentTarget = topInv.getItem(itemSlot);
                if (currentTarget != null && !currentTarget.getType().isAir() && !ItemUtil.isDecoration(currentTarget)) {
                    if (!plugin.getForgeManager().isMatchingFragment(currentTarget, item)) {
                        plugin.getConfigManager().sendRawMessage(player, "error.fragment-mismatch");
                        plugin.getConfigManager().playSound(player, "error");
                        return;
                    }
                }
                ItemStack cur = topInv.getItem(fragSlot);
                if (cur == null || cur.getType().isAir() || ItemUtil.isDecoration(cur)) {
                    topInv.setItem(fragSlot, item.clone());
                    player.getInventory().setItem(playerSlot, null);
                    updateRerollButton(player, topInv);
                    refreshPlayerView(player, topInv);
                    return;
                } else if (cur.isSimilar(item)) {
                    int max = cur.getMaxStackSize();
                    int space = max - cur.getAmount();
                    if (space > 0) {
                        int toAdd = Math.min(space, item.getAmount());
                        cur.setAmount(cur.getAmount() + toAdd);
                        topInv.setItem(fragSlot, cur);
                        int rem = item.getAmount() - toAdd;
                        player.getInventory().setItem(playerSlot, rem <= 0 ? null : item);
                        updateRerollButton(player, topInv);
                        refreshPlayerView(player, topInv);
                        return;
                    }
                }
            } else if (plugin.getForgeManager().isValidRerollTarget(item)) {
                ItemStack cur = topInv.getItem(itemSlot);
                if (cur == null || cur.getType().isAir() || ItemUtil.isDecoration(cur)) {
                    topInv.setItem(itemSlot, item.clone());
                    player.getInventory().setItem(playerSlot, null);
                    updateRerollButton(player, topInv);
                    refreshPlayerView(player, topInv);
                    return;
                }
            } else {
                plugin.getConfigManager().sendRawMessage(player, "error.invalid-tier");
                plugin.getConfigManager().playSound(player, "error");
            }
        }
    }

    // =========================================================================
    // INVENTORY CLOSE (REFUND ITEMS)
    // =========================================================================
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        HumanEntity player = event.getPlayer();

        if (inv.getHolder() instanceof ForgeGUI) {
            FileConfiguration guiConfig = plugin.getConfigManager().getForgeGui();
            refundSlot(inv, player, guiConfig.getInt("slots.blueprint-slot", 4));

            List<Integer> s1 = guiConfig.getIntegerList("slots.material-1");
            if (s1.isEmpty()) s1 = guiConfig.getIntegerList("slots.material-mob");
            for (int slot : s1) refundSlot(inv, player, slot);

            List<Integer> s2 = guiConfig.getIntegerList("slots.material-2");
            if (s2.isEmpty()) s2 = guiConfig.getIntegerList("slots.material-fish");
            for (int slot : s2) refundSlot(inv, player, slot);

            List<Integer> s3 = guiConfig.getIntegerList("slots.material-3");
            if (s3.isEmpty()) s3 = guiConfig.getIntegerList("slots.material-mine");
            for (int slot : s3) refundSlot(inv, player, slot);

            List<Integer> ingSlots = guiConfig.getIntegerList("slots.ingredients");
            for (int slot : ingSlots) refundSlot(inv, player, slot);
        } else if (inv.getHolder() instanceof CrusherGUI) {
            FileConfiguration guiConfig = plugin.getConfigManager().getCrusherGui();
            for (int slot : guiConfig.getIntegerList("slots.items-to-crush")) refundSlot(inv, player, slot);
            for (int slot : guiConfig.getIntegerList("slots.fragment-results")) refundSlot(inv, player, slot);
        } else if (inv.getHolder() instanceof RerollGUI) {
            FileConfiguration guiConfig = plugin.getConfigManager().getRerollGui();
            refundSlot(inv, player, guiConfig.getInt("slots.item-target", 21));
            refundSlot(inv, player, guiConfig.getInt("slots.fragment-slot", 23));
        }
    }

    private void handleIngredientSlotClick(Player player, Inventory inv, ForgeGUI gui, int slot, InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = inv.getItem(slot);
        boolean currentIsItem = (current != null && !current.getType().isAir() && !ItemUtil.isDecoration(current));

        // 1. Shift-click dari dalam slot GUI ke inventaris player
        if (event.isShiftClick()) {
            if (currentIsItem) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(current.clone());
                if (leftover.isEmpty()) {
                    inv.setItem(slot, gui.getPlaceholder(slot));
                } else {
                    for (ItemStack drop : leftover.values()) {
                        current.setAmount(drop.getAmount());
                    }
                    inv.setItem(slot, current);
                }
                player.updateInventory();
            }
            return;
        }

        // 2. Cursor memegang item (mencoba menaruh ingredient)
        if (cursor != null && !cursor.getType().isAir()) {
            if (!plugin.getForgeManager().isValidIngredient(cursor)) {
                plugin.getConfigManager().sendRawMessage(player, "error.invalid-ingredient");
                plugin.getConfigManager().playSound(player, "error");
                return;
            }

            // Batasi tepat 1 item di slot ingredient
            if (!currentIsItem) {
                ItemStack placed = cursor.clone();
                placed.setAmount(1);
                inv.setItem(slot, placed);

                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                    event.getView().setCursor(cursor);
                } else {
                    event.getView().setCursor(null);
                }
            } else {
                if (cursor.getAmount() == 1) {
                    ItemStack toSwap = current.clone();
                    inv.setItem(slot, cursor.clone());
                    event.getView().setCursor(toSwap);
                }
            }
            player.updateInventory();
            return;
        }

        // 3. Cursor kosong (mengambil item dari slot)
        if (currentIsItem) {
            event.getView().setCursor(current.clone());
            inv.setItem(slot, gui.getPlaceholder(slot));
            player.updateInventory();
        }
    }

    private Map<Integer, ItemStack> collectActiveIngredients(Inventory inv, List<Integer> slots) {
        Map<Integer, ItemStack> map = new HashMap<>();
        if (slots == null || slots.isEmpty()) return map;
        for (int s : slots) {
            if (s < 0 || s >= inv.getSize()) continue;
            ItemStack item = inv.getItem(s);
            if (item != null && !item.getType().isAir() && !ItemUtil.isDecoration(item)) {
                if (plugin.getForgeManager().isValidIngredient(item)) {
                    map.put(s, item.clone());
                }
            }
        }
        return map;
    }

    private boolean canUseBlueprint(Player player, ItemStack blueprint) {
        if (plugin.getCyayoProfilesHook() == null) {
            return true;
        }
        id.cyayo.forgev2.hook.CyayoProfilesHook.BlueprintLevelCheck check =
                plugin.getCyayoProfilesHook().checkBlueprintLevel(player, blueprint);
        if (check.allowed) {
            return true;
        }
        String msg;
        if (check.noProfile) {
            msg = plugin.getConfigManager().getRawMessage(
                    "error.no-profile",
                    "&cKamu harus memilih atau membuat profil karakter terlebih dahulu untuk memakai blueprint ini!"
            );
        } else {
            msg = plugin.getConfigManager().getRawMessage(
                    "error.blueprint-level-too-low",
                    "&cLevel kamu masih kurang! Blueprint ini membutuhkan &eLevel %required% &c(Level kamu: &e%level%&c)."
            );
        }
        if (msg != null && !msg.isEmpty()) {
            msg = msg.replace("%required%", String.valueOf(check.requiredLevel))
                    .replace("%level%", String.valueOf(check.playerLevel))
                    .replace("%player%", player.getName());
            if (!ChatColor.stripColor(msg).trim().isEmpty()) {
                player.sendMessage(msg);
            }
        }
        plugin.getConfigManager().playSound(player, "error");
        return false;
    }

    /**
     * Mengembalikan item ke inventory player hanya jika BUKAN item dekorasi/placeholder!
     */
    private void refundSlot(Inventory inv, HumanEntity player, int slot) {
        if (slot < 0 || slot >= inv.getSize()) return;
        ItemStack item = inv.getItem(slot);
        if (item != null && !item.getType().isAir() && !ItemUtil.isDecoration(item)) {
            player.getInventory().addItem(item).values()
                    .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
            inv.setItem(slot, null);
        }
    }
}
