# Changelog

All notable changes to mseries-core are listed here, newest first.
This project follows [Semantic Versioning](https://semver.org/).

## Unreleased

### Added
- `Scheduler`: one API for running tasks on every platform.
  - Folia: global region, region, entity and async schedulers.
  - Spigot and Paper: the Bukkit scheduler.
  - Entity tasks stop once the entity is removed or the player logs out, matching Folia on every platform.
- `Task`: a handle to cancel a scheduled task or check whether it is cancelled.

## 1.0.0

First release: platform detection and the command layer.

### Added
- `Platform` and `ServerCore`: detect Spigot, Paper, Purpur or Folia once at startup, without loading Paper classes on Spigot.
- `CommandService`: one call creates the right Incendo Cloud v2 manager for the server.
  - Paper, Purpur and Folia with Paper's command API: `PaperCommandManager` with native Brigadier.
  - Older Paper: `LegacyPaperCommandManager` with Brigadier where Cloud supports it.
  - Spigot: `LegacyPaperCommandManager` with Cloud's standard Bukkit registration.
- Annotation-based commands through `cloud-annotations`, with `CommandActor` as the sender type on every platform.
