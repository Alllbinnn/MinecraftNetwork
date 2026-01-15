# VELL Network Guard Suite (VELLSCAFFOLDING + VELGUARD + VELUTILITYGUARD)

Dieses Repository enthält eine kleine Plugin-Suite für ein Minecraft-Netzwerk (Velocity + Paper/Spigot), bestehend aus:

- **VELLSCAFFOLDING (Velocity)**  
  Zentrale Verarbeitung/Enforcement von Strafen (Ban/Mute/Kick/Unban/Unmute) + Session-Logging + User-Tracking in MariaDB.
- **VELGUARD (Paper/Spigot)**  
  Ingame-Commands für Team/Staff (`/ban`, `/mute`, …), Rang-Check via LuckPerms + Mute-Chatblock mit Cache. Sendet Strafen per PluginMessage an Velocity.
- **VELUTILITYGUARD (Paper/Spigot)**  
  Blockiert/entfernt gefährliche bzw. unnötige Commands aus Tab-Completion + verhindert „Unknown Command“-Spam.

---

## Wie es zusammen arbeitet

**Paper (VELGUARD)** → sendet eine PluginMessage auf  
`vellscaffolding:punishment`

**Velocity (VELLSCAFFOLDING)** → empfängt die Message, schreibt in DB, enforced:
- BAN: Spieler wird beim Login geblockt (Ban-Screen) / ggf. sofort gekickt
- MUTE: Mute wird in DB gespeichert; Paper blockiert Chat anhand DB-Check
- UNBAN/UNMUTE: Strafe wird beendet (expires_at = NOW())
- KICK: kickt sofort

**Paper (VELUTILITYGUARD)** → filtert Commands in Autocomplete + blockiert bestimmte Commands direkt.

---

## Features

### VELLSCAFFOLDING (Velocity)
- MariaDB-Anbindung
- **Ban-Enforcement beim Login**
- **Punishment Message Listener** (asynchroner DB-Worker)
- **User Tracking**: Eintrag/Update in `Users`
- **Session Logging**: `UserSessions` Join/Leave inkl. Crash-Handling

### VELGUARD (Paper)
- Commands: `ban/unban/mute/unmute/kick`
- Dauer:
  - **Relativ**: `1d`, `2w`, `1m`, `1y`
  - **Absolut**: Datum `dd-MM-yyyy` (endet 23:59:59 Serverzeit)
  - **Permanent**: Standard
- UUID-Auflösung:
  - schnell (online / cached)
  - fallback Mojang API (async)
- LuckPerms-Rangvergleich: Staff kann nur **untere** Ränge bestrafen
- Mute-Chatblock (DB-Check) mit kurzem Cache (5s)

### VELUTILITYGUARD (Paper)
- Entfernt blockierte Commands aus Tab-Completion
- Blockiert Ausführung blockierter Commands
- Einheitliche „Unknown command“-Message

---

## Voraussetzungen

- **Velocity** (für VELLSCAFFOLDING)
- **Paper/Spigot** (für VELGUARD & VELUTILITYGUARD)
- **MariaDB** (oder kompatibel)
- **LuckPerms** (für Rang-Check in VELGUARD)
- Netzwerk muss PluginMessage-Channel zulassen (Velocity ↔ Paper)

---

## Installation

### 1) Datenbank
Erstelle eine Datenbank (Beispiel):
- `users_db`

Benötigte Tabellen (aus dem Code ersichtlich):
- `punishment` (UUID, TYPE, Reason, ACTOR, created_at, expires_at, id PK auto-inc)
- `Users` (UUID, IP_ADRESS, ONLINE, SERVER, RANK, PLAYTIME, FIRST_JOIN, …)
- `UserSessions` (UUID, IP_ADRESS, JOIN_DATETIME, LEAVE_DATETIME)

> Hinweis: Die exakten Spalten/Typen hängen von deinem bestehenden Schema ab.  
> Wichtig ist: `punishment.expires_at` muss `NULL` (permanent) oder ein Timestamp sein.

### 2) VELLSCAFFOLDING auf Velocity
- `VELLSCAFFOLDING.jar` nach `velocity/plugins/`
- Server starten

### 3) VELGUARD & VELUTILITYGUARD auf Paper
- `VELGUARD.jar` und `VELUTILITYGUARD.jar` nach `paper/plugins/`
- Server starten

---

## Konfiguration

### Datenbank (WICHTIG)
Aktuell sind DB-Daten im Code hinterlegt (hardcoded). Für GitHub/Production empfohlen:
- Passwort/Host/DB/User in Config (`config.yml`) oder ENV auslagern
- Secrets **nie** committen

**Velocity:** `DatabaseManager`  
**Paper:** `DatabaseManagerPaper`

---

## Commands (VELGUARD)

> Permissions folgen dem Muster: `vellscaffolding.punishment.<command>`

- `/ban <player> [reason...] [dauer|datum]`
- `/mute <player> [reason...] [dauer|datum]`
- `/unban <player> [reason...]`
- `/unmute <player> [reason...]`
- `/kick <player> [reason...]` *(Spieler muss online sein)*

Beispiele:
- `/ban Notch Griefing 7d`
- `/mute Steve Spam 1w`
- `/ban Alex Alt account 25-12-2025`
- `/unmute Steve Entmutet`

---

## Permissions

### VELGUARD (Punishment)
- `vellscaffolding.punishment.ban`
- `vellscaffolding.punishment.unban`
- `vellscaffolding.punishment.mute`
- `vellscaffolding.punishment.unmute`
- `vellscaffolding.punishment.kick`

### VELGUARD (Rang-System / Staffelung)
- `vellscaffolding.punishment.sgd.<rank>`
  - Beispiele: `vellscaffolding.punishment.sgd.owner`, `...admin`, `...mod`, …

### VELUTILITYGUARD
- Bypass für Command-Block & Autocomplete-Filter:
  - `vellscaffolding.utility.bypasscmd`

---

## ranks.yml (VELGUARD)

`plugins/VELGUARD/ranks.yml`

Beispiel:
```yml
ranks:
  - owner
  - admin
  - mod
  - supporter
  - default
