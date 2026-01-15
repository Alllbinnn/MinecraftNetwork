package at.albin.VELGUARD.listener;

import at.albin.VELGUARD.VELGUARD;
import at.albin.VELGUARD.helper.DatabaseManagerPaper;
import at.albin.VELGUARD.helper.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuteChatListener implements Listener {

    private final VELGUARD plugin;

    private final Map<UUID, CacheEntry> muteCache = new ConcurrentHashMap<>();
    private static final long CACHE_MS = 5000;
    private static final DateTimeFormatter UNTIL_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public MuteChatListener(VELGUARD plugin) {
        this.plugin = plugin;
    }

    public void invalidate(UUID uuid) {
        muteCache.remove(uuid);
    }


    @EventHandler
    public void onChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        MuteInfo mute = getMuteInfo(uuid);
        if (mute == null) return;

        event.setCancelled(true);

        String until = formatUntil(mute.expiresAt);

        event.getPlayer().sendMessage(Messages.muteLine1());
        event.getPlayer().sendMessage(Messages.muteLine2(mute.reason));
        event.getPlayer().sendMessage(Messages.muteLine3(until));
        event.getPlayer().sendMessage(Messages.muteLine4(mute.id()));
    }

    private String formatUntil(Instant expiresAt) {
        if (expiresAt == null) return "PERMANENT";
        LocalDateTime ldt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
        return UNTIL_FMT.format(ldt);
    }

    private MuteInfo getMuteInfo(UUID uuid) {
        long now = System.currentTimeMillis();

        CacheEntry cached = muteCache.get(uuid);
        if (cached != null && (now - cached.cachedAtMs) < CACHE_MS) {
            return cached.mute;
        }

        MuteInfo result = queryMute(uuid);
        muteCache.put(uuid, new CacheEntry(now, result));
        return result;
    }

    private MuteInfo queryMute(UUID uuid) {
        String sql =
                "SELECT id, Reason, expires_at " +
                        "FROM punishment " +
                        "WHERE UUID = ? AND TYPE = 'MUTE' AND (expires_at IS NULL OR expires_at > NOW()) " +
                        "ORDER BY created_at DESC LIMIT 1";

        try (Connection con = DatabaseManagerPaper.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                long id = rs.getLong("id");
                String reason = rs.getString("Reason");
                Timestamp ts = rs.getTimestamp("expires_at");
                Instant expires = (ts == null) ? null : ts.toInstant();
                return new MuteInfo(id, reason, expires);

            }

        } catch (Exception e) {
            plugin.getLogger().warning("DB Fehler beim Mute-Check: " + e.getMessage());
            return null;
        }
    }

    private record MuteInfo(long id, String reason, Instant expiresAt) {}
    private record CacheEntry(long cachedAtMs, MuteInfo mute) {}
}
