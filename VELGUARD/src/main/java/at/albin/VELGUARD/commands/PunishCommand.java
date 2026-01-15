package at.albin.VELGUARD.commands;

import at.albin.VELGUARD.VELGUARD;
import at.albin.VELGUARD.helper.Messages;
import at.albin.VELGUARD.listener.MuteChatListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.albin.VELGUARD.rank.LuckPermsRankService;


public class PunishCommand implements CommandExecutor {

    private final VELGUARD plugin;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([dwmy])$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final MuteChatListener muteChatListener;
    private final LuckPermsRankService rankService;


    private enum ExpiryMode { PERMA, RELATIVE, ABSOLUTE }

    private static class ExpirySpec {
        final ExpiryMode mode;
        final long amount;     // RELATIVE
        final char unit;       // RELATIVE
        final long absoluteMs; // ABSOLUTE

        ExpirySpec(ExpiryMode mode, long amount, char unit, long absoluteMs) {
            this.mode = mode;
            this.amount = amount;
            this.unit = unit;
            this.absoluteMs = absoluteMs;
        }

        static ExpirySpec perma() { return new ExpirySpec(ExpiryMode.PERMA, 0, '\0', 0); }
        static ExpirySpec rel(long amount, char unit) { return new ExpirySpec(ExpiryMode.RELATIVE, amount, unit, 0); }
        static ExpirySpec abs(long ms) { return new ExpirySpec(ExpiryMode.ABSOLUTE, 0, '\0', ms); }
    }

    private static class ParsedReasonAndExpiry {
        final String reason;
        final ExpirySpec expiry;

        ParsedReasonAndExpiry(String reason, ExpirySpec expiry) {
            this.reason = reason;
            this.expiry = expiry;
        }
    }

    public PunishCommand(VELGUARD plugin, MuteChatListener muteChatListener, LuckPermsRankService rankService) {
        this.plugin = plugin;
        this.muteChatListener = muteChatListener;
        this.rankService = rankService;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player actorPlayer)) {
            sender.sendMessage("Nur Spieler können das nutzen.");
            return true;
        }

        String sub = cmd.getName().toLowerCase();   // ban/unban/mute/unmute/kick
        String action = sub.toUpperCase();
        String perm = "vellscaffolding.punishment." + sub;

        if (!actorPlayer.hasPermission(perm)) {
            actorPlayer.sendMessage(Messages.noPermission(perm));
            return true;
        }

        if (args.length < 1) {
            actorPlayer.sendMessage(Messages.usage(sub));
            return true;
        }

        String targetName = args[0];


        // Default
        String reason = "Kein Grund";
        ExpirySpec expiry = ExpirySpec.perma();

        if (sub.equals("kick")) {
            Player targetOnline = Bukkit.getPlayerExact(targetName);
            if (targetOnline == null) {
                actorPlayer.sendMessage(Messages.kickRequiresOnline());
                return true;
            }

            UUID targetUuid = targetOnline.getUniqueId();
            checkAndSend(actorPlayer, sub, action, targetUuid, targetName, reason, ExpirySpec.perma());
            return true;
        }


        if (sub.equals("ban") || sub.equals("mute")) {
            ParsedReasonAndExpiry parsed = parseReasonAndExpiry(args);
            reason = parsed.reason;
            expiry = parsed.expiry;
        } else {
            // unban/unmute/kick -> alles nach player ist reason (optional)
            if (args.length >= 2) {
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (reason.isBlank()) reason = "Kein Grund";
            }
        }

        // 1) Online/Cached UUID
        Optional<UUID> fast = resolveUuidFast(targetName);
        if (fast.isPresent()) {
            checkAndSend(actorPlayer, sub, action, fast.get(), targetName, reason, expiry);
            return true;
        }

        actorPlayer.sendMessage(Messages.resolvingUuid(targetName));

        final String finalReason = reason;
        final ExpirySpec finalExpiry = expiry;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<UUID> mojang = resolveUuidViaMojang(targetName);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!actorPlayer.isOnline()) return;

                if (mojang.isEmpty()) {
                    actorPlayer.sendMessage(Messages.uuidResolveFailed(targetName));
                    return;
                }

                if (sub.equals("mute") || sub.equals("unmute")) {
                    muteChatListener.invalidate(mojang.get()); // <-- HIER
                }

                checkAndSend(actorPlayer, sub, action, mojang.get(), targetName, finalReason, finalExpiry);
                // actorPlayer.sendMessage(Messages.sentToProxy(action, targetName)); // willst du ja entfernen
            });

        });

        return true;
    }

    private ParsedReasonAndExpiry parseReasonAndExpiry(String[] args) {
        if (args.length == 1) return new ParsedReasonAndExpiry("Kein Grund", ExpirySpec.perma());

        String last = args[args.length - 1];
        Optional<ExpirySpec> specOpt = parseExpiryTokenToSpec(last);

        if (specOpt.isPresent()) {
            String reason = (args.length >= 3)
                    ? String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1))
                    : "Kein Grund";
            if (reason.isBlank()) reason = "Kein Grund";
            return new ParsedReasonAndExpiry(reason, specOpt.get());
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (reason.isBlank()) reason = "Kein Grund";
        return new ParsedReasonAndExpiry(reason, ExpirySpec.perma());
    }

    private Optional<ExpirySpec> parseExpiryTokenToSpec(String token) {
        // ABSOLUTE: dd-MM-yyyy
        try {
            LocalDate date = LocalDate.parse(token, DATE_FMT);
            ZonedDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault());
            return Optional.of(ExpirySpec.abs(endOfDay.toInstant().toEpochMilli()));
        } catch (DateTimeParseException ignored) {}

        // RELATIVE: 1d/1w/1m/1y
        Matcher m = DURATION_PATTERN.matcher(token);
        if (!m.matches()) return Optional.empty();

        long amount = Long.parseLong(m.group(1));
        char unit = Character.toLowerCase(m.group(2).charAt(0));
        return Optional.of(ExpirySpec.rel(amount, unit));
    }

    private Optional<UUID> resolveUuidFast(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return Optional.of(online.getUniqueId());

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) return Optional.of(cached.getUniqueId());

        return Optional.empty();
    }

    private Optional<UUID> resolveUuidViaMojang(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(4000);
            con.setReadTimeout(4000);

            if (con.getResponseCode() != 200) return Optional.empty();

            String json;
            try (InputStream is = con.getInputStream()) {
                json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String id = extractJsonField(json, "id");
            if (id == null || id.length() != 32) return Optional.empty();

            return Optional.of(uuidFromMojangId(id));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String extractJsonField(String json, String field) {
        String needle = "\"" + field + "\":\"";
        int start = json.indexOf(needle);
        if (start == -1) return null;
        start += needle.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private UUID uuidFromMojangId(String s32) {
        String withHyphens =
                s32.substring(0, 8) + "-" +
                        s32.substring(8, 12) + "-" +
                        s32.substring(12, 16) + "-" +
                        s32.substring(16, 20) + "-" +
                        s32.substring(20);
        return UUID.fromString(withHyphens);
    }

    private void send(Player via, String action, UUID actor, UUID target, String targetName, String reason, ExpirySpec expiry) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeUTF(action);
            out.writeUTF(actor.toString());
            out.writeUTF(target.toString());
            out.writeUTF(targetName);
            out.writeUTF(reason);

            // mode: 0=PERMA, 1=RELATIVE, 2=ABSOLUTE
            out.writeByte(switch (expiry.mode) {
                case PERMA -> 0;
                case RELATIVE -> 1;
                case ABSOLUTE -> 2;
            });

            // immer senden (Velocity liest immer diese Felder)
            out.writeLong(expiry.amount);
            out.writeChar(expiry.unit);
            out.writeLong(expiry.absoluteMs);

            via.sendPluginMessage(plugin, VELGUARD.CHANNEL, baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAndSend(Player actorPlayer, String sub, String action,
                              UUID targetUuid, String targetName, String reason, ExpirySpec expiry) {

        UUID actorUuid = actorPlayer.getUniqueId();

        String actorRank = resolveActorRank(actorPlayer);

        rankService.canPunish(actorUuid, targetUuid, () -> actorRank).thenAccept(res -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!actorPlayer.isOnline()) return;

                if (!res.allowed()) {
                    actorPlayer.sendMessage(Messages.cannotPunishHigher());
                    return;
                }

                if (sub.equals("mute") || sub.equals("unmute")) {
                    muteChatListener.invalidate(targetUuid);
                }

                send(actorPlayer, action, actorUuid, targetUuid, targetName, reason, expiry);
            });
        });

    }

    private String resolveActorRank(Player actor) {
        for (String rank : plugin.getRankManager().getRanks()) { // siehe Hinweis unten
            if (actor.hasPermission("vellscaffolding.punishment.sgd." + rank)) {
                return rank;
            }
        }
        return "default";
    }


}
