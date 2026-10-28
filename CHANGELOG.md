# Changelog

All notable changes to mseries-core are listed here, newest first.
This project follows [Semantic Versioning](https://semver.org/).

## Unreleased

### Added
- `ConfigFile`: loads a YAML file from the plugin folder, saving the default from the jar on first start.
  - Adds settings that are missing from the owner's file, with their comments, and keeps the owner's values.
  - Raises `config-version` to the jar's version and logs what was added.
  - A file with broken YAML fails with a `ConfigException` that names the file; on reload the old values stay.
- `Messages`: loads `messages.yml` and sends text with one `prefix`.
  - MiniMessage on every platform, plus `&` color codes and `&#rrggbb` hex colors.
  - Chat, action bar, title and broadcast; an empty message is not sent.
  - `Arg.of` placeholders escape player-supplied text so it cannot inject formatting.
- `Text`: MiniMessage parsing and legacy or plain output for item names and inventory titles.
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
