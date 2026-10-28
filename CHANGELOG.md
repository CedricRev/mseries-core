# Changelog

All notable changes to mseries-core are listed here, newest first.
This project follows [Semantic Versioning](https://semver.org/).

## 1.1.0

### Added
- `ConfigFile`: loads a YAML file from the plugin folder, saving the default from the jar on first start.
  - Adds settings that are missing from the owner's file, with their comments, and keeps the owner's values.
  - Raises `config-version` to the jar's version and logs what was added.
  - A file with broken YAML fails with a `ConfigException` that names the file; on reload the old values stay.
- `Messages`: loads `messages.yml` and sends text with one `prefix`.
  - MiniMessage on every platform, plus `&` color codes and `&#rrggbb` hex colors.
  - Chat, action bar, title and broadcast; an empty message is not sent.
  - `Arg.of` placeholders escape player-supplied text so it cannot inject formatting.
- `Storage`: SQLite by default, or MySQL/MariaDB, picked by `storage.type` in the plugin's config.
  - Async `query` and `transaction` on a connection pool; the main thread never waits on the database.
  - Numbered migrations from `db/migration/<sqlite|mysql>/V1__init.sql` in the plugin jar, each in its own transaction, with `${prefix}` for table names. A database written by a newer plugin version is refused.
  - `migrate(from, to)` copies every table between SQLite and MySQL into an empty database and never deletes the source.
  - `warnIfSwitched` logs a warning when the database in use is empty but the other one still has data.
  - Uses the SQLite and MySQL drivers that ship with the server; only HikariCP is bundled.
- `Hooks`: loads soft-dependency hooks only when the other plugin is enabled, and turns the feature off with one warning if the hook fails.
- `Text`: MiniMessage parsing and legacy or plain output for item names and inventory titles.
- Menus: one `Menu` description, shown by `Menus` in the best style for each player.
  - Bedrock players get a Cumulus form through Floodgate, Java players on 1.21.6 or newer get a dialog, and everyone else gets a chest menu.
  - Button menus and yes/no confirm menus. Chest menus support fixed slots, chest-only buttons, pages and refreshing.
  - Floodgate is optional; without it Bedrock players see the Java menu.
- `NameResolver`: finds a player from a typed name, for example a vote.
  - Remembers every player who joins in a `known_players` table in the plugin's database.
  - On servers with an empty Floodgate `username-prefix`, also accepts the Bedrock gamertag, with or without spaces, and a loose match that counts only when one player fits.
  - With a prefix set, only the exact in-game name matches.
- `Text.bedrock` turns text into the section-sign format that Bedrock forms show.
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
