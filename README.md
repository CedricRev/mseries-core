# mseries-core

Shared library for MSeries plugins. Every MSeries plugin shades it in; it is never installed on a server by itself.

Version 1.0.0 covers:

- **Platform detection:** `Platform.current().core()` returns `SPIGOT`, `PAPER`, `PURPUR` or `FOLIA`, detected once at startup.
- **Commands:** `CommandService.create(this)` sets up Incendo Cloud v2 for the server. Paper, Purpur and Folia get native Brigadier. Spigot gets standard Bukkit registration.

Unreleased:

- **Config files:** `ConfigFile.load(this, "config.yml")` saves the default, adds new settings from the jar to the owner's file and bumps `config-version`.
- **Messages:** `Messages.load(this)` reads `messages.yml` and sends MiniMessage text with the plugin's `prefix` on Spigot, Paper and Folia.
- **Scheduler:** `Scheduler.create(this)` runs tasks on Folia's global, region, entity and async schedulers, and on the Bukkit scheduler everywhere else. Plugins never call `Bukkit.getScheduler()` themselves.

## How to use it

**1. Add it** in `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.3-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.3.10")
    implementation("com.github.CedricRev:mseries-core:v1.0.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.compilerArgs.add("-parameters")
}
```

**2. Shade and relocate** it with Shadow, replacing `<plugin>` with the plugin's package name:

```kotlin
tasks.shadowJar {
    val lib = "me.cedricrev.<plugin>.lib"
    relocate("me.cedricrev.mseries.core", "$lib.core")
    relocate("org.incendo.cloud", "$lib.cloud")
    relocate("io.leangen.geantyref", "$lib.geantyref")
    relocate("net.kyori", "$lib.kyori")
    exclude("META-INF/services/javax.annotation.processing.Processor")
}
```

**3. Register commands** in `onEnable`:

```java
public final class MExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CommandService commands = CommandService.create(this);
        commands.register(new ExampleCommands());
    }
}

public final class ExampleCommands {

    @Command("mexample|mex ping")
    @Permission("mexample.ping")
    public void ping(CommandActor actor) {
        actor.reply("pong");
    }
}
```

**4. Schedule tasks** through core instead of `Bukkit.getScheduler()`:

```java
Scheduler scheduler = Scheduler.create(this);

scheduler.runAt(block.getLocation(), () -> block.setType(Material.AIR));
scheduler.runFor(player, () -> player.sendMessage("Welcome back"));
scheduler.runAsync(() -> loadFromDatabase());
Task countdown = scheduler.runForTimer(player, this::tick, 20L, 20L);
```

| Method | Folia | Spigot and Paper |
| --- | --- | --- |
| `run`, `runLater`, `runTimer` | Global region | Main thread |
| `runAt...` (a location) | That location's region | Main thread |
| `runFor...` (an entity) | That entity's scheduler; skipped once it is removed | Main thread; skipped once it is removed or the player logs out |
| `runAsync...` | Async scheduler | Bukkit async |

Delays and periods are in ticks. Call `scheduler.cancelAll()` in `onDisable`.

Ship both `plugin.yml` and `paper-plugin.yml` with `folia-supported: true`, and don't list commands in either file.

**5. Load config and messages** in `onEnable`, and reload both from the `reload` command:

```java
ConfigFile config = ConfigFile.load(this, "config.yml");
Messages messages = Messages.load(this);

int goal = config.yaml().getInt("vote-party.goal");
messages.send(player, "vote.received", Arg.of("site", siteName));
```

`messages.yml` holds every player-facing line and one `prefix`:

```yaml
config-version: 1
prefix: "<gray>[<gold>MExample</gold>]</gray> "
vote:
  received: "<green>Thanks for voting on <site>!"
```

- `send` adds the prefix; `sendRaw`, `sendActionBar` and `sendTitle` don't.
- An empty message (`""`) is not sent, so owners can turn a line off.
- `Arg.of` escapes its value, so player names can't add colors or click events. Use `Arg.formatted` only for text the owner wrote.
- `legacy` and `plain` return strings for item names and inventory titles.
- Call `messages.close()` in `onDisable`.

## License

[MIT](LICENSE)
