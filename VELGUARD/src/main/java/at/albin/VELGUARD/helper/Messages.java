package at.albin.VELGUARD.helper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Messages {

    private Messages() {}

    public static Component prefix() {
        return Component.text("VEL-BKM", NamedTextColor.DARK_RED)
                .append(Component.space())
                .append(Component.text("»", NamedTextColor.GRAY))
                .append(Component.space());
    }

    public static Component noPermission(String perm) {
        return prefix()
                .append(Component.text("Keine Permission: ", NamedTextColor.RED))
                .append(Component.text(perm, NamedTextColor.GRAY));
    }

    public static Component usage(String sub) {
        return prefix()
                .append(Component.text("Usage: ", NamedTextColor.RED))
                .append(Component.text("/" + sub + " <player> [reason...] [dauer|datum]", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(prefix())
                .append(Component.text("Dauer: ", NamedTextColor.GRAY))
                .append(Component.text("1d 3d 1w 4w 1m 6m 1y", NamedTextColor.WHITE))
                .append(Component.text(" oder Datum: ", NamedTextColor.GRAY))
                .append(Component.text("25-12-2025", NamedTextColor.WHITE));
    }

    public static Component resolvingUuid(String playerName) {
        return prefix()
                .append(Component.text("UUID wird geladen für ", NamedTextColor.GRAY))
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text("...", NamedTextColor.GRAY));
    }

    public static Component uuidResolveFailed(String playerName) {
        return prefix()
                .append(Component.text("Konnte UUID nicht auflösen: ", NamedTextColor.RED))
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" (Name falsch oder Mojang API down).", NamedTextColor.GRAY));
    }

    public static Component muteLine1() {
        return prefix()
                .append(Component.text("Du bist ", NamedTextColor.RED))
                .append(Component.text("gemutet", NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.RED));
    }

    public static Component kickRequiresOnline() {
        return prefix().append(Component.text("Der Spieler muss online sein, um gekickt zu werden.", NamedTextColor.RED));
    }

    public static Component cannotPunishHigher() {
        return prefix()
                .append(Component.text("Du kannst nur Spieler unter deinem Rang bestrafen/bearbeiten.", NamedTextColor.RED));
    }

    public static Component muteLine2(String reason) {
        return prefix()
                .append(Component.text("Grund: ", NamedTextColor.RED))
                .append(Component.text(reason, NamedTextColor.WHITE));
    }

    public static Component muteLine3(String until) {
        return prefix()
                .append(Component.text("Bis: ", NamedTextColor.RED))
                .append(Component.text(until, NamedTextColor.WHITE));
    }

    public static Component muteLine4(long id) {
        return prefix()
                .append(Component.text("Mute-ID: ", NamedTextColor.RED))
                .append(Component.text(String.valueOf(id), NamedTextColor.WHITE));
    }

}
