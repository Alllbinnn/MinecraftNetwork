package at.albin.VELGUARD.rank;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class RankManager {

    private final JavaPlugin plugin;
    private final Map<String, Integer> power = new HashMap<>();
    private List<String> ordered = new ArrayList<>();

    public RankManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<String> ranks = cfg.getStringList("ranks");
        if (ranks == null || ranks.isEmpty()) {
            ranks = List.of("owner", "admin", "mod", "supporter", "default");
        }

        ordered = new ArrayList<>();
        power.clear();

        int base = ranks.size();
        for (int i = 0; i < ranks.size(); i++) {
            String r = ranks.get(i).toLowerCase(Locale.ROOT).trim();
            ordered.add(r);
            power.put(r, base - i);
        }

        // fallback
        power.putIfAbsent("default", 0);
        if (!ordered.contains("default")) ordered.add("default");
    }

    public List<String> getRanks() {
        return Collections.unmodifiableList(ordered);
    }

    public int getPower(String rank) {
        if (rank == null) return power.getOrDefault("default", 0);
        return power.getOrDefault(rank.toLowerCase(Locale.ROOT), power.getOrDefault("default", 0));
    }

    public boolean canPunish(String actorRank, String targetRank) {
        return getPower(actorRank) > getPower(targetRank);
    }
}
