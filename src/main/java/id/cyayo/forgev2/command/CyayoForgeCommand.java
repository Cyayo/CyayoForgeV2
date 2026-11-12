package id.cyayo.forgev2.command;

import id.cyayo.forgev2.CyayoForgeV2;
import id.cyayo.forgev2.gui.CrusherGUI;
import id.cyayo.forgev2.gui.ForgeGUI;
import id.cyayo.forgev2.gui.RerollGUI;
import id.cyayo.forgev2.gui.WinspectGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CyayoForgeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();
        
        // --- RELOAD COMMAND (/cyayoforge reload) ---
        if (cmdName.equals("cyayoforge") || cmdName.equals("forge") || cmdName.equals("cf")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("cyayoforgev2.admin")) {
                    CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.no-permission");
                    return true;
                }
                CyayoForgeV2.getInstance().reloadConfigs();
                CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "success.reloaded");
                return true;
            }

            // --- ADDSTAT COMMAND (/cyayoforge addstat <player> <tracker> <amount> [slot]) ---
            if (args.length > 0 && args[0].equalsIgnoreCase("addstat")) {
                if (!sender.hasPermission("cyayoforgev2.admin")) {
                    CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.no-permission");
                    return true;
                }
                if (args.length < 4) {
                    String usage = CyayoForgeV2.getInstance().getConfigManager().getMessages()
                            .getString("command.addstat-usage", "&cPenggunaan: /%cmd% addstat <player> <tracker_id|trigger|all> <amount> [mainhand|armor|all]")
                            .replace("%cmd%", label);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', usage));
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.player-not-found");
                    return true;
                }

                String trackerId = args[2];
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    String invMsg = CyayoForgeV2.getInstance().getConfigManager().getMessages()
                            .getString("command.addstat-invalid-amount", "&cJumlah penambahan harus berupa angka valid!");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', invMsg));
                    return true;
                }

                String slot = args.length >= 5 ? args[4] : "all";
                int updatedCount = CyayoForgeV2.getInstance().getShinyTrackerManager().addTrackerToPlayer(targetPlayer, trackerId, amount, slot);

                if (updatedCount > 0) {
                    String succMsg = CyayoForgeV2.getInstance().getConfigManager().getMessages()
                            .getString("command.addstat-success", "&aBerhasil menambahkan &f+%amount% &ake tracker &e%tracker% &apada &f%count% &aitem milik &e%player%&a!")
                            .replace("%amount%", String.format(java.util.Locale.US, "%,d", amount))
                            .replace("%tracker%", trackerId)
                            .replace("%count%", String.valueOf(updatedCount))
                            .replace("%player%", targetPlayer.getName());
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', succMsg));
                } else {
                    String notFoundMsg = CyayoForgeV2.getInstance().getConfigManager().getMessages()
                            .getString("command.addstat-not-found", "&cTidak ada item Shiny yang cocok ditemukan pada slot yang dipilih (%slot%) untuk pemain %player%.")
                            .replace("%slot%", slot)
                            .replace("%player%", targetPlayer.getName());
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', notFoundMsg));
                }
                return true;
            }
        }

        // --- TARGETING LOGIC (Player target or Sender) ---
        Player target = null;

        // Jika console mengirim tanpa argumen, tolak
        if (!(sender instanceof Player) && args.length == 0) {
            CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.not-a-player");
            return true;
        }

        if (args.length > 0 && !cmdName.equals("winspect")) { // winspect has different logic for args
            if (!sender.hasPermission("cyayoforgev2.admin")) {
                CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.player-not-found");
                return true;
            }
        } else {
            // Self-target
            target = (Player) sender;
            if (!target.hasPermission("cyayoforgev2.use") && !cmdName.equals("winspect")) {
                CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(target, "error.no-permission");
                return true;
            }
        }

        // --- WINSPECT COMMAND ---
        if (cmdName.equals("winspect")) {
            if (!(sender instanceof Player)) {
                CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(sender, "error.not-a-player");
                return true;
            }
            Player player = (Player) sender;
            org.bukkit.inventory.ItemStack item;
            
            if (args.length == 1) {
                item = id.cyayo.forgev2.listener.ChatListener.getShowcaseItem(args[0]);
                if (item == null) {
                    String expiredMsg = CyayoForgeV2.getInstance().getConfigManager().getConfig()
                            .getString("chat-showcase.messages.expired", "&cShowcase ini telah kadaluarsa atau item tidak ditemukan!");
                    if (expiredMsg != null && !expiredMsg.trim().isEmpty() && !expiredMsg.equalsIgnoreCase("none")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', expiredMsg));
                    }
                    return true;
                }
            } else {
                item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == org.bukkit.Material.AIR) {
                    CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(player, "error.hold-item-inspect");
                    return true;
                }
            }

            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
            if (!nbt.hasTag("CYAYO_FORGE_CRAFTER")) {
                CyayoForgeV2.getInstance().getConfigManager().sendRawMessage(player, "error.not-forged-item");
                return true;
            }
            new WinspectGUI(player, item).open();
            return true;
        }

        // --- OPEN GUIs ---
        if (cmdName.equals("cyayoforge") || cmdName.equals("forge") || cmdName.equals("cf")) {
            new ForgeGUI(target).open();
            return true;
        }
        if (cmdName.equals("crusher")) {
            new CrusherGUI(target).open();
            return true;
        }
        if (cmdName.equals("reroll")) {
            new RerollGUI(target).open();
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("cyayoforge") || cmd.equals("forge") || cmd.equals("cf")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                if (sender.hasPermission("cyayoforgev2.admin")) {
                    list.add("reload");
                    list.add("addstat");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        list.add(p.getName());
                    }
                }
                return filterStartingWith(args[0], list);
            } else if (args.length > 1 && args[0].equalsIgnoreCase("addstat") && sender.hasPermission("cyayoforgev2.admin")) {
                if (args.length == 2) {
                    List<String> list = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        list.add(p.getName());
                    }
                    return filterStartingWith(args[1], list);
                } else if (args.length == 3) {
                    List<String> list = new ArrayList<>();
                    list.add("all");
                    if (CyayoForgeV2.getInstance().getShinyTrackerManager() != null) {
                        list.addAll(CyayoForgeV2.getInstance().getShinyTrackerManager().getTrackers().keySet());
                    }
                    list.add("MOBS_KILLED");
                    list.add("PLAYERS_KILLED");
                    list.add("BOSSES_KILLED");
                    list.add("DAMAGE_DEALT");
                    list.add("DAMAGE_TAKEN");
                    list.add("DEATHS");
                    return filterStartingWith(args[2], list);
                } else if (args.length == 4) {
                    return filterStartingWith(args[3], java.util.Arrays.asList("1", "5", "10", "50", "100"));
                } else if (args.length == 5) {
                    return filterStartingWith(args[4], java.util.Arrays.asList("all", "mainhand", "armor"));
                }
            }
        } else if (cmd.equals("crusher") || cmd.equals("reroll")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                if (sender.hasPermission("cyayoforgev2.admin")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        list.add(p.getName());
                    }
                }
                return filterStartingWith(args[0], list);
            }
        } else if (cmd.equals("winspect")) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>(id.cyayo.forgev2.listener.ChatListener.getActiveShowcaseIds());
                return filterStartingWith(args[0], list);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterStartingWith(String prefix, List<String> options) {
        if (prefix == null || prefix.isEmpty()) return options;
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
