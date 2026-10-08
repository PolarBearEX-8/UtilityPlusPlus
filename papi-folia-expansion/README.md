# Folia-compatible PAPI Bungee expansion

This standalone PlaceholderAPI expansion provides the usual `%bungee_total%` and `%bungee_<server>%` placeholders through Velocity's BungeeCord-compatible plugin-message channel. It uses Folia's global and entity schedulers rather than Bukkit's unsupported repeating scheduler.

## Build

From the repository root, run:

```powershell
.\gradlew.bat -p papi-folia-expansion clean jar
```

The expansion jar is written to `papi-folia-expansion/build/libs/Bungee-Folia-1.0.0.jar`.

## Install

On every Folia backend where placeholders are parsed:

1. Remove the official `Bungee` expansion jar from `plugins/PlaceholderAPI/expansions` so it does not register the same `bungee` identifier.
2. Copy `Bungee-Folia-1.0.0.jar` into `plugins/PlaceholderAPI/expansions`.
3. Restart the backend, or run `/papi reload`.
4. From an online player, test `/papi parse me %bungee_total%` and `/papi parse me %bungee_pvp%`.

Velocity must have `bungee-plugin-message-channel = true`; expansion queries refresh every 5 seconds by default.
