package id.cyayo.forgev2.manager;

import id.cyayo.forgev2.CyayoForgeV2;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LeaderboardManager {
    
    private final CyayoForgeV2 plugin;
    private File file;
    private FileConfiguration data;
    
    // Cache di memori agar cepat
    private Map<String, Integer> scores = new HashMap<>();

    public LeaderboardManager(CyayoForgeV2 plugin) {
        this.plugin = plugin;
        setupDataFile();
        loadScores();
    }

    private void setupDataFile() {
        file = new File(plugin.getDataFolder(), "data/leaderboard.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal membuat file leaderboard.yml!");
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    private void loadScores() {
        scores.clear();
        if (data.getConfigurationSection("scores") != null) {
            for (String playerName : data.getConfigurationSection("scores").getKeys(false)) {
                scores.put(playerName, data.getInt("scores." + playerName));
            }
        }
    }

    /**
     * Menambah skor pemain jika mendapatkan senjata sempurna.
     */
    public void addScore(String playerName) {
        int currentScore = scores.getOrDefault(playerName, 0);
        scores.put(playerName, currentScore + 1);
        data.set("scores." + playerName, currentScore + 1);
        saveData();
    }

    /**
     * Mendapatkan skor seorang pemain
     */
    public int getPlayerScore(String playerName) {
        return scores.getOrDefault(playerName, 0);
    }

    /**
     * Mendapatkan daftar pemain yang diurutkan dari skor tertinggi (Top 10)
     */
    public Map<String, Integer> getTopPlayers(int limit) {
        return scores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private void saveData() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan leaderboard!");
        }
    }
}
