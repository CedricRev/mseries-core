# Changelog

All notable changes to mseries-core are listed here, newest first.
This project follows [Semantic Versioning](https://semver.org/).

## 1.0.0

First release: platform detection and the command layer.

### Added
- `Platform` and `ServerCore`: detect Spigot, Paper, Purpur or Folia once at startup, without loading Paper classes on Spigot.
- `CommandService`: one call creates the right Incendo Cloud v2 manager for the server.
  - Paper, Purpur and Folia with Paper's command API: `PaperCommandManager` with native Brigadier.
  - Older Paper: `LegacyPaperCommandManager` with Brigadier where Cloud supports it.
  - Spigot: `LegacyPaperCommandManager` with Cloud's standard Bukkit registration.
- Annotation-based commands through `cloud-annotations`, with `CommandActor` as the sender type on every platform.
