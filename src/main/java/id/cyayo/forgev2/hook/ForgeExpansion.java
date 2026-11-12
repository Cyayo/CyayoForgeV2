package id.cyayo.forgev2.hook;

import id.cyayo.forgev2.CyayoForgeV2;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForgeExpansion extends PlaceholderExpansion {

    private final CyayoForgeV2 plugin;

    public ForgeExpansion(CyayoForgeV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "cyayoforge";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isEmpty()) return "";

        // %cyayoforge_player_score%
        if (params.equalsIgnoreCase("player_score")) {
            if (player == null || player.getName() == null) return "0";
            return String.valueOf(plugin.getLeaderboardManager().getPlayerScore(player.getName()));
        }

        // Placeholders untuk item yang sedang dipegang di main hand player
        // %cyayoforge_hand_is_forged%
        // %cyayoforge_hand_crafter%
        // %cyayoforge_hand_quality%
        // %cyayoforge_hand_multiplier%
        // %cyayoforge_hand_dominant%
        // %cyayoforge_hand_rerolls%
        if (params.startsWith("hand_")) {
            if (player == null || !player.isOnline() || player.getPlayer() == null) return "";
            org.bukkit.entity.Player onlinePlayer = player.getPlayer();
            ItemStack hand = onlinePlayer.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == org.bukkit.Material.AIR) return "";

            if (params.equalsIgnoreCase("hand_is_forged")) {
                return String.valueOf(id.cyayo.forgev2.api.CyayoForgeAPI.isForgedItem(hand));
            }
            if (params.equalsIgnoreCase("hand_crafter")) {
                String crafter = id.cyayo.forgev2.api.CyayoForgeAPI.getCrafter(hand);
                return crafter.isEmpty() ? "-" : crafter;
            }
            if (params.equalsIgnoreCase("hand_quality")) {
                double q = id.cyayo.forgev2.api.CyayoForgeAPI.getQuality(hand);
                return q > 0 ? String.format(java.util.Locale.US, "%.1f", q) : "-";
            }
            if (params.equalsIgnoreCase("hand_multiplier")) {
                double m = id.cyayo.forgev2.api.CyayoForgeAPI.getFinalMultiplier(hand);
                return String.format(java.util.Locale.US, "%.3f", m);
            }
            if (params.equalsIgnoreCase("hand_dominant")) {
                String d = id.cyayo.forgev2.api.CyayoForgeAPI.getDominantMaterial(hand);
                return d.isEmpty() ? "-" : d;
            }
            if (params.equalsIgnoreCase("hand_rerolls")) {
                return String.valueOf(id.cyayo.forgev2.api.CyayoForgeAPI.getRerollCount(hand));
            }
        }

        // %cyayoforge_top_1_name% atau %cyayoforge_top_3_score%
        if (params.startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length >= 3) {
                try {
                    int rank = Integer.parseInt(parts[1]);
                    String type = parts[2]; // "name" atau "score"

                    Map<String, Integer> topPlayers = plugin.getLeaderboardManager().getTopPlayers(10);
                    List<Map.Entry<String, Integer>> list = new ArrayList<>(topPlayers.entrySet());

                    if (rank > 0 && rank <= list.size()) {
                        Map.Entry<String, Integer> target = list.get(rank - 1);
                        if (type.equalsIgnoreCase("name")) return target.getKey();
                        if (type.equalsIgnoreCase("score")) return String.valueOf(target.getValue());
                    } else {
                        // Jika belum ada pemain di rank tersebut
                        if (type.equalsIgnoreCase("name")) return "Belum Ada";
                        if (type.equalsIgnoreCase("score")) return "0";
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }
}
